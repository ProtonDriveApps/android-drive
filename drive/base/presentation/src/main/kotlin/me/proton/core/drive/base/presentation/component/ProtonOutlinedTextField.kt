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

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import me.proton.core.compose.theme.ProtonTheme

@Suppress("UnusedReceiverParameter")
@Composable
fun TextFieldDefaults.protonOutlineTextFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = ProtonTheme.colors.textNorm,
        unfocusedTextColor = ProtonTheme.colors.textNorm,

        focusedContainerColor = ProtonTheme.colors.backgroundSecondary,
        unfocusedContainerColor = ProtonTheme.colors.backgroundSecondary,
        disabledContainerColor = ProtonTheme.colors.backgroundSecondary,
        errorContainerColor = ProtonTheme.colors.backgroundSecondary,

        focusedLabelColor = ProtonTheme.colors.textNorm,
        focusedBorderColor = ProtonTheme.colors.brandNorm,

        unfocusedLabelColor = ProtonTheme.colors.textHint,
        unfocusedBorderColor = ProtonTheme.colors.backgroundSecondary,

        disabledLabelColor = ProtonTheme.colors.textDisabled,
        disabledBorderColor = ProtonTheme.colors.backgroundSecondary,

        errorLabelColor = ProtonTheme.colors.notificationError,
        errorBorderColor = ProtonTheme.colors.notificationError,
    )
