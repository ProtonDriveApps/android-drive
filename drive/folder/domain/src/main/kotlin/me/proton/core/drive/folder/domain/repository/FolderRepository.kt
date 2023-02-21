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
package me.proton.core.drive.folder.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.folder.domain.entity.FolderInfo
import me.proton.core.drive.folder.domain.entity.Sorting
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId

interface FolderRepository {

    /**
     * Check if we have cached any children for a given folder
     */
    suspend fun hasFolderChildren(folderId: FolderId): Boolean

    /**
     * Get reactive list of all children for a given folder
     */
    fun getAllFolderChildrenFlow(folderId: FolderId): Flow<DataResult<List<Link>>>

    /**
     * Fetches all children of a given folder from the server and stores it into cache
     */
    suspend fun fetchAllFolderChildren(folderId: FolderId): Result<List<Link>>

    /**
     * Fetches list of children of a given folder from the server and stores it into cache
     */
    suspend fun fetchFolderChildren(
        folderId: FolderId,
        pageIndex: Int,
        pageSize: Int,
        sorting: Sorting = Sorting.DEFAULT,
    ): Result<Pair<List<Link>, SaveAction>>

    /**
     * Creates new folder
     */
    suspend fun createFolder(shareId: ShareId, folderInfo: FolderInfo): Result<FolderId>

    /**
     * Deletes given links from folder
     */
    suspend fun deleteFolderChildren(folderId: FolderId, linkIds: List<LinkId>): Result<Unit>

    /**
     * Checks if folder children were already fetched
     */
    suspend fun shouldInitiallyFetchFolderChildren(folderId: FolderId): Boolean

    suspend fun hasFolderChildrenWithHash(folderId: FolderId, hash: String): Boolean
}
