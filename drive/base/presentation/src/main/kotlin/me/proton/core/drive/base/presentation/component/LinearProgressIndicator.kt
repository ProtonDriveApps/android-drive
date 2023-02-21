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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun IndeterminateProgressIndicator(
    modifier: Modifier = Modifier,
    height: Dp = ProgressHeight,
    progressColors: Color = ProtonTheme.colors.brandNorm,
    backgroundColor: Color = ProtonTheme.colors.backgroundSecondary,
) {
    LinearProgressIndicator(
        progress = null,
        modifier = modifier,
        height = height,
        progressColors = progressColors,
        backgroundColor = backgroundColor,
    )
}

@Composable
fun LinearProgressIndicator(
    progress: Float?,
    modifier: Modifier = Modifier,
    height: Dp = ProgressHeight,
    progressColors: Color = ProtonTheme.colors.brandNorm,
    backgroundColor: Color = ProtonTheme.colors.backgroundSecondary,
) {
    val progressModifier = modifier
        .fillMaxWidth()
        .height(height)
    if (progress != null) {
        LinearProgressIndicator(
            progress = progress,
            modifier = progressModifier,
            color = progressColors,
            backgroundColor = backgroundColor
        )
    } else {
        LinearProgressIndicator(
            modifier = progressModifier,
            color = progressColors,
            backgroundColor = backgroundColor
        )
    }
}

val ProgressHeight = 1.dp
