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

import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.EVENTS
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.drivelink.offline.domain.usecase.UpdateOfflineContent
import me.proton.core.drive.eventmanager.entity.LinkEventVO
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.ids
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.link.domain.usecase.InsertOrUpdateLinks
import me.proton.core.drive.linktrash.domain.usecase.SetOrRemoveTrashState
import me.proton.core.drive.photo.domain.usecase.InsertOrDeleteAlbumListings
import me.proton.core.drive.photo.domain.usecase.InsertOrDeletePhotoListings
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class HandleCreateOrUpdateLinksEvent @Inject constructor(
    private val insertOrUpdateLinks: InsertOrUpdateLinks,
    private val setOrRemoveTrashState: SetOrRemoveTrashState,
    private val updateOfflineContent: UpdateOfflineContent,
    private val getLink: GetLink,
    private val insertOrDeletePhotoListings: InsertOrDeletePhotoListings,
    private val insertOrDeleteAlbumListings: InsertOrDeleteAlbumListings,
) {

    suspend operator fun invoke(vos: List<LinkEventVO>) {
        if (vos.isNotEmpty()) {
            vos
                .groupBy({ vo -> vo.volumeId }) { vo -> vo.link }
                .forEach { (volumeId, links) ->
                    val modifiedStateOrParentLinks = links.modifiedStateOrParentLinks()
                    insertOrUpdateLinks(links)
                    setOrRemoveTrashState(volumeId, links)
                    updateOfflineContent(modifiedStateOrParentLinks.ids)
                    insertOrDeletePhotoListings(volumeId, links.filterIsInstance<Link.File>())
                    insertOrDeleteAlbumListings(volumeId, links.filterIsInstance<Link.Album>())
                }
        }
    }

    private suspend fun List<Link>.modifiedStateOrParentLinks() = filter { link ->
        getLink(link.id, flowOf { false }).toResult().fold(
            onSuccess = { staleLink ->
                (staleLink.state != link.state) or (staleLink.parentId != link.parentId)
            },
            onFailure = { error ->
                CoreLogger.w(EVENTS, error, "Cannot get link ${link.id.id.logId()}")
                true
            }
        )
    }
}
