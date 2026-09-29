/*
 * Copyright (c) 2026 Proton AG.
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
package me.proton.core.drive.drivelink.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.nodeContext
import me.proton.core.drive.drivelink.domain.extension.nodeUid
import me.proton.core.drive.link.domain.entity.AlbumContext
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileContext
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderContext
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.NodeContext
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class GetNodeContext @Inject constructor(
    private val getShare: GetShare,
) {
    operator fun invoke(driveLink: DriveLink): NodeContext = driveLink.nodeContext

    @JvmName("invokeDriveLinks")
    operator fun invoke(driveLinks: List<DriveLink>): Map<LinkId, NodeContext> =
        driveLinks.associateOrdered { driveLink -> driveLink.id to invoke(driveLink) }

    suspend operator fun invoke(linkId: LinkId): Result<NodeContext> = coRunCatching {
        linkId.nodeContext(share(linkId.shareId))
    }

    suspend operator fun invoke(folderId: FolderId): Result<FolderContext> = coRunCatching {
        share(folderId.shareId).let { share ->
            FolderContext(folderId.userId, folderId.nodeUid(share.volumeId), share.volumeType)
        }
    }

    suspend operator fun invoke(linkIds: List<LinkId>): Result<Map<LinkId, NodeContext>> =
        coRunCatching {
            val sharesById = linkIds.shares()
            linkIds.associateWithOrdered { linkId ->
                linkId.nodeContext(sharesById.getValue(linkId.shareId))
            }
        }

    private fun LinkId.nodeContext(share: Share): NodeContext = when (this) {
        is FileId -> FileContext(userId, nodeUid(share.volumeId), share.volumeType)
        is FolderId -> FolderContext(userId, nodeUid(share.volumeId), share.volumeType)
        is AlbumId -> AlbumContext(userId, nodeUid(share.volumeId), share.volumeType)
    }

    private suspend fun share(shareId: ShareId): Share =
        getShare(shareId).toResult().getOrThrow()

    private suspend fun List<LinkId>.shares(): Map<ShareId, Share> =
        map { linkId -> linkId.shareId }
            .distinct()
            .associateWith { shareId -> share(shareId) }
}

private inline fun <T, V> List<T>.associateWithOrdered(value: (T) -> V): Map<T, V> =
    associateWithTo(LinkedHashMap(size), value)

private inline fun <T, K, V> List<T>.associateOrdered(transform: (T) -> Pair<K, V>): Map<K, V> =
    associateTo(LinkedHashMap(size), transform)
