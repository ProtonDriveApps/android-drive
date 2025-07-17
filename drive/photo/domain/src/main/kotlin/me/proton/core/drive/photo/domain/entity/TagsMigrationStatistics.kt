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

package me.proton.core.drive.photo.domain.entity

import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.UPDATED

data class TagsMigrationStatistics(
    val data: Map<TagsMigrationFile.State, Int>,
) {
    val isFinished get() = data.isNotEmpty() && data.keys.all { state -> state == UPDATED }
    val count get() = data.values.sum()
    fun count(state: TagsMigrationFile.State) = data[state] ?: 0
    val progress: Float?
        get() = if (data.isEmpty()) {
            null
        } else {
            count(UPDATED).toFloat() / count
        }
}
