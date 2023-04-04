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
package me.proton.core.drive.drivelink.selection.domain.usecase

import me.proton.core.drive.drivelink.domain.repository.DriveLinkRepository
import me.proton.core.drive.drivelink.selection.domain.repository.DriveLinkSelectionRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import javax.inject.Inject

class SelectAll @Inject constructor(
    private val selectLinks: SelectLinks,
    private val driveLinkSelectionRepository: DriveLinkSelectionRepository,
    private val driveLinkRepository: DriveLinkRepository,
) {

    suspend operator fun invoke(parentId: FolderId, selectionId: SelectionId?) = driveLinkSelectionRepository.selectAll(
        parentId = parentId,
        selectionId = selectionId,
        getDriveLinks = { fromIndex, count ->
            driveLinkRepository.getDriveLinks(parentId, fromIndex, count)
        },
        selectLinks = { id, linkIds ->
            id?.let {
                selectLinks(id, linkIds)
                Result.success(id)
            } ?: selectLinks(linkIds)
        }
    )
}
