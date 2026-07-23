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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.entity.UpsellPlanConfig
import me.proton.android.drive.extension.failureReason
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.getUpsellPlanConfig
import me.proton.android.drive.extension.log
import me.proton.android.drive.ui.effect.PaymentErrorEffect
import me.proton.android.drive.ui.viewevent.UpsellViewEvent
import me.proton.android.drive.ui.viewstate.ProtonPaymentButtonViewState
import me.proton.android.drive.ui.viewstate.UpsellViewState
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
import me.proton.core.payment.presentation.viewmodel.ProtonPaymentEvent
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.usecase.GetCurrentSubscription
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import me.proton.core.plan.domain.usecase.ObserveUserCurrency
import me.proton.core.plan.presentation.usecase.ComposeAutoRenewText
import me.proton.core.presentation.utils.formatCentsPriceDefaultLocale
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.payment.R as CorePayment
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class UpsellViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    observeUserCurrency: ObserveUserCurrency,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices,
    private val composeAutoRenewText: ComposeAutoRenewText,
    private val broadcastMessages: BroadcastMessages,
    private val getCurrentSubscription: GetCurrentSubscription,
    private val asyncAnnounceEvent: AsyncAnnounceEvent,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private var viewEvent: UpsellViewEvent? = null
    private val _paymentErrorEffect = MutableSharedFlow<PaymentErrorEffect>()
    val paymentErrorEffect: Flow<PaymentErrorEffect>
        get() = _paymentErrorEffect.asSharedFlow()
    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private val plans: StateFlow<Result<List<DynamicPlan>>?> = retryTrigger.transformLatest {
        emit(coRunCatching { getDynamicPlansAdjustedPrices(userId).plans })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val payloadPlan = getFeatureFlagFlow(
        featureFlagId = FeatureFlagId.driveMobileUpsellPlan(userId),
        emitNotFoundInitially = false,
    )
        .map { featureFlag ->
            featureFlag.getUpsellPlanConfig()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val closeAction = Action.Icon(
        iconResId = CorePresentation.drawable.ic_proton_cross,
        contentDescriptionResId = I18N.string.common_close_action,
        onAction = { viewEvent?.onClose?.invoke() },
    )

    val initialViewState: UpsellViewState = UpsellViewState.Loading(closeAction)

    val viewState: Flow<UpsellViewState> = combine(
        observeUserCurrency(userId),
        plans.filterNotNull(),
        payloadPlan,
    ) { userCurrency, plans, payloadPlan ->
        if (payloadPlan == null) {
            viewEvent?.onRedirectToSubscription?.invoke()
            return@combine UpsellViewState.Loading(closeAction)
        }
        plans.fold(
            onFailure = { error ->
                error.log(VIEW_MODEL, "Failed to load dynamic plans")
                UpsellViewState.Error(
                    closeAction = closeAction,
                    message = error.getDefaultMessage(
                        context = appContext,
                        useExceptionMessage = configurationProvider.useExceptionMessage,
                    ),
                    actionResId = if (error.isRetryable) I18N.string.common_retry_action else null,
                    onAction = ::retry,
                )
            },
            onSuccess = { dynamicPlans ->
                dynamicPlans
                    .firstOrNull { plan -> plan.name == payloadPlan.paidPlan.name }
                    ?.getUpsellViewStateContent(payloadPlan, userCurrency)
                    ?: UpsellViewState.Loading(closeAction).also {
                        CoreLogger.w(
                            tag = VIEW_MODEL,
                            message = buildString {
                                append("Plan ${payloadPlan.paidPlan.name} not found. Dynamic plans: ")
                                append(
                                    dynamicPlans.joinToString { dynamicPlan ->
                                        dynamicPlan.name.orEmpty()
                                    }
                                )
                            },
                        )
                        viewEvent?.onRedirectToSubscription?.invoke()
                    }
            },
        )
    }

    fun viewEvent(
        navigateToSubscription: () -> Unit,
        navigateBack: () -> Unit,
    ): UpsellViewEvent = object : UpsellViewEvent {
        override val onClose = { navigateBack() }
        override val onShown = { onShown() }
        override val onRedirectToSubscription = { navigateToSubscription() }
        override val onPaymentCallback = { event: ProtonPaymentEvent ->
            handleProtonPaymentEvent(event, navigateBack)
        }
        override val onRedeemedSuccessfully = { purchaseSuccess(navigateBack) }
    }.also { viewEvent ->
        this.viewEvent = viewEvent
    }

    private fun DynamicPlan.getUpsellViewStateContent(
        upsellPlanConfig: UpsellPlanConfig,
        userCurrency: String,
    ): UpsellViewState.Content {
        val cycle = upsellPlanConfig.paidPlan.cycleMonths.toInt()
        val availableCurrencies = instances[cycle]?.price?.keys?.map { it.uppercase() } ?: emptyList()
        val currency = if (availableCurrencies.contains(userCurrency.uppercase())) {
            userCurrency
        } else {
            availableCurrencies.firstOrNull() ?: ""
        }
        val firstPeriodPrice = takeIf { currency.isNotEmpty() }
            ?.let {
                instances[cycle]?.price[currency]?.current?.toDouble()
                    ?.formatCentsPriceDefaultLocale(currency)
            } ?: ""
        return UpsellViewState.Content(
            closeAction = closeAction,
            title = appContext.getString(I18N.string.promo_upsell_title, title, firstPeriodPrice),
            subtitle = appContext.getString(I18N.string.promo_upsell_subtitle),
            freePlanColumnTitle = upsellPlanConfig.freePlan.shortTitle,
            planColumnTitle = upsellPlanConfig.paidPlan.shortTitle,
            storageLabel = appContext.getString(I18N.string.promo_upsell_storage_label),
            versionHistoryLabel = appContext.getString(I18N.string.promo_upsell_version_history_label),
            shareWithEditAccessLabel = appContext.getString(
                I18N.string.promo_upsell_share_with_edit_access_label
            ),
            storageFree = upsellPlanConfig.freePlan.storage.asHumanReadableString(appContext),
            storagePlan = upsellPlanConfig.paidPlan.storage.asHumanReadableString(appContext),
            storagePlanSubtitle = appContext.getString(I18N.string.promo_upsell_storage_plan_subtitle, upsellPlanConfig.paidPlan.storageIncreaseFactor, upsellPlanConfig.freePlan.shortTitle),
            versionHistoryFree = upsellPlanConfig.freePlan.history.formatDays(appContext),
            versionHistoryPlan = upsellPlanConfig.paidPlan.history.formatDays(appContext),
            shareWithEditAccessFree = upsellPlanConfig.freePlan.shareWithEditAccess,
            shareWithEditAccessPlan = upsellPlanConfig.paidPlan.shareWithEditAccess,
            buttonTitle = appContext.getString(I18N.string.promo_upsell_claim_offer_button, appContext.quantityString(I18N.plurals.common_x_months, cycle), firstPeriodPrice),
            footer = composeAutoRenewText(instances[cycle]?.price[currency], cycle) ?: "",
            protonPaymentButtonViewState = ProtonPaymentButtonViewState(
                userId = userId,
                id = PAYMENT_BUTTON_ID,
                plan = this,
                currency = currency,
                cycle = cycle,
            )
        )
    }

    private fun handleProtonPaymentEvent(
        event: ProtonPaymentEvent,
        navigateBack: () -> Unit,
    ) {
        when (event) {
            is ProtonPaymentEvent.Loading -> asyncAnnounceEvent(
                userId = userId,
                event = Event.Sentry.Upsell(
                    action = Event.Sentry.Payments.Action.CLAIM_OFFER
                )
            )
            is ProtonPaymentEvent.GiapSuccess -> purchaseSuccess(navigateBack)
            is ProtonPaymentEvent.Error -> {
                asyncAnnounceEvent(
                    userId = userId,
                    event = Event.Sentry.Upsell(
                        result = Event.Sentry.Payments.Result.FAILURE,
                        failureReason = event.failureReason,
                    )
                )
                when (event) {
                    is ProtonPaymentEvent.Error.GiapUnredeemed -> giapUnredeemed()
                    is ProtonPaymentEvent.Error.UserCancelled -> Unit
                    else -> broadcastMessages(
                        userId = userId,
                        message = appContext.getString(CorePayment.string.payments_general_error),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
            }
            else -> Unit
        }
    }

    private fun purchaseSuccess(navigateBack: () -> Unit) {
        viewModelScope.launch {
            asyncAnnounceEvent(
                userId = userId,
                event = Event.Sentry.Upsell(
                    result = Event.Sentry.Payments.Result.SUCCESS
                )
            )
            coRunCatching {
                getCurrentSubscription(userId)
            }
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed getting current subscription")
                }
            broadcastMessages(
                userId = userId,
                message = appContext.getString(CorePayment.string.payments_giap_redeem_success),
                type = BroadcastMessage.Type.SUCCESS,
            )
            navigateBack()
        }
    }

    private fun giapUnredeemed() = viewModelScope.launch {
        _paymentErrorEffect.emit(PaymentErrorEffect.GiapUnredeemed)
    }

    private fun onShown() {
        asyncAnnounceEvent(
            userId = userId,
            event = Event.Sentry.Upsell(
                action = Event.Sentry.Payments.Action.SCREEN_SHOWN,
            )
        )
    }

    private fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    companion object {
        private const val PAYMENT_BUTTON_ID = 20260633
    }
}
