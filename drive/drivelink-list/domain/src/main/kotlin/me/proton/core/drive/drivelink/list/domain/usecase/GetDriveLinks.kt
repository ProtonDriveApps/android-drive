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

package me.proton.core.drive.drivelink.list.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.repository.DriveLinkRepository
import me.proton.core.drive.drivelink.domain.usecase.UpdateIsAnyAncestorMarkedAsOffline
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

class GetDriveLinks @Inject constructor(
    private val repository: DriveLinkRepository,
    private val updateIsAnyAncestorMarkedAsOffline: UpdateIsAnyAncestorMarkedAsOffline,
) {
    operator fun invoke(parentId: FolderId): Flow<List<DriveLink>> =
        repository.getDriveLinks(parentId)
            .map { driveLinks -> updateIsAnyAncestorMarkedAsOffline(driveLinks) }

    operator fun invoke(parentId: FolderId, fromIndex: Int, count: Int): Flow<List<DriveLink>> =
        repository.getDriveLinks(parentId, fromIndex, count)
            .map { driveLinks -> updateIsAnyAncestorMarkedAsOffline(driveLinks) }
}
