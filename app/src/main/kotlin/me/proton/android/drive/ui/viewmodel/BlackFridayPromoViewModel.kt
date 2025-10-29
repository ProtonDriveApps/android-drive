/*
 * Copyright (c) 2025 Proton AG.
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.android.drive.ui.viewevent.BlackFridayPromoViewEvent
import me.proton.android.drive.ui.viewstate.BlackFridayPromoViewState
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import me.proton.core.plan.domain.usecase.ObserveUserCurrency
import me.proton.core.plan.presentation.usecase.ComposeAutoRenewText
import me.proton.core.presentation.utils.formatCentsPriceDefaultLocale
import java.util.Locale
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
class BlackFridayPromoViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    savedStateHandle: SavedStateHandle,
    observeUserCurrency: ObserveUserCurrency,
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices,
    private val composeAutoRenewText: ComposeAutoRenewText,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private var viewEvent: BlackFridayPromoViewEvent? = null
    private val items = setOf(
        BlackFridayPromoViewState.Item(
            imageResId = BasePresentation.drawable.img_storage_32,
            title = appContext.getString(I18N.string.bf_promo_storage_title),
        ),
        BlackFridayPromoViewState.Item(
            imageResId = BasePresentation.drawable.img_document_32,
            title = appContext.getString(I18N.string.bf_promo_document_title),
        ),
        BlackFridayPromoViewState.Item(
            imageResId = BasePresentation.drawable.img_secure_32,
            title = appContext.getString(I18N.string.bf_promo_secure_title),
        ),
        BlackFridayPromoViewState.Item(
            imageResId = BasePresentation.drawable.img_private_32,
            title = appContext.getString(I18N.string.bf_promo_private_title),
        ),
    )

    private val initialViewState = BlackFridayPromoViewState(
        titleImageResId = BasePresentation.drawable.img_bf_promo_any,
        backgroundResId = BasePresentation.drawable.bg_black_friday_promo_full,
        closeAction = Action.Icon(
            iconResId = CorePresentation.drawable.ic_proton_cross,
            contentDescriptionResId = I18N.string.common_close_action,
            onAction = { viewEvent?.onClose?.invoke() },
        ),
        items = items,
        getDealButtonResId = I18N.string.bf_promo_get_deal_button,
        firstMonthPrice = "",
        period = "1 ${appContext.getString(I18N.string.common_month).lowercase()}",
        pricePeriod = "/${appContext.getString(I18N.string.common_month).lowercase()}",
        autoRenewPrice = "",
    )
    val viewState: Flow<BlackFridayPromoViewState> = observeUserCurrency(userId).map { userCurrency ->
        val cycle = 1
        val plans = getDynamicPlansAdjustedPrices(userId).plans
        plans.firstOrNull { plan -> plan.name == DRIVE_PLUS_1_TB }
            ?.let { plan ->
                val availableCurrencies = plan.instances[cycle]?.price?.keys?.map { it.uppercase() } ?: emptyList()
                val currency = if (availableCurrencies.contains(userCurrency.uppercase())) {
                    userCurrency
                } else {
                    availableCurrencies.firstOrNull() ?: ""
                }
                val firstMonthPrice = takeIf { currency.isNotEmpty() }
                    ?.let {
                        plan.instances[cycle]?.price[currency]?.current?.toDouble()?.formatCentsPriceDefaultLocale(currency)
                    } ?: ""
                initialViewState.copy(
                    titleImageResId = currency.toTitleImageRes,
                    firstMonthPrice = firstMonthPrice,
                    autoRenewPrice = composeAutoRenewText(plan.instances[cycle]?.price[currency], cycle) ?: ""
                )
            } ?: initialViewState
    }

    fun viewEvent(
        navigateToSubscription: () -> Unit,
        navigateBack: () -> Unit,
    ): BlackFridayPromoViewEvent = object : BlackFridayPromoViewEvent {
        override val onClose = navigateBack
        override val onGetDeal: () -> Unit = {
            navigateToSubscription.invoke().also { navigateBack.invoke() }
        }
    }.also { viewEvent ->
        this.viewEvent = viewEvent
    }

    private val String.toTitleImageRes: Int get() = when (uppercase(Locale.getDefault())) {
        AUD -> BasePresentation.drawable.img_bf_promo_aud
        BRL -> BasePresentation.drawable.img_bf_promo_brl
        CAD -> BasePresentation.drawable.img_bf_promo_cad
        EUR -> BasePresentation.drawable.img_bf_promo_eur
        GBP -> BasePresentation.drawable.img_bf_promo_gbp
        USD -> BasePresentation.drawable.img_bf_promo_usd
        else -> BasePresentation.drawable.img_bf_promo_any
    }

    companion object {
        private const val AUD = "AUD"
        private const val BRL = "BRL"
        private const val CAD = "CAD"
        private const val EUR = "EUR"
        private const val GBP = "GBP"
        private const val USD = "USD"
        private const val DRIVE_PLUS_1_TB = "drive1tb2025"
    }
}
