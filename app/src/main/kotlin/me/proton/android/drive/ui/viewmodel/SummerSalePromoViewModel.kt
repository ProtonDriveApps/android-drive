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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.failureReason
import me.proton.android.drive.extension.log
import me.proton.android.drive.ui.effect.PaymentErrorEffect
import me.proton.android.drive.ui.viewevent.SummerSalePromoViewEvent
import me.proton.android.drive.ui.viewstate.ProtonPaymentButtonViewState
import me.proton.android.drive.ui.viewstate.SummerSalePromoViewState
import me.proton.android.drive.usecase.notification.MarkSummerSalePromoAsShown
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.payment.presentation.viewmodel.ProtonPaymentEvent
import me.proton.core.plan.domain.usecase.GetCurrentSubscription
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import me.proton.core.plan.domain.usecase.ObserveUserCurrency
import me.proton.core.plan.presentation.entity.PlanCycle
import me.proton.core.plan.presentation.usecase.ComposeAutoRenewText
import me.proton.core.presentation.utils.formatCentsPriceDefaultLocale
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.payment.R as CorePayment
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
class SummerSalePromoViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    observeUserCurrency: ObserveUserCurrency,
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices,
    private val composeAutoRenewText: ComposeAutoRenewText,
    private val markSummerSalePromoAsShown: MarkSummerSalePromoAsShown,
    private val broadcastMessages: BroadcastMessages,
    private val getCurrentSubscription: GetCurrentSubscription,
    private val asyncAnnounceEvent: AsyncAnnounceEvent,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val _paymentErrorEffect = MutableSharedFlow<PaymentErrorEffect>()
    private var viewEvent: SummerSalePromoViewEvent? = null
    private val plans = flowOf {
        coRunCatching { getDynamicPlansAdjustedPrices(userId).plans }
            .onFailure { error ->
                error.log(VIEW_MODEL, "Failed to get dynamic plans")
            }
            .getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val storageImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.img_storage_light_32,
        dark = BasePresentation.drawable.img_storage_dark_32,
        dayNight = BasePresentation.drawable.img_storage_daynight_32,
    )

    private val storageImageLandResId: Int get() = BasePresentation.drawable.img_storage_dark_32

    private val documentImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.img_document_light_32,
        dark = BasePresentation.drawable.img_document_dark_32,
        dayNight = BasePresentation.drawable.img_document_daynight_32,
    )

    private val documentImageLandResId: Int get() = BasePresentation.drawable.img_document_dark_32

    private val secureImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.img_secure_light_32,
        dark = BasePresentation.drawable.img_secure_dark_32,
        dayNight = BasePresentation.drawable.img_secure_daynight_32,
    )

    private val secureImageLandResId: Int get() = BasePresentation.drawable.img_secure_dark_32

    private val privateImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.img_private_light_32,
        dark = BasePresentation.drawable.img_private_dark_32,
        dayNight = BasePresentation.drawable.img_private_daynight_32,
    )

    private val privateImageLandResId: Int get() = BasePresentation.drawable.img_private_dark_32

    private val titleImageResId: Int get() = BasePresentation.drawable.img_summer_sale

    private val subtitleImageResId: Int get() = BasePresentation.drawable.img_summer_sale_discount

    private val backgroundResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.bg_summer_sale_promo_port_light,
        dark = BasePresentation.drawable.bg_summer_sale_promo_port_dark,
        dayNight = BasePresentation.drawable.bg_summer_sale_promo_port_daynight,
    )

    private val backgroundLandResId: Int get() = BasePresentation.drawable.bg_summer_sale_promo_land

    private val items = setOf(
        SummerSalePromoViewState.Item(
            imageResId = storageImageResId,
            imageLandResId = storageImageLandResId,
            title = appContext.getString(I18N.string.promo_storage_200_GB_title),
        ),
        SummerSalePromoViewState.Item(
            imageResId = documentImageResId,
            imageLandResId = documentImageLandResId,
            title = appContext.getString(I18N.string.promo_document_title),
        ),
        SummerSalePromoViewState.Item(
            imageResId = secureImageResId,
            imageLandResId = secureImageLandResId,
            title = appContext.getString(I18N.string.promo_secure_title),
        ),
        SummerSalePromoViewState.Item(
            imageResId = privateImageResId,
            imageLandResId = privateImageLandResId,
            title = appContext.getString(I18N.string.promo_private_title),
        ),
    )

    private val initialViewState = SummerSalePromoViewState(
        titleImageResId = titleImageResId,
        subtitleImageResId = subtitleImageResId,
        backgroundResId = backgroundResId,
        backgroundLandResId = backgroundLandResId,
        closeAction = Action.Icon(
            iconResId = CorePresentation.drawable.ic_proton_cross,
            contentDescriptionResId = I18N.string.common_close_action,
            onAction = { viewEvent?.onClose?.invoke() },
        ),
        items = items,
        getDealButtonResId = I18N.string.promo_claim_offer_button,
        monthlyPrice = "",
        period = appContext.quantityString(I18N.plurals.common_x_months, 12).lowercase(),
        monthlyPricePeriod = "/${appContext.getString(I18N.string.common_month).lowercase()}",
        autoRenewPrice = "",
    )
    val paymentErrorEffect: Flow<PaymentErrorEffect>
        get() = _paymentErrorEffect.asSharedFlow()
    val viewState: Flow<SummerSalePromoViewState> = combine(
        observeUserCurrency(userId),
        plans.filterNotNull(),
    ) { userCurrency, plans ->
        val cycle = PlanCycle.YEARLY.value
        plans.firstOrNull { plan -> plan.name == DRIVE_PLUS_200_GB }
            ?.let { plan ->
                val availableCurrencies = plan.instances[cycle]?.price?.keys?.map { it.uppercase() } ?: emptyList()
                val currency = if (availableCurrencies.contains(userCurrency.uppercase())) {
                    userCurrency
                } else {
                    availableCurrencies.firstOrNull() ?: ""
                }
                val monthlyPrice = takeIf { currency.isNotEmpty() }
                    ?.let {
                        plan.instances[cycle]?.price[currency]?.current?.toDouble()?.div(12)?.formatCentsPriceDefaultLocale(currency)
                    } ?: ""
                initialViewState.copy(
                    monthlyPrice = monthlyPrice,
                    autoRenewPrice = composeAutoRenewText(plan.instances[cycle]?.price[currency], cycle) ?: "",
                    paymentButtonViewState = ProtonPaymentButtonViewState(
                        userId = userId,
                        id = PAYMENT_BUTTON_ID,
                        plan = plan,
                        currency = currency,
                        cycle = cycle,
                    )
                )
            } ?: initialViewState
    }

    fun viewEvent(
        navigateBack: () -> Unit,
    ): SummerSalePromoViewEvent = object : SummerSalePromoViewEvent {
        override val onClose = navigateBack
        override val onPromoShown = { onShown() }
        override val onPaymentCallback = { event: ProtonPaymentEvent ->
            handleProtonPaymentEvent(event, navigateBack)
        }
        override val onRedeemedSuccessfully = { purchaseSuccess(navigateBack) }
    }.also { viewEvent ->
        this.viewEvent = viewEvent
    }

    private fun handleProtonPaymentEvent(
        event: ProtonPaymentEvent,
        navigateBack: () -> Unit,
    ) {
        when (event) {
            is ProtonPaymentEvent.Loading -> asyncAnnounceEvent(
                userId = userId,
                event = Event.Sentry.SummerSale2026(
                    action = Event.Sentry.Payments.Action.CLAIM_OFFER
                )
            )
            is ProtonPaymentEvent.GiapSuccess -> purchaseSuccess(navigateBack)
            is ProtonPaymentEvent.Error -> {
                asyncAnnounceEvent(
                    userId = userId,
                    event = Event.Sentry.SummerSale2026(
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
                event = Event.Sentry.SummerSale2026(
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
        viewModelScope.launch {
            asyncAnnounceEvent(
                userId = userId,
                event = Event.Sentry.SummerSale2026(
                    action = Event.Sentry.Payments.Action.SCREEN_SHOWN,
                )
            )
            markSummerSalePromoAsShown(userId)
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed to mark summer sale promo as shown")
                }
        }
    }

    companion object {
        private const val DRIVE_PLUS_200_GB = "drive2022"
        private const val PAYMENT_BUTTON_ID = 20260622
    }
}
