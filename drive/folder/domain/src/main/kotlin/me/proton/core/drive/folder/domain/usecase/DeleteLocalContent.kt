/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.folder.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DeleteLocalContent @Inject constructor(
    private val getLink: GetLink,
    private val getAllFolderChildren: GetAllFolderChildren,
    private val getCacheFolder: GetCacheFolder,
    private val getPermanentFolder: GetPermanentFolder,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ): Result<Unit> = coRunCatching {
        invoke(
            volumeId = volumeId,
            file = getLink(fileId).toResult().getOrThrow(),
            coroutineContext = coroutineContext
        ).getOrThrow()
    }

    suspend operator fun invoke(
        volumeId: VolumeId,
        folderId: FolderId,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ): Result<Unit> = coRunCatching {
        invoke(
            volumeId = volumeId,
            folder = getLink(folderId).toResult().getOrThrow(),
            coroutineContext = coroutineContext
        ).getOrThrow()
    }

    private suspend operator fun invoke(
        volumeId: VolumeId,
        link: Link,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ): Result<Unit> = coRunCatching {
        when (link) {
            is Link.File -> invoke(
                volumeId = volumeId,
                file = link,
                coroutineContext = coroutineContext
            ).getOrThrow()

            is Link.Folder -> invoke(
                volumeId = volumeId,
                folder = link,
                coroutineContext = coroutineContext
            ).getOrThrow()

            is Link.Album -> error("Albums are not supported with this use case")
        }
    }

    private suspend fun invoke(
        volumeId: VolumeId,
        file: Link.File,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ) = coRunCatching {
        getCacheFolder(
            userId = file.userId,
            volumeId = volumeId.id,
            revisionId = file.activeRevisionId,
            coroutineContext = coroutineContext
        ).deleteRecursively()
        getPermanentFolder(
            userId = file.userId,
            volumeId = volumeId.id,
            revisionId = file.activeRevisionId,
            coroutineContext = coroutineContext
        ).deleteRecursively()
        CoreLogger.d(LogTag.EVENTS, "Deleting local folders for: ${file.id.id.logId()}")
    }

    private suspend operator fun invoke(
        volumeId: VolumeId,
        folder: Link.Folder,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ) = coRunCatching {
        getAllFolderChildren(
            folderId = folder.id,
            block = { children ->
                children.onEach { link ->
                    invoke(volumeId, link, coroutineContext)
                }
            }
        )
    }
}
