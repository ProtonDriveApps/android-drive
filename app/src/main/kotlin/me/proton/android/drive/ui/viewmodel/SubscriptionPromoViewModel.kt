/*
 * Copyright (c) 2023-2024 Proton AG.
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.drive.R
import me.proton.android.drive.log.DriveLogTag.DEFAULT
import me.proton.android.drive.ui.viewevent.DriveLitePopupViewEvent
import me.proton.android.drive.ui.viewstate.SubscriptionPromoViewState
import me.proton.android.drive.usecase.MarkSubscriptionPromoAsShown
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import me.proton.core.plan.domain.usecase.ObserveUserCurrency
import me.proton.core.plan.presentation.entity.PlanCycle
import me.proton.core.presentation.utils.formatCentsPriceDefaultLocale
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class SubscriptionPromoViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val markSubscriptionPromoAsShown: MarkSubscriptionPromoAsShown,
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices,
    observeUserCurrency: ObserveUserCurrency,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val key: String = savedStateHandle.require(PROMO_KEY)

    val viewState = observeUserCurrency(userId).map { userCurrency ->
        val plans = getDynamicPlansAdjustedPrices(userId)
        val plan = plans.plans.firstOrNull { plan -> plan.name == key }
        if (plan != null) {
            val currency = plan.getCurrency(userCurrency)
            when (key) {
                PLAN_NAME_PLUS -> drivePlusViewState(plan, currency, PlanCycle.MONTHLY)
                PLAN_NAME_LITE -> driveLiteViewState(plan, currency, PlanCycle.MONTHLY)
                else -> null
            }
        } else {
            null
        }
    }

    private fun DynamicPlan.getCurrency(
        userCurrency: String
    ): String {
        val instanceCurrencies = instances.flatMap { it.value.price.keys }.toSet().toList()
        val currencies = when {
            instanceCurrencies.contains(userCurrency) -> listOf(userCurrency) + (instanceCurrencies - userCurrency)
            else -> instanceCurrencies
        }
        val currency = currencies.firstOrNull { it == userCurrency }
            ?: currencies.firstOrNull() ?: userCurrency
        return currency
    }

    private fun drivePlusViewState(
        plan: DynamicPlan,
        currency: String,
        planCycle: PlanCycle,
    ): SubscriptionPromoViewState {
        val price = plan.instances[planCycle.value]?.price?.get(currency)
        val firstMonthPriceString =
            (price?.current)?.toDouble()?.formatCentsPriceDefaultLocale(currency).orEmpty()
        val priceString =
            (price?.default)?.toDouble()?.formatCentsPriceDefaultLocale(currency).orEmpty()
        val photoCountString = NumberFormat.getNumberInstance(Locale.getDefault()).format(50_000)
        val storageString = 200.GiB.asHumanReadableString(appContext, numberOfDecimals = 0)
        return SubscriptionPromoViewState(
            image = R.drawable.img_drive_plus,
            title = appContext.getString(I18N.string.drive_plus_promo_title)
                .format(storageString, firstMonthPriceString),
            description = appContext.getString(I18N.string.drive_plus_promo_description)
                .format(
                    photoCountString,
                    firstMonthPriceString,
                    priceString
                ),
            actionText = appContext.getString(I18N.string.drive_plus_promo_action)
                .format(firstMonthPriceString)
        )
    }

    private fun driveLiteViewState(
        plan: DynamicPlan,
        currency: String,
        planCycle: PlanCycle,
    ): SubscriptionPromoViewState {
        val price = plan.instances[planCycle.value]?.price?.get(currency)
        val priceString =
            (price?.default)?.toDouble()?.formatCentsPriceDefaultLocale(currency).orEmpty()
        val name = plan.title
        val storageString = 20.GiB.asHumanReadableString(appContext, numberOfDecimals = 0)
        return SubscriptionPromoViewState(
            image = R.drawable.img_drive_lite,
            title = appContext.getString(I18N.string.drive_lite_promo_title)
                .format(priceString),
            description = appContext.getString(I18N.string.drive_lite_promo_description)
                .format(name, storageString, priceString),
            actionText = appContext.getString(I18N.string.drive_lite_promo_action)
                .format(name)
        )
    }

    fun viewEvent(
        runAction: RunAction,
        navigateToSubscription: () -> Unit,
    ): DriveLitePopupViewEvent = object : DriveLitePopupViewEvent {
        override val onGetSubscription = {
            runAction {
                navigateToSubscription()
            }
        }
        override val onCancel = {
            runAction {

            }
        }
        override val onDismiss = {
            viewModelScope.launch {
                markSubscriptionPromoAsShown(userId, key).onFailure { error ->
                    error.log(DEFAULT, "Cannot mark subscription promo as shown")
                }
            }
            Unit
        }
    }

    companion object {
        const val PROMO_KEY = "key"
        private const val PLAN_NAME_LITE = "drivelite2024"
        private const val PLAN_NAME_PLUS = "drive2022"
    }
}
