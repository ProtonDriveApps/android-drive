/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.base.presentation.extension

import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun ButtonDefaults.protonNotificationSuccessButtonColors(
    loading: Boolean = false
) =
    protonButtonColors(
        backgroundColor = ProtonTheme.colors.notificationSuccess,
        contentColor = Color.White,
        disabledBackgroundColor = ProtonTheme.colors.notificationSuccess.blendToSolid(
            alpha = 0.3f,
            bg = ProtonTheme.colors.backgroundNorm,
        ),
        disabledContentColor = Color.White.copy(alpha = 0.5f),
        loading = loading,
    )

@Composable
fun ButtonDefaults.protonDriveCustomGreenButtonColors(
    loading: Boolean = false
) =
    protonButtonColors(
        backgroundColor = if (ProtonTheme.colors.isDark) {
            driveCustomGreen
        } else {
            driveCustomGreenLight
        },
        contentColor = if (ProtonTheme.colors.isDark) {
            Color.White
        } else {
            driveCustomGreen
        },
        disabledBackgroundColor = if (ProtonTheme.colors.isDark) {
            driveCustomGreen
        } else {
            driveCustomGreenLight
        }.blendToSolid(
            alpha = 0.3f,
            bg = ProtonTheme.colors.backgroundNorm,
        ),
        disabledContentColor = if (ProtonTheme.colors.isDark) {
            Color.White
        } else {
            driveCustomGreen
        }.copy(alpha = 0.5f),
        loading = loading,
    )

@Composable
fun ButtonDefaults.protonBlackFridayPromoButtonColors(
    loading: Boolean = false
) =
    protonButtonColors(
        backgroundColor = driveCustomCitrusGreen,
        contentColor = Color.Black,
        loading = loading,
    )

private val driveCustomGreen = Color(0xFF059669)
private val driveCustomGreenLight = Color(0xFFECFDF5)
val driveCustomCitrusGreen = Color(0xFFD8FF00)
