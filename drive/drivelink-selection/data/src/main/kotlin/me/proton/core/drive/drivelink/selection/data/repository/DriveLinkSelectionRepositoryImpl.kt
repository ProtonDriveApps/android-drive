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
package me.proton.core.drive.drivelink.selection.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.data.extension.toDriveLinks
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.selection.data.db.DriveLinkSelectionDatabase
import me.proton.core.drive.drivelink.selection.domain.repository.DriveLinkSelectionRepository
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import javax.inject.Inject

class DriveLinkSelectionRepositoryImpl @Inject constructor(
    private val db: DriveLinkSelectionDatabase,
    private val configurationProvider: ConfigurationProvider,
) : DriveLinkSelectionRepository {

    override fun getSelectedDriveLinks(selectionId: SelectionId): Flow<List<DriveLink>> =
        db.driveLinkSelectionDao.getSelectedLinks(selectionId).map { entities ->
            entities.toDriveLinks()
        }

    override suspend fun selectAll(
        parentId: ParentId,
        selectionId: SelectionId?,
        getDriveLinks: (fromIndex: Int, count: Int) -> Flow<List<DriveLink>>,
        selectLinks: suspend (SelectionId?, List<LinkId>) -> Result<SelectionId>,
    ) {
        val pageSize = configurationProvider.dbPageSize
        var id = selectionId
        db.inTransaction {
            var pageIndex = 0
            while (true) {
                val driveLinks = getDriveLinks(
                    pageIndex++ * pageSize,
                    pageSize,
                )
                    .map { driveLinks -> driveLinks.map { driveLink -> driveLink.id } }
                    .first()
                if (driveLinks.isNotEmpty()) {
                    id = selectLinks(
                        id,
                        driveLinks,
                    ).getOrNull()
                } else {
                    break
                }
            }
        }
    }
}
