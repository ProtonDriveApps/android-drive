/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.presentation.viewstate

import androidx.annotation.StringRes

data class ConfirmLeaveAlbumDialogViewState(
    val title: String,
    val description: String,
    @StringRes val dismissButtonResId: Int,
    @StringRes val confirmWithoutSavingButtonResId: Int,
    @StringRes val confirmSaveAndLeaveButtonResId: Int,
    val isWithoutSavingOperationInProgress: Boolean = false,
    val isSavingOperationInProgress: Boolean = false,
    val isSaveAndLeaveButtonVisible: Boolean = true,
)
