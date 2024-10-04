/*
 * Copyright (c) 2024 Proton AG.
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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun NotificationDot(
    modifier: Modifier = Modifier,
    outerRadiusDp: Dp = 5.dp,
    innerRadiusDp: Dp = 3.dp,
    outerColor: Color = ProtonTheme.colors.notificationError.copy(
        alpha = 0.3f
    ),
    innerColor: Color = ProtonTheme.colors.notificationError
) {
    val outerRadiusPx = with(LocalDensity.current) { outerRadiusDp.toPx() }
    val innerRadiusPx = with(LocalDensity.current) { innerRadiusDp.toPx() }
    Canvas(modifier = modifier.size(outerRadiusDp * 2)) {
        // Draw the outer circle
        drawCircle(
            color = outerColor,
            radius = outerRadiusPx
        )
        // Draw the inner circle
        drawCircle(
            color = innerColor,
            radius = innerRadiusPx,
            center = center
        )
    }
}

@Preview
@Composable
fun PreviewNotificationDot() {
    ProtonTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            NotificationDot()
        }
    }
}
