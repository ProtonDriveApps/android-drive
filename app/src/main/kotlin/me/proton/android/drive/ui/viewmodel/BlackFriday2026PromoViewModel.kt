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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.log
import me.proton.android.drive.ui.viewevent.BlackFriday2026PromoViewEvent
import me.proton.android.drive.ui.viewstate.BlackFriday2026PromoViewState
import me.proton.android.drive.ui.viewstate.BlackFriday2026PromoViewState.CellValue
import me.proton.android.drive.ui.viewstate.BlackFriday2026PromoViewState.ComparisonRow
import me.proton.android.drive.usecase.BlackFriday2026OfferCache
import me.proton.android.payment.common.exception.PaymentException
import me.proton.android.payment.purchase.extension.transacting
import me.proton.android.payment.purchase.model.PendingPurchase
import me.proton.android.payment.purchase.model.SessionState
import me.proton.android.payment.purchase.usecase.ObserveSessionState
import me.proton.android.payment.purchase.usecase.PurchaseProduct
import me.proton.android.payment.ui.common.extension.toDisplayMessageStringRes
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.promo.domain.entity.BlackFriday2026Offer
import me.proton.core.drive.promo.domain.entity.PromoStoreOffer
import me.proton.core.drive.promo.domain.usecase.MarkBlackFriday2026PromoAsShown
import me.proton.core.drive.promo.domain.usecase.OptOutOfBlackFriday2026Upgrade
import me.proton.core.presentation.utils.formatCentsPriceDefaultLocale
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.payment.R as CorePayment
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
class BlackFriday2026PromoViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val blackFriday2026OfferCache: BlackFriday2026OfferCache,
    private val markBlackFriday2026PromoAsShown: MarkBlackFriday2026PromoAsShown,
    private val optOutOfBlackFriday2026Upgrade: OptOutOfBlackFriday2026Upgrade,
    private val broadcastMessages: BroadcastMessages,
    private val asyncAnnounceEvent: AsyncAnnounceEvent,
    private val purchaseProduct: PurchaseProduct,
    private val observeSessionState: ObserveSessionState,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private var viewEvent: BlackFriday2026PromoViewEvent? = null
    private var sessionStateObservationJob: Job? = null
    private val isPaymentLoading = MutableStateFlow(false)
    private var isShownReported = false
    private var isOptingOut = false

    private val offer: StateFlow<BlackFriday2026Offer?> = blackFriday2026OfferCache(userId)
        .filterIsInstance<BlackFriday2026OfferCache.State.Loaded>()
        .map { state -> state.offer }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val freeColumnTitle = appContext.getString(I18N.string.promo_black_friday_2026_free_column_title)
    private val plusColumnTitle = appContext.getString(I18N.string.promo_black_friday_2026_plus_column_title)

    private val initialViewState = BlackFriday2026PromoViewState(
        headerResId = BasePresentation.drawable.bg_black_friday_2026_promo_port,
        headerDescriptionResId = I18N.string.promo_black_friday_2026_header_description,
        closeAction = Action.Icon(
            iconResId = CorePresentation.drawable.ic_proton_cross,
            contentDescriptionResId = I18N.string.common_close_action,
            onAction = { viewEvent?.onClose?.invoke() },
        ),
        currentPlanColumnTitle = freeColumnTitle,
        planColumnTitle = plusColumnTitle,
        comparisonRows = listOf(
            ComparisonRow(
                label = appContext.getString(I18N.string.promo_upsell_storage_label),
                currentPlan = CellValue.Value(appContext.getString(I18N.string.promo_black_friday_2026_storage_free)),
                plan = CellValue.Value(
                    text = appContext.getString(I18N.string.promo_black_friday_2026_storage_plus),
                    subtitle = appContext.getString(
                        I18N.string.promo_upsell_storage_plan_subtitle,
                        STORAGE_INCREASE_FACTOR,
                        freeColumnTitle,
                    ),
                ),
            ),
            ComparisonRow(
                label = appContext.getString(I18N.string.promo_upsell_version_history_label),
                currentPlan = CellValue.Value(
                    appContext.quantityString(I18N.plurals.common_x_days, FREE_HISTORY_DAYS)
                ),
                plan = CellValue.Value(appContext.quantityString(I18N.plurals.common_x_years, PLUS_HISTORY_YEARS)),
            ),
            ComparisonRow(
                label = appContext.getString(I18N.string.promo_upsell_share_with_edit_access_label),
                currentPlan = CellValue.Check(included = false),
                plan = CellValue.Check(included = true),
            ),
        ),
        period = appContext.quantityString(I18N.plurals.common_x_months, OFFER_MONTHS),
        discount = appContext.getString(I18N.string.promo_black_friday_2026_discount, DISCOUNT_PERCENTAGE),
        claimOfferButtonResId = I18N.string.promo_claim_offer_button,
    )

    private val upgradeViewState = initialViewState.copy(
        currentPlanColumnTitle = plusColumnTitle,
        planColumnTitle = appContext.getString(I18N.string.promo_black_friday_2026_unlimited_column_title),
        comparisonRows = listOf(
            ComparisonRow(
                label = appContext.getString(I18N.string.promo_upsell_storage_label),
                currentPlan = CellValue.Value(appContext.getString(I18N.string.promo_black_friday_2026_storage_plus)),
                plan = CellValue.Value(appContext.getString(I18N.string.promo_black_friday_2026_storage_unlimited)),
            ),
        ) + listOf(
            I18N.string.promo_black_friday_2026_premium_access_label,
            I18N.string.promo_black_friday_2026_aliases_label,
            I18N.string.promo_black_friday_2026_custom_email_domain_label,
            I18N.string.promo_black_friday_2026_priority_support_label,
        ).map { labelResId ->
            ComparisonRow(
                label = appContext.getString(labelResId),
                currentPlan = CellValue.Check(included = false),
                plan = CellValue.Check(included = true),
            )
        },
        stayOnPlanButtonResId = I18N.string.promo_black_friday_2026_stay_on_plan_button,
    )

    val viewState: Flow<BlackFriday2026PromoViewState> = combine(
        offer,
        isPaymentLoading,
    ) { offer, isPaymentLoading ->
        (offer?.toViewState() ?: initialViewState).copy(isPaymentLoading = isPaymentLoading)
    }

    fun viewEvent(
        navigateBack: () -> Unit,
    ): BlackFriday2026PromoViewEvent {
        ensureSessionStateObservation()
        return object : BlackFriday2026PromoViewEvent {
            override val onClose = navigateBack
            override val onPromoShown = { onShown() }
            override val onPurchaseClicked = { productId: String, offerToken: String ->
                onPurchaseClicked(productId, offerToken)
            }
            override val onStayOnPlan = { onStayOnPlan() }
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

    private fun onShown() {
        if (isShownReported) return
        isShownReported = true
        viewModelScope.launch {
            val offer = offer.filterNotNull().first()
            announce(offer.promoId, action = Event.Sentry.Payments.Action.SCREEN_SHOWN)
            markBlackFriday2026PromoAsShown(userId, offer)
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed to mark Black Friday 2026 promo as shown")
                }
        }
    }

    private fun onPurchaseClicked(productId: String, offerToken: String) {
        viewModelScope.launch {
            isPaymentLoading.value = true
            announce(promoId, action = Event.Sentry.Payments.Action.CLAIM_OFFER)
            purchaseProduct(PendingPurchase(productId, offerToken))
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed to initiate purchase")
                    isPaymentLoading.value = false
                }
        }
    }

    private fun onStayOnPlan() {
        if (isOptingOut) return
        isOptingOut = true
        viewModelScope.launch {
            announce(promoId, action = Event.Sentry.Payments.Action.STAY_ON_PLAN)
            optOutOfBlackFriday2026Upgrade(userId)
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed to opt out of Black Friday 2026 upgrade")
                }
            blackFriday2026OfferCache.refresh(userId)
            viewEvent?.onClose?.invoke()
        }
    }

    private fun onPurchaseSuccess() {
        announce(promoId, result = Event.Sentry.Payments.Result.SUCCESS)
        blackFriday2026OfferCache.refresh(userId)
        broadcastMessages(
            userId = userId,
            message = appContext.getString(CorePayment.string.payments_giap_redeem_success),
            type = BroadcastMessage.Type.SUCCESS,
        )
        isPaymentLoading.value = false
        viewEvent?.onClose?.invoke()
    }

    private fun onPurchaseFailure(exception: PaymentException) {
        exception.log(VIEW_MODEL, "Purchase failed")
        announce(promoId, result = Event.Sentry.Payments.Result.FAILURE)
        broadcastMessages(
            userId = userId,
            message = appContext.getString(exception.toDisplayMessageStringRes()),
            type = BroadcastMessage.Type.ERROR,
        )
        isPaymentLoading.value = false
    }

    private val promoId: String get() = offer.value?.promoId ?: BlackFriday2026Offer.FREE_PROMO_ID

    private fun announce(
        promoId: String,
        action: Event.Sentry.Payments.Action? = null,
        result: Event.Sentry.Payments.Result? = null,
    ) = asyncAnnounceEvent(
        userId = userId,
        event = Event.Sentry.BlackFriday2026(promoId = promoId, action = action, result = result),
    )

    private fun BlackFriday2026Offer.toViewState(): BlackFriday2026PromoViewState = when (this) {
        is BlackFriday2026Offer.Free -> storeOffer.toViewState(initialViewState)
        is BlackFriday2026Offer.Upgrade -> storeOffer.toViewState(upgradeViewState)
    }

    private fun PromoStoreOffer.toViewState(layout: BlackFriday2026PromoViewState): BlackFriday2026PromoViewState {
        val introductoryPriceCents = introductoryPriceMicros / MICROS_PER_CENT
        return layout.copy(
            price = introductoryPriceCents.formatCentsPriceDefaultLocale(currency),
            monthlyPrice = appContext.getString(
                I18N.string.promo_offer_per_month_details,
                (introductoryPriceCents / OFFER_MONTHS).formatCentsPriceDefaultLocale(currency),
            ),
            footer = appContext.getString(
                I18N.string.promo_black_friday_2026_footer,
                (renewalPriceMicros / MICROS_PER_CENT).formatCentsPriceDefaultLocale(currency),
            ),
            productId = productId,
            offerToken = offerToken,
        )
    }

    private companion object {
        const val MICROS_PER_CENT = 10_000.0
        const val OFFER_MONTHS = 12
        const val DISCOUNT_PERCENTAGE = 50
        const val STORAGE_INCREASE_FACTOR = 40
        const val FREE_HISTORY_DAYS = 7
        const val PLUS_HISTORY_YEARS = 10
    }
}
