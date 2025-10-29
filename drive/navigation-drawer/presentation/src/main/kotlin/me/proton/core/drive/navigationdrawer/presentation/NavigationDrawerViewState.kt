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
package me.proton.core.drive.navigationdrawer.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import me.proton.core.user.domain.entity.User

@Immutable
data class NavigationDrawerViewState(
    @StringRes val appNameResId: Int,
    val appVersion: String,
    val closeOnBackEnabled: Boolean = true,
    val closeOnActionEnabled: Boolean = true,
    val currentUser: User? = null,
    val showGetFreeStorage: Boolean = false,
    val showSubscription: Boolean = false,
    val isBlackFridayPromoEnabled: Boolean = false,
)
