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

package me.proton.core.drive.drivelink.offline.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.folder.domain.usecase.GetAllFolderChildren
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.usecase.GetAllAlbumDirectChildren
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DeleteLocalContent @Inject constructor(
    private val getCacheFolder: GetCacheFolder,
    private val getPermanentFolder: GetPermanentFolder,
    private val getAllFolderChildren: GetAllFolderChildren,
    private val getAllAlbumDirectChildren: GetAllAlbumDirectChildren,
    private val getDriveLink: GetDriveLink,
) {

    suspend operator fun invoke(
        fileId: FileId,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ) = coRunCatching {
        invoke(
            file = getDriveLink(fileId).toResult().getOrThrow(),
            coroutineContext = coroutineContext,
        ).getOrThrow()
    }

    suspend operator fun invoke(
        folderId: FolderId,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ) = coRunCatching {
        invoke(
            folder = getDriveLink(folderId).toResult().getOrThrow(),
            coroutineContext = coroutineContext,
        ).getOrThrow()
    }

    suspend operator fun invoke(
        albumId: AlbumId,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ) = coRunCatching {
        invoke(
            album = getDriveLink(albumId).toResult().getOrThrow(),
            coroutineContext = coroutineContext,
        ).getOrThrow()
    }

    suspend operator fun invoke(
        file: DriveLink.File,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ): Result<Unit> = coRunCatching {
        CoreLogger.d(LogTag.EVENTS, "Deleting local folders for: ${file.id.id.logId()}")
        getCacheFolder(file.userId, file.volumeId.id, file.activeRevisionId, coroutineContext).deleteRecursively()
        getPermanentFolder(file.userId, file.volumeId.id, file.activeRevisionId, coroutineContext).deleteRecursively()
    }

    suspend operator fun invoke(
        folder: DriveLink.Folder,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ): Result<Unit> = coRunCatching {
        getAllFolderChildren(folder.id)
            .getOrThrow()
            .map { link -> link.id }
            .forEach { linkId ->
                when (linkId) {
                    is FileId -> invoke(linkId, coroutineContext)
                        .getOrNull(
                            tag = LogTag.EVENTS,
                            message = "Failed delete content for file ${linkId.id.logId()}",
                        )
                    is FolderId -> invoke(linkId, coroutineContext)
                        .getOrNull(
                            tag = LogTag.EVENTS,
                            message = "Failed delete content for folder ${linkId.id.logId()}",
                        )
                    is AlbumId -> invoke(linkId, coroutineContext)
                        .getOrNull(
                            tag = LogTag.EVENTS,
                            message = "Failed delete content for album ${linkId.id.logId()}",
                        )
                }
            }
    }

    suspend operator fun invoke(
        album: DriveLink.Album,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ): Result<Unit> = coRunCatching {
        getAllAlbumDirectChildren(album.volumeId, album.id)
            .getOrThrow()
            .forEach { photoId ->
                invoke(photoId, coroutineContext).getOrNull(
                    tag = LogTag.EVENTS,
                    message = "Failed delete content for photo ${photoId.id.logId()}",
                )
            }
    }
}
