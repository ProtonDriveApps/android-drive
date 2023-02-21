/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.files.presentation.state

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class ListContentState {
    abstract val isRefreshing: Boolean

    object Loading : ListContentState() {
        override val isRefreshing = false
    }

    data class Empty(
        @DrawableRes val imageResId: Int,
        @StringRes val titleId: Int,
        @StringRes val descriptionResId: Int = 0,
        @StringRes val actionResId: Int = 0,
        override val isRefreshing: Boolean = false,
    ) : ListContentState()

    data class Content(override val isRefreshing: Boolean = false) : ListContentState()
    data class Error(
        val message: String,
        @StringRes val actionResId: Int? = null,
        override val isRefreshing: Boolean = false,
    ) : ListContentState()
}
