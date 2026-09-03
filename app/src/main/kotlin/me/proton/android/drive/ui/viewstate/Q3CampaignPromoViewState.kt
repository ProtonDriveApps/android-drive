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

package me.proton.android.drive.ui.viewstate

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import me.proton.core.drive.base.presentation.common.Action

data class Q3CampaignPromoViewState(
    @param:DrawableRes val backgroundResId: Int,
    val closeAction: Action,
    val comparisonRows: List<ComparisonRow> = emptyList(),
    @param:StringRes val getDealButtonResId: Int,
    val totalPrice: String,
    val monthlyPrice: String,
    val period: String,
    val monthlyPricePeriod: String,
    val autoRenewPrice: String,
    val productId: String? = null,
    val offerToken: String? = null,
    val isPaymentLoading: Boolean = false,
) {
    /**
     * A single row of the portrait Free vs. Unlimited comparison table.
     */
    data class ComparisonRow(
        val label: String,
        val free: CellValue,
        val unlimited: CellValue,
    )

    sealed class CellValue {
        data class Check(val included: Boolean) : CellValue()
        data class Value(val text: String, val subtitle: String? = null) : CellValue()
    }
}
