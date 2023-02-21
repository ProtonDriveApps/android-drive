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

package me.proton.core.drive.drivelink.list.domain.extension

import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.folder.domain.entity.Sorting as FolderSorting

fun Sorting.toFolderSorting() = FolderSorting(
    by = when (by) {
        By.NAME -> FolderSorting.By.LAST_MODIFIED
        By.LAST_MODIFIED -> FolderSorting.By.LAST_MODIFIED
        By.SIZE -> FolderSorting.By.SIZE
        By.TYPE -> FolderSorting.By.TYPE
    },
    direction = when (direction) {
        Direction.ASCENDING -> FolderSorting.Direction.ASCENDING
        Direction.DESCENDING -> FolderSorting.Direction.DESCENDING
    },
)
