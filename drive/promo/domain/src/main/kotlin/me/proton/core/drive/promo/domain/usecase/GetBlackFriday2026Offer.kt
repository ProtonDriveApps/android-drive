/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.core.drive.promo.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.PROMO
import me.proton.core.drive.base.domain.util.coRunCatchingException
import me.proton.core.drive.feature.flag.domain.entity.BlackFriday2026Promo
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidBlackFridayUpgradeOptOut
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.feature.flag.domain.usecase.IsBlackFriday2026PromoEnabled
import me.proton.core.drive.feature.flag.domain.usecase.RefreshFeatureFlag
import me.proton.core.drive.promo.domain.entity.BlackFriday2026Offer
import me.proton.core.drive.promo.domain.repository.PromoStoreRepository
import me.proton.core.drive.user.domain.usecase.GetInAppNotificationsEnabled
import me.proton.core.plan.domain.entity.SubscriptionManagement
import me.proton.core.plan.domain.usecase.GetDynamicSubscription
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.Delinquent
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.extension.hasSubscription
import me.proton.core.util.kotlin.CoreLogger
import java.time.Instant
import javax.inject.Inject

class GetBlackFriday2026Offer @Inject constructor(
    private val isBlackFriday2026PromoEnabled: IsBlackFriday2026PromoEnabled,
    private val userManager: UserManager,
    private val getDynamicSubscription: GetDynamicSubscription,
    private val getInAppNotificationsEnabled: GetInAppNotificationsEnabled,
    private val promoStoreRepository: PromoStoreRepository,
    private val getFeatureFlagFlow: GetFeatureFlagFlow,
    private val refreshFeatureFlag: RefreshFeatureFlag,
) {

    operator fun invoke(userId: UserId): Flow<BlackFriday2026Offer?> = invoke(userId, Instant.now())

    internal operator fun invoke(userId: UserId, now: Instant): Flow<BlackFriday2026Offer?> = channelFlow {
        val optOutId = driveAndroidBlackFridayUpgradeOptOut(userId)
        if (now.isBefore(IsBlackFriday2026PromoEnabled.END)) {
            launch {
                refreshFeatureFlag(optOutId).getOrNull(PROMO, "Failed to refresh Black Friday 2026 upgrade opt-out")
            }
        }
        val optOutState = getFeatureFlagFlow(optOutId, refresh = { false }, emitNotFoundInitially = false)
            .map<FeatureFlag, FeatureFlag.State?> { featureFlag -> featureFlag.state }
            .onStart { emit(null) }
            .catch { error -> CoreLogger.d(PROMO, error, "Failed to observe Black Friday 2026 upgrade opt-out") }
        combine(getOffer(userId, now), optOutState) { offer, state ->
            offer.takeUnless { offer is BlackFriday2026Offer.Upgrade && state != FeatureFlag.State.DISABLED }
        }.distinctUntilChanged().collect { offer -> send(offer) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getOffer(userId: UserId, now: Instant): Flow<BlackFriday2026Offer?> =
        isBlackFriday2026PromoEnabled(userId, now)
            .flatMapLatest { enabledPromos ->
                if (enabledPromos.isEmpty()) flowOf(null) else getEligiblePromo(userId, enabledPromos)
            }
            .distinctUntilChanged()
            .mapLatest { promo -> promo?.let { getOffer(userId, promo) } }
            .catch { error ->
                CoreLogger.d(PROMO, error, "Failed to get Black Friday 2026 offer")
                emit(null)
            }

    private fun getEligiblePromo(
        userId: UserId,
        enabledPromos: Set<BlackFriday2026Promo>,
    ): Flow<BlackFriday2026Promo?> = combine(
        userManager.observeUser(userId),
        getInAppNotificationsEnabled(userId),
    ) { user, inAppNotificationsEnabled ->
        if (user == null || user.isDelinquent || !inAppNotificationsEnabled) {
            null
        } else {
            val promo = if (user.hasSubscription()) BlackFriday2026Promo.UPGRADE else BlackFriday2026Promo.FREE
            promo.takeIf { promo in enabledPromos }
        }
    }

    private suspend fun getOffer(userId: UserId, promo: BlackFriday2026Promo): BlackFriday2026Offer? =
        coRunCatchingException {
            when (promo) {
                BlackFriday2026Promo.FREE -> getIntroductoryOffer(userId, BlackFriday2026Offer.FREE_PLAN_NAME)
                    ?.let(BlackFriday2026Offer::Free)
                BlackFriday2026Promo.UPGRADE -> if (hasUpgradeableSubscription(userId)) {
                    getIntroductoryOffer(userId, BlackFriday2026Offer.UPGRADE_PLAN_NAME)
                        ?.let(BlackFriday2026Offer::Upgrade)
                } else {
                    null
                }
            }
        }.getOrNull(PROMO, "Failed to get Black Friday 2026 store offer")

    private val User.isDelinquent: Boolean get() = delinquent != null && delinquent != Delinquent.None

    private suspend fun hasUpgradeableSubscription(userId: UserId): Boolean {
        val subscription = getDynamicSubscription(userId) ?: return false
        return (subscription.name to subscription.cycleMonths) in UPGRADEABLE_PLANS &&
            subscription.external == SubscriptionManagement.GOOGLE_MANAGED &&
            subscription.renew == true &&
            subscription.couponCode == null
    }

    private suspend fun getIntroductoryOffer(userId: UserId, planName: String) =
        promoStoreRepository.getIntroductoryOffer(userId, planName, BlackFriday2026Offer.OFFER_TAG)

    private companion object {
        val UPGRADEABLE_PLANS = setOf(
            "drive2022" to 1,
            "drive2022" to 12,
            "drive1tb2026" to 1,
        )
    }
}
