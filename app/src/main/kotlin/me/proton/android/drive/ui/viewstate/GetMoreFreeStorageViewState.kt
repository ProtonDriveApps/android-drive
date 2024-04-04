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

package me.proton.android.drive.ui.viewstate

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString

@Immutable
data class GetMoreFreeStorageViewState(
    @DrawableRes val imageResId: Int,
    val title: String,
    @StringRes val descriptionResId: Int,
    val actions: List<Action>,
) {
    data class Action(
        @DrawableRes val iconResId: Int,
        @StringRes val titleResId: Int,
        val getDescription: @Composable () -> AnnotatedString,
        val isDone: Boolean,
        val onSubtitleClick: (Int) -> Unit = {},
    )
}
