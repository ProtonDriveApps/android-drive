/*
 * Copyright (c) 2021-2024 Proton AG.
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

package me.proton.core.drive.eventmanager.usecase

import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.nullIfNotFound
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.EVENTS
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.documentsprovider.domain.usecase.NotifyDocumentChanged
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.offline.domain.usecase.UpdateOfflineContent
import me.proton.core.drive.eventmanager.entity.LinkEventVO
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.ids
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.DeleteLinks
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.link.domain.usecase.InsertOrUpdateLinks
import me.proton.core.drive.linktrash.domain.usecase.SetOrRemoveTrashState
import me.proton.core.drive.photo.domain.usecase.InsertOrDeleteAlbumListings
import me.proton.core.drive.photo.domain.usecase.InsertOrDeleteAlbumPhotoListings
import me.proton.core.drive.photo.domain.usecase.InsertOrDeletePhotoListings
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.upload.domain.usecase.CancelAllUpload
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class HandleCreateOrUpdateLinksEvent @Inject constructor(
    private val insertOrUpdateLinks: InsertOrUpdateLinks,
    private val setOrRemoveTrashState: SetOrRemoveTrashState,
    private val cancelAllUpload: CancelAllUpload,
    private val updateOfflineContent: UpdateOfflineContent,
    private val getLink: GetLink,
    private val insertOrDeletePhotoListings: InsertOrDeletePhotoListings,
    private val insertOrDeleteAlbumListings: InsertOrDeleteAlbumListings,
    private val getPhotoShare: GetPhotoShare,
    private val insertOrDeleteAlbumPhotoListings: InsertOrDeleteAlbumPhotoListings,
    private val notifyDocumentChanged: NotifyDocumentChanged,
    private val getDriveLink: GetDriveLink,
    private val deleteLinks: DeleteLinks,
) {

    suspend operator fun invoke(vos: List<LinkEventVO>) {
        if (vos.isNotEmpty()) {
            val photoShare = getPhotoShare(vos.first().link.userId)
                        .toResult()
                        .getOrNull()
            vos
                .groupBy({ vo -> vo.volumeId }) { vo -> vo.link }
                .forEach { (volumeId, links) ->
                    val staleLinksWithDifferentShare = links.staleLinksWithDifferentShare(volumeId)
                    val modifiedStateOrParentLinks = links.modifiedStateOrParentLinks()
                    insertOrUpdateLinks(links)
                    cancelAllUpload(links.filterFoldersTrashedOrDeleted())

                    setOrRemoveTrashState(volumeId, links)
                    updateOfflineContent(
                        modifiedStateOrParentLinks.ids +
                                links.filterIsInstance<Link.Album>().map { link -> link.id }
                    ).getOrNull(EVENTS, "Failed to update online content")
                    insertOrDeletePhotoListings(volumeId, links.filterVolumePhotoListings(photoShare?.rootFolderId))
                    insertOrDeleteAlbumPhotoListings(volumeId, links.filterIsInstance<Link.File>())
                    insertOrDeleteAlbumListings(volumeId, links.filterIsInstance<Link.Album>())
                    deleteLinks(staleLinksWithDifferentShare.map { link -> link.id }).getOrNull(EVENTS, "Error handling on update metadata event")
                    (links + staleLinksWithDifferentShare).forEach { link ->
                        notifyDocumentChanged(DocumentId(link.userId, link.id))
                    }
                }
        }
    }

    private fun List<Link>.filterFoldersTrashedOrDeleted(): List<Link.Folder> {
        val trashOrDeleted = listOf(Link.State.TRASHED, Link.State.DELETED)
        return filterIsInstance<Link.Folder>()
            .filter { it.state in trashOrDeleted }
    }

    private suspend fun List<Link>.modifiedStateOrParentLinks() = filter { link ->
        getLink(link.id, flowOf { false }).toResult().nullIfNotFound().fold(
            onSuccess = { staleLink ->
                if (staleLink != null) {
                    (staleLink.state != link.state) or (staleLink.parentId != link.parentId)
                } else {
                    true
                }
            },
            onFailure = { error ->
                CoreLogger.w(EVENTS, error, "Cannot get link ${link.id.id}")
                true
            }
        )
    }

    private suspend fun List<Link>.staleLinksWithDifferentShare(
        volumeId: VolumeId,
        excludedShareTypes: Set<Share.Type> = setOf(Share.Type.STANDARD),
    ) = flatMap { link ->
        getDriveLink(
            userId = link.userId,
            volumeId = volumeId,
            linkId = link.id.id,
            excludedShareTypes = excludedShareTypes,
        )
            .firstOrNull()
            ?.map { driveLink -> driveLink.link }
            ?.let { links ->
                links.filterNot { staleLink ->
                    staleLink.shareId.id == link.id.shareId.id
                }
            }
            ?: emptyList()
    }

    private fun List<Link>.filterVolumePhotoListings(photoShareRootFolderId: FolderId?) =
        filterIsInstance<Link.File>()
            .filter { link -> link.parentId == photoShareRootFolderId }
}
