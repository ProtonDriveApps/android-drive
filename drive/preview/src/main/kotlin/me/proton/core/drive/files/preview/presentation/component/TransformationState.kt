/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.files.preview.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize

@Composable
fun rememberTransformationState(
    initialScale: Float = 1F,
    initialOffset: Offset = Offset.Zero,
    initialMinScale: Float = 1F,
    initialMaxScale: Float = 4F,
): TransformationState {
    return rememberSaveable(saver = TransformationState.Saver) {
        TransformationState(
            initialScale = initialScale,
            initialOffset = initialOffset,
            initialMinScale = initialMinScale,
            initialMaxScale = initialMaxScale,
        )
    }
}

class TransformationState(
    private val initialScale: Float = 1F,
    private val initialOffset: Offset = Offset.Zero,
    val initialMinScale: Float = 1F,
    val initialMaxScale: Float = 4F,
) {

    var containerLayout: LayoutCoordinates? = null
    var contentLayout: LayoutCoordinates? = null
    set(value) {
        // Do not take LayoutCoordinates with a size of zero it would stop updating the offset
        if(value?.size != IntSize.Zero) {
            field = value
        }
    }

    private var _scale by mutableStateOf(initialScale)
    var scale
        get() = _scale
        set(value) {
            _scale = value.coerceIn(minScale, maxScale)
        }
    var offset by mutableStateOf(initialOffset)
    var minScale by mutableStateOf(initialMinScale)
    var maxScale by mutableStateOf(initialMaxScale)

    fun hasScale() = scale > 1F

    fun addOffset(dragAmount: Offset) {
        val containerSize = containerLayout?.size
        val contentSize = contentLayout?.size
        if (containerSize == null || contentSize == null) {
            // let the offset go outside bounds sizes are not set
            offset += dragAmount
        } else {
            val horizontalLimit = ((contentSize.width * scale - containerSize.width) / 2F)
                .coerceAtLeast(0F)
            val verticalLimit = ((contentSize.height * scale - containerSize.height) / 2F)
                .coerceAtLeast(0F)

            val offsetX = (offset.x + dragAmount.x)
                .coerceIn(minimumValue = -horizontalLimit, maximumValue = horizontalLimit)
            val offsetY = (offset.y + dragAmount.y)
                .coerceIn(minimumValue = -verticalLimit, maximumValue = verticalLimit)
            offset = Offset(x = offsetX, y = offsetY)
        }
    }

    companion object {
        val Saver: Saver<TransformationState, *> = listSaver(
            save = {
                listOf(
                    it.scale,
                    it.offset.x,
                    it.offset.y,
                    it.minScale,
                    it.maxScale,
                )
            },
            restore = {
                TransformationState(
                    initialScale = it[0],
                    initialOffset = Offset(it[1], it[2]),
                    initialMinScale = it[3],
                    initialMaxScale = it[4],
                )
            }
        )
    }
}