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
package me.proton.core.drive.linkdownload.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState

interface LinkDownloadRepository {

    /**
     * Get reactive [DownloadState] for given [linkId] and [revisionId]
     */
    fun getDownloadStateFlow(
        linkId: LinkId,
        revisionId: String,
    ): Flow<DataResult<DownloadState?>>

    /**
     * Inserts or updates given [linkId] and [revisionId] with the given [downloadState]
     */
    suspend fun insertOrUpdateDownloadState(
        linkId: LinkId,
        revisionId: String,
        downloadState: DownloadState,
    )

    /**
     * Removes [DownloadState] for given [linkId] and [revisionId]
     */
    suspend fun removeDownloadState(
        linkId: LinkId,
        revisionId: String,
    )

    /**
     * Tells if all the links within a given [folderId] are downloaded
     */
    suspend fun areAllFilesDownloaded(folderId: FolderId): Boolean
}
