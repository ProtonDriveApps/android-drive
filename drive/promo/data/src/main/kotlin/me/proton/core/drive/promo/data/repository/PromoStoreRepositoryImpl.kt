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

import me.proton.android.payment.product.model.Offer
import me.proton.android.payment.product.model.Product
import me.proton.android.payment.product.usecase.GetProducts
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.promo.domain.entity.PromoStoreOffer
import me.proton.core.drive.promo.domain.repository.PromoStoreRepository
import javax.inject.Inject

class PromoStoreRepositoryImpl @Inject constructor(
    private val getProducts: GetProducts,
) : PromoStoreRepository {

    override suspend fun getIntroductoryOffer(userId: UserId, planName: String, tag: String): PromoStoreOffer? =
        getProducts()
            .getOrThrow()
            .filter { product -> product.planId == planName && product.recurringPeriod == ANNUAL_PERIOD }
            .firstNotNullOfOrNull { product -> product.toPromoStoreOffer(tag) }

    private val Product.recurringPeriod: String?
        get() = offers.filterIsInstance<Offer.NonDiscounted>().firstOrNull()?.pricingPhases?.lastOrNull()?.period

    private fun Product.toPromoStoreOffer(tag: String): PromoStoreOffer? {
        val offer = offers.filterIsInstance<Offer.Discounted>().firstOrNull { offer -> tag in offer.tags }
            ?: return null
        val introductoryPhase = offer.pricingPhases.firstOrNull() ?: return null
        val renewalPhase = offer.pricingPhases.last()
        return PromoStoreOffer(
            productId = id,
            offerToken = offer.token,
            introductoryPriceMicros = introductoryPhase.price.amount,
            renewalPriceMicros = renewalPhase.price.amount,
            currency = introductoryPhase.price.currency,
        )
    }

    private companion object {
        const val ANNUAL_PERIOD = "P1Y"
    }
}
