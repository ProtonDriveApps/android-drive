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
package me.proton.core.drive.folder.data.extension

import me.proton.core.drive.base.data.api.Dto.MIME_TYPE
import me.proton.core.drive.base.data.api.Dto.MODIFY_TIME
import me.proton.core.drive.base.data.api.Dto.SIZE
import me.proton.core.drive.folder.domain.entity.Sorting

fun Sorting.By.toDtoSort(): String = when (this) {
    Sorting.By.LAST_MODIFIED -> MODIFY_TIME
    Sorting.By.SIZE -> SIZE
    Sorting.By.TYPE -> MIME_TYPE
}

fun Sorting.Direction.toDtoDesc(): Int = when (this) {
    Sorting.Direction.DESCENDING -> 1
    Sorting.Direction.ASCENDING -> 0
}
