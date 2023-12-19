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

package me.proton.core.drive.drivelink.sorting.domain.sorter

import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.sorting.domain.extension.comparableName
import me.proton.core.drive.sorting.domain.entity.Direction

data object SizeSorter : Sorter() {

    override fun sort(driveLinks: List<DriveLink>, direction: Direction): List<DriveLink> =
        driveLinks.sortedWith(
            compareBy<DriveLink> { driveLink -> if (driveLink is DriveLink.Folder) 0 else 1 }
                .thenBy { driveLink -> if (driveLink.isNameEncrypted) 0 else 1 }
                .run {
                    when (direction) {
                        Direction.ASCENDING -> thenBy { driveLink ->
                            if (driveLink is DriveLink.Folder) 0 else driveLink.size.value
                        }
                        Direction.DESCENDING -> thenByDescending { driveLink ->
                            if (driveLink is DriveLink.Folder) 0 else driveLink.size.value
                        }
                    }
                }
                .run {
                    when (direction) {
                        Direction.ASCENDING -> thenBy { driveLink -> driveLink.comparableName }
                        Direction.DESCENDING -> thenByDescending { driveLink -> driveLink.comparableName }
                    }
                }
        )
}
