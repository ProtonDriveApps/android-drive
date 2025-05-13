/*
 * Copyright (c) 2024 Proton AG.
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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.datastore.GetUserDataStore.Keys.subscriptionLastUpdate
import me.proton.core.drive.base.data.extension.get
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.extension.off
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.payment.domain.PaymentManager
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import me.proton.core.plan.presentation.entity.PlanCycle
import me.proton.core.user.domain.extension.hasSubscription
import me.proton.core.user.domain.usecase.GetUser
import me.proton.drive.android.settings.domain.entity.UserOverlay
import javax.inject.Inject

class ShouldShowSubscriptionPromo @Inject constructor(
    private val getFeatureFlag: GetFeatureFlag,
    private val getUser: GetUser,
    private val paymentManager: PaymentManager,
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices,
    private val getUserDataStore: GetUserDataStore,
) {
    suspend operator fun invoke(userId: UserId): Result<UserOverlay.Subcription?> = coRunCatching {
        if (!paymentManager.isSubscriptionAvailable(userId)) {
            return@coRunCatching null
        }

        if (getFeatureFlag(FeatureFlagId.drivePlusPlanIntro(userId)).on) {
            val subscription = subscription(userId, PLAN_NAME_PLUS, PlanCycle.MONTHLY)
            if (subscription != null) {
                return@coRunCatching subscription
            }
        }

        if (getFeatureFlag(FeatureFlagId.driveOneDollarPlanUpsell(userId)).on) {
            subscription(userId, PLAN_NAME_LITE, PlanCycle.MONTHLY)
        } else {
            null
        }
    }

    private suspend fun subscription(
        userId: UserId,
        name: String,
        cycle: PlanCycle,
    ): UserOverlay.Subcription? {
        val isFreeUser = getUser(userId, false).hasSubscription().not()
        val plans = getDynamicPlansAdjustedPrices(userId).plans
        val lastUpdate = getUserDataStore(userId).get(subscriptionLastUpdate(name))
        val hasPlan = plans.any { plan -> plan.name == name && cycle.value in plan.instances.keys }
        return if (isFreeUser && hasPlan && lastUpdate == null) {
            UserOverlay.Subcription(name)
        } else {
            null
        }
    }


    private companion object {
        const val PLAN_NAME_LITE = "drivelite2024"
        const val PLAN_NAME_PLUS = "drive2022"
    }
}
