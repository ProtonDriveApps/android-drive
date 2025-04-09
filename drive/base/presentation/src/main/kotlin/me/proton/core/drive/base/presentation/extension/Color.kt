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

import androidx.compose.ui.graphics.Color

fun Color.blendToSolid(alpha: Float, bg: Color = Color.White): Color =
    alpha.coerceIn(0f, 1f).let { alpha ->
        Color(
            red = (red * alpha) + (bg.red * (1 - alpha)),
            green = (green * alpha) + (bg.green * (1 - alpha)),
            blue = (blue * alpha) + (bg.blue * (1 - alpha)),
            alpha = 1f,
        )
    }
