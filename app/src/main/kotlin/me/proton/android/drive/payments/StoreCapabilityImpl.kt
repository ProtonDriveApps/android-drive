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

package me.proton.android.drive.payments

import me.proton.android.payment.billing.model.StoreProduct
import me.proton.android.payment.billing.usecase.AcknowledgePurchase
import me.proton.android.payment.billing.usecase.GetStoreProducts
import me.proton.android.payment.billing.usecase.PurchaseStoreProduct
import me.proton.android.payment.capability.StoreCapability
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreCapabilityImpl @Inject constructor(
    private val getStoreProducts: GetStoreProducts,
    private val purchaseStoreProduct: PurchaseStoreProduct,
    private val acknowledgePurchase: AcknowledgePurchase,
) : StoreCapability {

    override suspend fun getProducts(ids: List<String>): Result<List<StoreProduct>> =
        getStoreProducts(ids)

    override suspend fun purchase(
        productId: String,
        offerToken: String,
        userId: String?,
    ): Result<Unit> = purchaseStoreProduct(productId, offerToken, userId)

    override suspend fun acknowledge(orderId: String): Result<Unit> =
        acknowledgePurchase(orderId)
}
