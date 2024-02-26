/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.presentation.component

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

@Stable
class PhotosGridCells(private val minSize: Dp, private val minCount: Int) : GridCells {
    private val fixed = GridCells.Fixed(minCount)
    private val adaptive = GridCells.Adaptive(minSize)
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), 1)
        return with(if (count >= minCount) adaptive else fixed) {
            calculateCrossAxisCellSizes(availableSize, spacing)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is PhotosGridCells && minSize == other.minSize && minCount == other.minCount
    }

    override fun hashCode(): Int {
        return 31 * minSize.hashCode() + minCount
    }

}
