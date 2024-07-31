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
package me.proton.core.drive.linkdownload.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.data.db.LinkDownloadDao
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadState
import me.proton.core.drive.linkdownload.data.extension.toDownloadState
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.repository.LinkDownloadRepository

class LinkDownloadRepositoryImpl(
    private val db: LinkDownloadDao,
) : LinkDownloadRepository {

    override fun getDownloadStateFlow(
        linkId: LinkId,
        revisionId: String,
    ): Flow<DataResult<DownloadState?>> =
        db.getDownloadStateWithBlocksFlow(linkId.userId, linkId.shareId.id, linkId.id, revisionId)
            .map { downloadStateWithBlocks ->
                downloadStateWithBlocks.toDownloadState().asSuccess
            }

    override suspend fun insertOrUpdateDownloadState(
        linkId: LinkId,
        revisionId: String,
        downloadState: DownloadState,
    ) =
        db.insertOrUpdate(linkId.userId, linkId.shareId.id, linkId.id, revisionId, downloadState)

    override suspend fun removeDownloadState(linkId: LinkId, revisionId: String) =
        db.delete(linkId.userId, linkId.shareId.id, linkId.id, revisionId)

    override suspend fun areAllFilesDownloaded(folderId: FolderId, excludeMimeTypes: Set<String>): Boolean =
        db.getAllChildrenStates(folderId.userId, folderId.shareId.id, folderId.id, excludeMimeTypes)
            .all { state -> state == LinkDownloadState.DOWNLOADED }
}
