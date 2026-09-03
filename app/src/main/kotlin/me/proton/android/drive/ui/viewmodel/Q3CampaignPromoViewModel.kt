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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.log
import me.proton.android.drive.ui.viewevent.Q3CampaignPromoViewEvent
import me.proton.android.drive.ui.viewstate.Q3CampaignPromoViewState
import me.proton.android.drive.usecase.notification.MarkQ3CampaignPromoAsShown
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
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.plan.domain.usecase.GetCurrentSubscription
import me.proton.core.presentation.utils.formatCentsPriceDefaultLocale
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.payment.R as CorePayment

@HiltViewModel
class Q3CampaignPromoViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val getComposedProducts: GetProducts,
    private val markQ3CampaignPromoAsShown: MarkQ3CampaignPromoAsShown,
    private val broadcastMessages: BroadcastMessages,
    private val getCurrentSubscription: GetCurrentSubscription,
    private val asyncAnnounceEvent: AsyncAnnounceEvent,
    private val purchaseProduct: PurchaseProduct,
    private val observeSessionState: ObserveSessionState,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private var viewEvent: Q3CampaignPromoViewEvent? = null
    private var sessionStateObservationJob: Job? = null
    private val isPaymentLoading = MutableStateFlow(false)

    private val products = flowOf {
        coRunCatching { getComposedProducts().getOrThrow() }
            .onFailure { error ->
                error.log(VIEW_MODEL, "Failed to get products")
            }
            .getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val backgroundResId: Int get() = BasePresentation.drawable.bg_q3_campaign_promo_port_dark

    private val comparisonRows = listOf(
        Q3CampaignPromoViewState.ComparisonRow(
            label = appContext.getString(I18N.string.promo_q3_campaign_comparison_storage_label),
            free = Q3CampaignPromoViewState.CellValue.Value(
                text = appContext.getString(I18N.string.promo_q3_campaign_comparison_storage_free),
            ),
            unlimited = Q3CampaignPromoViewState.CellValue.Value(
                text = appContext.getString(I18N.string.promo_q3_campaign_comparison_storage_unlimited),
                subtitle = appContext.getString(I18N.string.promo_q3_campaign_comparison_storage_unlimited_subtitle),
            ),
        ),
        Q3CampaignPromoViewState.ComparisonRow(
            label = appContext.getString(I18N.string.promo_q3_campaign_comparison_share_edit_label),
            free = Q3CampaignPromoViewState.CellValue.Check(included = false),
            unlimited = Q3CampaignPromoViewState.CellValue.Check(included = true),
        ),
        Q3CampaignPromoViewState.ComparisonRow(
            label = appContext.getString(I18N.string.promo_q3_campaign_comparison_mail_label),
            free = Q3CampaignPromoViewState.CellValue.Check(included = false),
            unlimited = Q3CampaignPromoViewState.CellValue.Check(included = true),
        ),
        Q3CampaignPromoViewState.ComparisonRow(
            label = appContext.getString(I18N.string.promo_q3_campaign_comparison_vpn_label),
            free = Q3CampaignPromoViewState.CellValue.Check(included = false),
            unlimited = Q3CampaignPromoViewState.CellValue.Check(included = true),
        ),
        Q3CampaignPromoViewState.ComparisonRow(
            label = appContext.getString(I18N.string.promo_q3_campaign_comparison_pass_label),
            free = Q3CampaignPromoViewState.CellValue.Check(included = false),
            unlimited = Q3CampaignPromoViewState.CellValue.Check(included = true),
        ),
    )

    private val initialViewState = Q3CampaignPromoViewState(
        backgroundResId = backgroundResId,
        // Action.Image (not Action.Icon): the drawable bakes in its own two colors (grey circle +
        // dark X), which Action.Icon would flatten into a single tint color when rendered.
        closeAction = Action.Image(
            imageResId = BasePresentation.drawable.drive_q3_campaign_close_button,
            contentDescriptionResId = I18N.string.common_close_action,
            onAction = { viewEvent?.onClose?.invoke() },
        ),
        comparisonRows = comparisonRows,
        getDealButtonResId = I18N.string.promo_claim_offer_button,
        totalPrice = "",
        monthlyPrice = "",
        period = appContext.quantityString(I18N.plurals.common_x_months, 12).lowercase(),
        monthlyPricePeriod = "/${appContext.getString(I18N.string.common_month).lowercase()}",
        autoRenewPrice = "",
    )

    val viewState: Flow<Q3CampaignPromoViewState> = combine(
        products.filterNotNull(),
        isPaymentLoading,
    ) { products, paymentLoading ->
        val product = products.firstOrNull { product ->
            val matchesPlan = product.planId == Q3_CAMPAIGN_PLAN_NAME || product.id.contains(Q3_CAMPAIGN_PLAN_NAME)
            val recurringPeriod = product.offers
                .filterIsInstance<Offer.NonDiscounted>()
                .firstOrNull()
                ?.pricingPhases
                ?.lastOrNull()
                ?.period
            matchesPlan && recurringPeriod == ANNUAL_PERIOD
        }
        (product?.toQ3CampaignContent() ?: initialViewState).copy(isPaymentLoading = paymentLoading)
    }.onStart {
        // Show the base promo layout immediately instead of a blank screen while products are
        // loading, or forever if the products fetch fails.
        emit(initialViewState)
    }

    fun viewEvent(
        navigateBack: () -> Unit,
    ): Q3CampaignPromoViewEvent {
        ensureSessionStateObservation()
        return object : Q3CampaignPromoViewEvent {
            override val onClose = navigateBack
            override val onPromoShown = { onShown() }
            override val onPurchaseClicked = { productId: String, offerToken: String ->
                onPurchaseClicked(productId, offerToken)
            }
        }.also { viewEvent -> this.viewEvent = viewEvent }
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

    private fun onPurchaseClicked(productId: String, offerToken: String) {
        viewModelScope.launch {
            isPaymentLoading.value = true
            asyncAnnounceEvent(
                userId = userId,
                event = Event.Sentry.Q3Campaign2026(action = Event.Sentry.Payments.Action.CLAIM_OFFER),
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
                event = Event.Sentry.Q3Campaign2026(result = Event.Sentry.Payments.Result.SUCCESS),
            )
            coRunCatching { getCurrentSubscription(userId) }
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed getting current subscription")
                }
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
            event = Event.Sentry.Q3Campaign2026(result = Event.Sentry.Payments.Result.FAILURE),
        )
        broadcastMessages(
            userId = userId,
            message = appContext.getString(exception.toDisplayMessageStringRes()),
            type = BroadcastMessage.Type.ERROR,
        )
        isPaymentLoading.value = false
    }

    private fun onShown() {
        viewModelScope.launch {
            asyncAnnounceEvent(
                userId = userId,
                event = Event.Sentry.Q3Campaign2026(action = Event.Sentry.Payments.Action.SCREEN_SHOWN),
            )
            markQ3CampaignPromoAsShown(userId)
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed to mark Q3 campaign promo as shown")
                }
        }
    }

    private fun Product.toQ3CampaignContent(): Q3CampaignPromoViewState? {
        val introOffer = offers.filterIsInstance<Offer.Discounted>().firstOrNull()
        val baseOffer = offers.filterIsInstance<Offer.NonDiscounted>().firstOrNull() ?: return null
        val phases = introOffer?.pricingPhases ?: baseOffer.pricingPhases
        val currentPhase = phases.firstOrNull() ?: return null
        val recurringPhase = phases.last()
        // Money.amount is in micros (1 unit = 1_000_000 micros = 100 cents).
        val monthlyPriceCents = currentPhase.price.amount / 10_000.0 / 12
        return initialViewState.copy(
            totalPrice = currentPhase.price.formattedAmount,
            monthlyPrice = monthlyPriceCents.formatCentsPriceDefaultLocale(currentPhase.price.currency),
            autoRenewPrice = appContext.getString(
                I18N.string.plan_welcome_offer_auto_renews_annual,
                recurringPhase.price.formattedAmount,
            ),
            productId = id,
            offerToken = (introOffer ?: baseOffer).token,
        )
    }

    companion object {
        private const val Q3_CAMPAIGN_PLAN_NAME = "bundle2022"
        private const val ANNUAL_PERIOD = "P1Y"
    }
}
