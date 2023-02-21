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

package me.proton.core.drive.thumbnail.presentation.painter

import androidx.compose.ui.graphics.painter.Painter
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter

/**
 * Unfortunately, this class cannot implement [Painter] and delegate to its wrapped [painter].
 * This is because [Painter] is an abstract class which requires to override the protected method [Painter.onDraw]
 * which this class cannot call on its wrapped [painter].
 */
@Suppress("UseDataClass")
class ThumbnailPainterWrapper(
    val painter: Painter,
) {
    @OptIn(ExperimentalCoilApi::class)
    val isLoaded get() = painter is ImagePainter && painter.state is ImagePainter.State.Success
}
