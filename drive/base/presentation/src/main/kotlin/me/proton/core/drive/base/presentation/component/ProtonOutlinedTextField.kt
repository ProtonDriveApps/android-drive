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
package me.proton.core.drive.base.presentation.component

import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun TextFieldDefaults.protonOutlineTextFieldColors(): TextFieldColors =
    outlinedTextFieldColors(
        textColor = ProtonTheme.colors.textNorm,
        backgroundColor = ProtonTheme.colors.backgroundSecondary,

        focusedLabelColor = ProtonTheme.colors.textNorm,
        focusedBorderColor = ProtonTheme.colors.brandNorm,

        unfocusedLabelColor = ProtonTheme.colors.textHint,
        unfocusedBorderColor = ProtonTheme.colors.backgroundSecondary,

        disabledLabelColor = ProtonTheme.colors.textDisabled,
        disabledBorderColor = ProtonTheme.colors.backgroundSecondary,

        errorLabelColor = ProtonTheme.colors.notificationError,
        errorBorderColor = ProtonTheme.colors.notificationError,
    )
