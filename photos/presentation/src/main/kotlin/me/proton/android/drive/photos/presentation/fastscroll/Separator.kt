/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.presentation.fastscroll

import me.proton.android.drive.photos.presentation.state.PhotosItem

data class SeparatorWithIndex(
    val separator: PhotosItem.Separator,
    val index: Int,
)

data class SeparatorWithIndexAndCount(
    val separatorWithIndex: SeparatorWithIndex,
    val count: Int,
)

data class YearSeparators(
    val year: Int,
    val separators: List<SeparatorWithIndexAndCount>,
    val count: Int,
)
