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
import androidx.compose.runtime.Immutable
import me.proton.core.drive.base.presentation.common.Action

@Immutable
data class BlackFriday2026PromoViewState(
    @param:DrawableRes val headerResId: Int,
    @param:StringRes val headerDescriptionResId: Int,
    val closeAction: Action.Icon,
    val currentPlanColumnTitle: String,
    val planColumnTitle: String,
    val comparisonRows: List<ComparisonRow>,
    val period: String,
    val discount: String,
    val price: String? = null,
    val monthlyPrice: String? = null,
    val footer: String? = null,
    @param:StringRes val claimOfferButtonResId: Int,
    val productId: String? = null,
    val offerToken: String? = null,
    @param:StringRes val stayOnPlanButtonResId: Int? = null,
    val isPaymentLoading: Boolean = false,
) {
    data class ComparisonRow(
        val label: String,
        val currentPlan: CellValue,
        val plan: CellValue,
    )

    sealed interface CellValue {
        data class Check(val included: Boolean) : CellValue
        data class Value(val text: String, val subtitle: String? = null) : CellValue
    }
}
