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
package me.proton.core.drive.promo.data.repository

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.datastore.GetUserDataStore.Keys.blackFriday2026FreePromoLastShown
import me.proton.core.drive.base.data.datastore.GetUserDataStore.Keys.blackFriday2026UpgradePromoLastShown
import me.proton.core.drive.base.data.extension.get
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.promo.domain.entity.BlackFriday2026Offer
import me.proton.core.drive.promo.domain.repository.PromoRepository
import javax.inject.Inject

class PromoRepositoryImpl @Inject constructor(
    private val getUserDataStore: GetUserDataStore,
) : PromoRepository {

    override suspend fun getLastShown(userId: UserId, promoId: String): TimestampMs? =
        getUserDataStore(userId).get(lastShownKey(promoId))?.let(::TimestampMs)

    override suspend fun setLastShown(userId: UserId, promoId: String, timestamp: TimestampMs) {
        getUserDataStore(userId).edit { preferences ->
            preferences[lastShownKey(promoId)] = timestamp.value
        }
    }

    private fun lastShownKey(promoId: String): Preferences.Key<Long> = when (promoId) {
        BlackFriday2026Offer.FREE_PROMO_ID -> blackFriday2026FreePromoLastShown
        BlackFriday2026Offer.UPGRADE_PROMO_ID -> blackFriday2026UpgradePromoLastShown
        else -> error("No last shown key for promo $promoId")
    }
}
