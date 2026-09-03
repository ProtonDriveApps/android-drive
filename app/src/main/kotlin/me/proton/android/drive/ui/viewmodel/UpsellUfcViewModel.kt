/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.entity.UpsellPlanConfig
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.getUpsellPlanConfig
import me.proton.android.drive.extension.log
import me.proton.android.drive.ui.effect.UpsellUfcEffect
import me.proton.android.drive.ui.viewevent.UpsellUfcViewEvent
import me.proton.android.drive.ui.viewstate.UpsellUfcViewState
import me.proton.android.payment.common.exception.PaymentException
import me.proton.android.payment.product.model.Offer
import me.proton.android.payment.product.model.Product
import me.proton.android.payment.product.usecase.GetProducts
import me.proton.android.payment.purchase.extension.transacting
import me.proton.android.payment.purchase.model.PendingPurchase
import me.proton.android.payment.purchase.model.SessionState
import me.proton.android.payment.purchase.usecase.ObserveSessionState
import me.proton.android.payment.purchase.usecase.PurchaseProduct
import me.proton.android.payment.ui.common.extension.toDisplayMessageStringRes
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.formatDays
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.plan.domain.usecase.GetCurrentSubscription
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.payment.R as CorePayment
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class UpsellUfcViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    private val getComposedProducts: GetProducts,
    private val broadcastMessages: BroadcastMessages,
    private val getCurrentSubscription: GetCurrentSubscription,
    private val asyncAnnounceEvent: AsyncAnnounceEvent,
    private val configurationProvider: ConfigurationProvider,
    private val purchaseProduct: PurchaseProduct,
    private val observeSessionState: ObserveSessionState,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private var viewEvent: UpsellUfcViewEvent? = null
    private var sessionStateObservationJob: Job? = null
    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private val isPaymentLoading = MutableStateFlow(false)
    private val closeAction = Action.Icon(
        iconResId = CorePresentation.drawable.ic_proton_cross,
        contentDescriptionResId = I18N.string.common_close_action,
        onAction = { viewEvent?.onClose?.invoke() },
    )

    val initialViewState: UpsellUfcViewState = UpsellUfcViewState.Loading(closeAction)

    private val products: StateFlow<Result<List<Product>>?> = retryTrigger.transformLatest {
        emit(coRunCatching { getComposedProducts().getOrThrow() })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val payloadPlan: StateFlow<PlanConfigState> = getFeatureFlagFlow(
        featureFlagId = FeatureFlagId.driveMobileUpsellPlan(userId),
        emitNotFoundInitially = false,
    )
        .map { featureFlag -> PlanConfigState.Resolved(featureFlag.getUpsellPlanConfig()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlanConfigState.Loading)

    // Pure resolution of the async inputs. Side-effect free, so it can drive both the view state
    // and the one-shot redirect effect without navigating from a state mapper.
    private val resolution: Flow<UpsellResolution> = combine(
        products.filterNotNull(),
        payloadPlan,
    ) { productsResult, planConfigState ->
        resolve(productsResult, planConfigState)
    }

    val effect: Flow<UpsellUfcEffect> = resolution
        .filterIsInstance<UpsellResolution.Redirect>()
        .take(1)
        .map { UpsellUfcEffect.RedirectToSubscription }

    val viewState: StateFlow<UpsellUfcViewState> = combine(
        resolution,
        isPaymentLoading,
    ) { resolution, paymentLoading ->
        when (resolution) {
            UpsellResolution.Loading,
            UpsellResolution.Redirect -> UpsellUfcViewState.Loading(closeAction)
            is UpsellResolution.Error -> resolution.toErrorViewState()
            is UpsellResolution.Content -> resolution.content.copy(isPaymentLoading = paymentLoading)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialViewState)

    fun viewEvent(
        navigateBack: () -> Unit,
    ): UpsellUfcViewEvent {
        ensureSessionStateObservation()
        return object : UpsellUfcViewEvent {
            override val onClose = { navigateBack() }
            override val onShown = { onShown() }
            override val onPurchaseClicked = { productId: String, offerToken: String ->
                onPurchaseClicked(productId, offerToken)
            }
        }.also { viewEvent = it }
    }

    private fun ensureSessionStateObservation() {
        if (sessionStateObservationJob != null) return

        sessionStateObservationJob = viewModelScope.launch {
            observeSessionState().collect { state ->
                when (state) {
                    is SessionState.Reconciling.Terminal.Success -> onPurchaseSuccess()
                    is SessionState.Purchasing.Terminal.Failure -> onPurchaseFailure(state.exception)
                    is SessionState.Reconciling.Terminal.Failure -> onPurchaseFailure(state.exception)
                    else -> isPaymentLoading.value = state.transacting()
                }
            }
        }
    }

    private fun onPurchaseClicked(
        productId: String,
        offerToken: String,
    ) {
        viewModelScope.launch {
            isPaymentLoading.value = true
            asyncAnnounceEvent(
                userId = userId,
                event = Event.Sentry.Upsell(action = Event.Sentry.Payments.Action.CLAIM_OFFER),
            )
            purchaseProduct(PendingPurchase(productId, offerToken))
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed to initiate purchase")
                    isPaymentLoading.value = false
                }
        }
    }

    private fun onPurchaseSuccess() {
        viewModelScope.launch {
            asyncAnnounceEvent(
                userId = userId,
                event = Event.Sentry.Upsell(result = Event.Sentry.Payments.Result.SUCCESS),
            )
            coRunCatching { getCurrentSubscription(userId) }
                .onFailure { it.log(VIEW_MODEL, "Failed getting current subscription") }
            broadcastMessages(
                userId = userId,
                message = appContext.getString(CorePayment.string.payments_giap_redeem_success),
                type = BroadcastMessage.Type.SUCCESS,
            )
            isPaymentLoading.value = false
            viewEvent?.onClose?.invoke()
        }
    }

    private fun onPurchaseFailure(exception: PaymentException) {
        exception.log(VIEW_MODEL, "Purchase failed")
        asyncAnnounceEvent(
            userId = userId,
            event = Event.Sentry.Upsell(result = Event.Sentry.Payments.Result.FAILURE),
        )
        broadcastMessages(
            userId = userId,
            message = appContext.getString(exception.toDisplayMessageStringRes()),
            type = BroadcastMessage.Type.ERROR,
        )
        isPaymentLoading.value = false
    }

    private fun onShown() {
        asyncAnnounceEvent(
            userId = userId,
            event = Event.Sentry.Upsell(action = Event.Sentry.Payments.Action.SCREEN_SHOWN),
        )
    }

    private fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    private fun resolve(
        productsResult: Result<List<Product>>,
        planConfigState: PlanConfigState,
    ): UpsellResolution {
        val planConfig = when (planConfigState) {
            PlanConfigState.Loading -> return UpsellResolution.Loading
            is PlanConfigState.Resolved -> planConfigState.config ?: return UpsellResolution.Redirect
        }
        val products = productsResult.getOrElse { error ->
            error.log(VIEW_MODEL, "Failed to load products")
            return UpsellResolution.Error(error)
        }
        val period = planConfig.paidPlan.cycleMonths.toPeriod()
        val product = products.firstOrNull { product ->
            val matchesPlan = product.planId == planConfig.paidPlan.name ||
                product.id.contains(planConfig.paidPlan.name)
            val recurringPeriod = product.offers
                .filterIsInstance<Offer.NonDiscounted>()
                .firstOrNull()
                ?.pricingPhases
                ?.lastOrNull()
                ?.period
            matchesPlan && recurringPeriod == period
        }
        if (product == null) {
            CoreLogger.w(
                tag = VIEW_MODEL,
                message = buildString {
                    append("Product ${planConfig.paidPlan.name} ($period) not found. Products: ")
                    append(products.joinToString { it.id })
                },
            )
            return UpsellResolution.Redirect
        }
        val content = product.toUpsellContent(planConfig)
        if (content == null) {
            CoreLogger.w(VIEW_MODEL, "No usable offer found for plan ${planConfig.paidPlan.name}")
            return UpsellResolution.Redirect
        }
        return UpsellResolution.Content(content)
    }

    private fun UpsellResolution.Error.toErrorViewState(): UpsellUfcViewState.Error =
        UpsellUfcViewState.Error(
            closeAction = closeAction,
            message = error.getDefaultMessage(
                context = appContext,
                useExceptionMessage = configurationProvider.useExceptionMessage,
            ),
            actionResId = if (error.isRetryable) I18N.string.common_retry_action else null,
            onAction = ::retry,
        )

    private fun Product.toUpsellContent(
        planConfig: UpsellPlanConfig,
    ): UpsellUfcViewState.Content? {
        val cycle = planConfig.paidPlan.cycleMonths.toInt()
        val introOffer = offers.filterIsInstance<Offer.Discounted>().firstOrNull()
        val baseOffer = offers.filterIsInstance<Offer.NonDiscounted>().firstOrNull() ?: return null
        val phases = introOffer?.pricingPhases ?: baseOffer.pricingPhases
        val currentPhase = phases.firstOrNull() ?: return null
        val recurringPhase = phases.last()
        val hasOffer = introOffer != null
        val firstPeriodPrice = currentPhase.price.formattedAmount
        return UpsellUfcViewState.Content(
            closeAction = closeAction,
            title = appContext.getString(
                I18N.string.promo_upsell_title,
                planConfig.paidPlan.shortTitle,
                firstPeriodPrice,
            ),
            subtitle = appContext.getString(I18N.string.promo_upsell_subtitle),
            freePlanColumnTitle = planConfig.freePlan.shortTitle,
            planColumnTitle = planConfig.paidPlan.shortTitle,
            storageLabel = appContext.getString(I18N.string.promo_upsell_storage_label),
            versionHistoryLabel = appContext.getString(I18N.string.promo_upsell_version_history_label),
            shareWithEditAccessLabel = appContext.getString(I18N.string.promo_upsell_share_with_edit_access_label),
            storageFree = planConfig.freePlan.storage.asHumanReadableString(appContext),
            storagePlan = planConfig.paidPlan.storage.asHumanReadableString(appContext),
            storagePlanSubtitle = appContext.getString(
                I18N.string.promo_upsell_storage_plan_subtitle,
                planConfig.paidPlan.storageIncreaseFactor,
                planConfig.freePlan.shortTitle,
            ),
            versionHistoryFree = planConfig.freePlan.history.formatDays(appContext),
            versionHistoryPlan = planConfig.paidPlan.history.formatDays(appContext),
            shareWithEditAccessFree = planConfig.freePlan.shareWithEditAccess,
            shareWithEditAccessPlan = planConfig.paidPlan.shareWithEditAccess,
            buttonTitle = appContext.getString(
                I18N.string.promo_upsell_claim_offer_button,
                appContext.quantityString(I18N.plurals.common_x_months, cycle),
                firstPeriodPrice,
            ),
            footer = appContext.getString(
                when {
                    hasOffer && cycle == ANNUAL_CYCLE_MONTHS -> I18N.string.plan_welcome_offer_auto_renews_annual
                    hasOffer -> I18N.string.plan_welcome_offer_auto_renews_monthly
                    cycle == ANNUAL_CYCLE_MONTHS -> I18N.string.plan_subscription_auto_renews_annual
                    else -> I18N.string.plan_subscription_auto_renews_monthly
                },
                recurringPhase.price.formattedAmount,
            ),
            productId = id,
            offerToken = introOffer?.token ?: baseOffer.token,
        )
    }
}

private const val ANNUAL_CYCLE_MONTHS = 12
private const val STOP_TIMEOUT_MILLIS = 5_000L

private fun Long.toPeriod(): String = when (this) {
    1L -> "P1M"
    12L -> "P1Y"
    else -> ""
}

/** Tri-state so "flag still loading" is distinguishable from "resolved to no upsell config". */
private sealed interface PlanConfigState {
    data object Loading : PlanConfigState
    data class Resolved(val config: UpsellPlanConfig?) : PlanConfigState
}

/** Side-effect-free resolution of the async inputs into a single decision. */
private sealed interface UpsellResolution {
    data object Loading : UpsellResolution
    data object Redirect : UpsellResolution
    data class Error(val error: Throwable) : UpsellResolution
    data class Content(val content: UpsellUfcViewState.Content) : UpsellResolution
}
