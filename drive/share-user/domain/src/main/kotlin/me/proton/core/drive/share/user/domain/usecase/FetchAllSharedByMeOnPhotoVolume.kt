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

package me.proton.core.drive.share.user.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.share.user.domain.entity.ShareTargetType
import me.proton.core.drive.share.user.domain.entity.SharedLinkId
import me.proton.core.drive.share.user.domain.entity.SharedListing
import me.proton.core.drive.share.user.domain.extension.toPairSharedListingSaveAction
import me.proton.core.drive.share.user.domain.repository.SharedRepository
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.usecase.GetActiveVolumes
import javax.inject.Inject

class FetchAllSharedByMeOnPhotoVolume @Inject constructor(
    private val getActiveVolumes: GetActiveVolumes,
    private val sharedRepository: SharedRepository,
    private val linkRepository: LinkRepository,
) {

    suspend operator fun invoke(userId: UserId): Result<Pair<SharedListing, SaveAction>> = coRunCatching {
        val result: MutableList<Pair<SharedListing, SaveAction>> = mutableListOf()
        getActiveVolumes(userId)
            .toResult()
            .getOrThrow()
            .firstOrNull { volume -> volume.type == Volume.Type.PHOTO }
            ?.let { photoVolume ->
                var anchorId: String? = null
                do {
                    val (sharedListing, _) = sharedRepository.fetchSharedByMeListing(
                        userId = userId,
                        volumeId = photoVolume.id,
                        anchorId = anchorId,
                    )
                    anchorId = sharedListing.anchorId
                    val nonAlbumSharedListings = sharedListing.filterAlbums()
                    result.add(nonAlbumSharedListings to sharedRepository.getSaveAction(nonAlbumSharedListings))
                } while (anchorId != null)
            }
        result.toPairSharedListingSaveAction()
    }

    private suspend fun SharedListing.filterAlbums(): SharedListing =
        copy(
            linkIds = linkIds
                .filter { sharedLinkId -> sharedLinkId.type != ShareTargetType.Album }
                .filter { sharedLinkId -> sharedLinkId.isNotAlbum() }
        )

    private suspend fun SharedLinkId.isNotAlbum(): Boolean {
        val link = linkRepository.getLinkFlow(linkId).toResult().getOrNull()
        if (link != null) return link.id !is AlbumId
        val children = linkRepository.fetchLinks(linkId.shareId, setOf(linkId.id))
            .getOrNull()
            ?.second
        if (!children.isNullOrEmpty()) {
            return children.first().id !is AlbumId
        }
        return false
    }
}
