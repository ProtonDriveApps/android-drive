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
package me.proton.core.drive.drivelink.download.domain.usecase

import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.usecase.MoveToCache
import me.proton.core.drive.folder.domain.usecase.GetDescendants
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.linkdownload.domain.usecase.RemoveDownloadState
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class DownloadCleanup @Inject constructor(
    private val getLink: GetLink,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
    private val moveToCache: MoveToCache,
    private val removeDownloadState: RemoveDownloadState,
    private val getDescendants: GetDescendants,
) {
    suspend operator fun invoke(volumeId: VolumeId, linkId: LinkId): Result<Unit> = coRunCatching {
        cleanup(
            volumeId = volumeId,
            link = getLink(linkId, flowOf(false)).toResult().getOrThrow(),
            includingDescendants = true,
        )
    }

    private suspend fun cleanup(
        volumeId: VolumeId,
        link: Link,
        includingDescendants: Boolean,
    ) {
        if (!isLinkOrAnyAncestorMarkedAsOffline(link.id)) {
            when (link) {
                is Link.Folder -> folderCleanup(volumeId, link, includingDescendants)
                is Link.File -> fileCleanup(volumeId, link)
            }
        }
    }

    private suspend fun fileCleanup(
        volumeId: VolumeId,
        fileLink: Link.File,
    ) {
        removeDownloadState(fileLink)
        moveToCache(fileLink.id.userId, volumeId, fileLink.activeRevisionId)
    }

    private suspend fun folderCleanup(
        volumeId: VolumeId,
        folderLink: Link.Folder,
        includingDescendants: Boolean,
    ) {
        removeDownloadState(folderLink)
        if (includingDescendants) {
            getDescendants(
                folderLink = folderLink,
                refresh = false,
            )
                .onSuccess { descendants ->
                    descendants.forEach { link ->
                        cleanup(
                            volumeId = volumeId,
                            link = link,
                            includingDescendants = false
                        )
                    }
                }
        }
    }
}
