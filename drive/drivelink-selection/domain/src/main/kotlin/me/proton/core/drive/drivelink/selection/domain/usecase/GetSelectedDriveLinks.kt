/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.drivelink.selection.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.selection.domain.repository.DriveLinkSelectionRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import javax.inject.Inject

class GetSelectedDriveLinks @Inject constructor(
    private val repository: DriveLinkSelectionRepository,
    private val deselectLinks: DeselectLinks,
) {

    operator fun invoke(selectionId: SelectionId): Flow<List<DriveLink>> =
        repository.getSelectedDriveLinks(selectionId)

    operator fun invoke(selectionId: SelectionId, parentId: FolderId): Flow<List<DriveLink>> =
        repository
            .getSelectedDriveLinks(selectionId)
            .map { driveLinks ->
                driveLinks.filter { driveLink -> driveLink.parentId == parentId }
                    .also { children ->
                        deselectLinks(
                            selectionId,
                            (driveLinks - children.toSet()).map { driveLink -> driveLink.id },
                        )
                    }
            }
}
