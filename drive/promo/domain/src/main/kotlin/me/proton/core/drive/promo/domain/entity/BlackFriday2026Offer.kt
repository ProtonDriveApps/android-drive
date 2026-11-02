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
package me.proton.core.drive.promo.domain.entity

sealed interface BlackFriday2026Offer {
    val planName: String
    val promoId: String
    val storeOffer: PromoStoreOffer

    data class Free(override val storeOffer: PromoStoreOffer) : BlackFriday2026Offer {
        override val planName: String get() = FREE_PLAN_NAME
        override val promoId: String get() = FREE_PROMO_ID
    }

    data class Upgrade(override val storeOffer: PromoStoreOffer) : BlackFriday2026Offer {
        override val planName: String get() = UPGRADE_PLAN_NAME
        override val promoId: String get() = UPGRADE_PROMO_ID
    }

    companion object {
        const val OFFER_TAG = "bf-2026"
        const val FREE_PLAN_NAME = "drive2022"
        const val UPGRADE_PLAN_NAME = "bundle2022"
        const val FREE_PROMO_ID = "black_friday_2026_free"
        const val UPGRADE_PROMO_ID = "black_friday_2026_upgrade"
    }
}
