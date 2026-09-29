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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.function.pagedList
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.data.db.LinkDownloadDatabase
import me.proton.core.drive.linkdownload.data.db.dao.LinkDownloadDao
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadFileSignatureVerificationFailedEntity
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadState
import me.proton.core.drive.linkdownload.data.extension.toDownloadState
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.repository.LinkDownloadRepository

class LinkDownloadRepositoryImpl(
    private val db: LinkDownloadDatabase,
    private val configurationProvider: ConfigurationProvider,
) : LinkDownloadRepository {

    override fun getDownloadStateFlow(
        linkId: LinkId,
        revisionId: String,
    ): Flow<DataResult<DownloadState?>> = db.linkDownloadDao.getDownloadStateFlow(
        userId = linkId.userId,
        shareId = linkId.shareId.id,
        linkId = linkId.id,
        revisionId = revisionId,
    ).map { entity ->
        entity?.toDownloadState().asSuccess
    }

    override suspend fun insertOrUpdateDownloadState(
        linkId: LinkId,
        revisionId: String,
        downloadState: DownloadState,
    ) = db.linkDownloadDao.insertOrUpdate(
        userId = linkId.userId,
        shareId = linkId.shareId.id,
        linkId = linkId.id,
        revisionId = revisionId,
        downloadState = downloadState,
    )

    override suspend fun removeDownloadState(linkId: LinkId, revisionId: String) =
        db.linkDownloadDao.delete(
            userId = linkId.userId,
            shareId = linkId.shareId.id,
            linkId = linkId.id,
            revisionId = revisionId,
        )

    override suspend fun areAllFilesDownloaded(
        folderId: FolderId,
        excludeMimeTypes: Set<String>,
    ): Boolean = pagedList(configurationProvider.dbPageSize) { fromIndex, count ->
        db.linkDownloadDao.getAllChildrenStates(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            excludeMimeTypes = excludeMimeTypes,
            limit = count,
            offset = fromIndex,
        )
    }.all { state -> state in downloadedStates }

    override suspend fun areAllAlbumPhotosDownloaded(
        albumId: AlbumId,
    ): Boolean = pagedList(configurationProvider.dbPageSize) { fromIndex, count ->
        db.linkDownloadDao.getAllAlbumChildrenStates(
            userId = albumId.userId,
            albumId = albumId.id,
            limit = count,
            offset = fromIndex,
        )
    }.all { state -> state in downloadedStates }

    private val downloadedStates = listOf(LinkDownloadState.DOWNLOADED, LinkDownloadState.READY)

    override fun getDownloadingCountFlow(userId: UserId) = db.linkDownloadDao.getDownloadingCountFlow(userId)

    override suspend fun hasSignatureVerificationFailed(fileId: FileId): Boolean =
        db.linkDownloadFileSignatureVerificationFailedDao.hasFileSignatureVerificationFailed(
            userId = fileId.userId,
            shareId = fileId.shareId.id,
            fileId = fileId.id,
        )

    override suspend fun removeSignatureVerificationFailed(fileId: FileId) =
        db.linkDownloadFileSignatureVerificationFailedDao.delete(
            LinkDownloadFileSignatureVerificationFailedEntity(
                userId = fileId.userId,
                shareId = fileId.shareId.id,
                linkId = fileId.id,
            )
        )

    override suspend fun setSignatureVerificationFailed(fileId: FileId) =
        db.linkDownloadFileSignatureVerificationFailedDao.insertOrIgnore(
            LinkDownloadFileSignatureVerificationFailedEntity(
                userId = fileId.userId,
                shareId = fileId.shareId.id,
                linkId = fileId.id,
            )
        )
}
