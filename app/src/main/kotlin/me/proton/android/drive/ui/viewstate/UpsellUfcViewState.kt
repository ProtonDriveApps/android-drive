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

import me.proton.core.drive.base.presentation.common.Action

sealed class UpsellUfcViewState(
    open val closeAction: Action,
) {
    data class Loading(
        override val closeAction: Action,
    ) : UpsellUfcViewState(closeAction)

    data class Error(
        override val closeAction: Action,
        val message: String,
        val actionResId: Int? = null,
        val onAction: () -> Unit = {},
    ) : UpsellUfcViewState(closeAction)

    data class Content(
        override val closeAction: Action,
        val title: String,
        val subtitle: String,
        val freePlanColumnTitle: String,
        val planColumnTitle: String,
        val storageLabel: String,
        val versionHistoryLabel: String,
        val shareWithEditAccessLabel: String,
        val storageFree: String,
        val storagePlan: String,
        val storagePlanSubtitle: String,
        val versionHistoryFree: String,
        val versionHistoryPlan: String,
        val shareWithEditAccessFree: Boolean,
        val shareWithEditAccessPlan: Boolean,
        val buttonTitle: String,
        val footer: String,
        val productId: String,
        val offerToken: String,
        val isPaymentLoading: Boolean = false,
    ) : UpsellUfcViewState(closeAction)
}
