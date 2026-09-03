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

package me.proton.android.drive.usecase

import kotlinx.coroutines.flow.first
import me.proton.android.drive.extension.log
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.feature.flag.domain.usecase.IsQ3CampaignPromoEnabled
import me.proton.core.payment.domain.entity.Currency
import me.proton.core.payment.domain.entity.SubscriptionCycle
import me.proton.core.plan.domain.usecase.ObserveUserCurrency
import me.proton.core.plan.domain.usecase.ValidateSubscriptionPlan
import javax.inject.Inject

/**
 * A user is eligible for the Q3 campaign promo when the feature flag/subscription rules from
 * [IsQ3CampaignPromoEnabled] are met, AND the campaign coupon has not already been redeemed and
 * refunded by this user. The latter is detected by calling subscriptions/check with the coupon:
 * a [me.proton.core.payment.domain.entity.SubscriptionStatus.couponDiscount] of 0 means the
 * backend will no longer apply the coupon for this user.
 */
class IsQ3CampaignPromoEligible @Inject constructor(
    private val isQ3CampaignPromoEnabled: IsQ3CampaignPromoEnabled,
    @Suppress("DEPRECATION") private val validateSubscriptionPlan: ValidateSubscriptionPlan,
    private val observeUserCurrency: ObserveUserCurrency,
) {
    suspend operator fun invoke(userId: UserId): Boolean {
        return isQ3CampaignPromoEnabled(userId) && hasUnusedCoupon(userId)
    }

    private suspend fun hasUnusedCoupon(userId: UserId): Boolean = runCatching {
        val currencyCode = observeUserCurrency(userId).first()
        val currency = Currency.entries.firstOrNull { it.name.equals(currencyCode, ignoreCase = true) } ?: Currency.USD
        @Suppress("DEPRECATION")
        val status = validateSubscriptionPlan(
            userId = userId,
            codes = listOf(Q3_CAMPAIGN_COUPON_CODE),
            plans = listOf(Q3_CAMPAIGN_PLAN_NAME),
            currency = currency,
            cycle = SubscriptionCycle.YEARLY,
        )
        status.couponDiscount != 0L
    }.getOrElse { error ->
        error.log(LogTag.DEFAULT, "Failed to validate Q3 campaign coupon, failing open")
        // Fail-open: a network/API error should not block a legitimate user from the promo.
        true
    }

    companion object {
        private const val Q3_CAMPAIGN_COUPON_CODE = "SEP26BUNDLESALE"
        private const val Q3_CAMPAIGN_PLAN_NAME = "bundle2022"
    }
}

