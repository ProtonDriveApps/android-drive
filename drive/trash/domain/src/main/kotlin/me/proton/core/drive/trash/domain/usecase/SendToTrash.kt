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

package me.proton.core.drive.trash.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.trash.domain.TrashManager
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class SendToTrash @Inject constructor(
    private val trashRepository: LinkTrashRepository,
    private val trashManager: TrashManager,
    private val getShare: GetShare,
) {

    suspend operator fun invoke(userId: UserId, link: BaseLink) =
        invoke(userId, listOf(link))

    suspend operator fun invoke(userId: UserId, links: List<BaseLink>) {
        links.applyGroupedByShareAndParentFolder { parentId, groupedLinks ->
            invoke(userId, parentId, groupedLinks.map { link -> link.id })
        }
    }

    suspend operator fun invoke(userId: UserId, parentId: ParentId, linkIds: List<LinkId>) {
        getShare(parentId.shareId).toResult().getOrNull()?.let { share ->
            invoke(userId, share.volumeId, linkIds)
        }
    }

    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        linkIds: List<LinkId>,
    ) {
        trashRepository.insertOrUpdateTrashState(volumeId, linkIds, TrashState.TRASHING)
        trashManager.trash(userId, volumeId, linkIds).onFailure {
            trashRepository.removeTrashState(linkIds)
        }
    }

    private inline fun List<BaseLink>.applyGroupedByShareAndParentFolder(
        block: (parentId: ParentId, nodes: List<BaseLink>) -> Unit,
    ) {
        groupBy { link -> link.id.shareId }
            .forEach { (_, links) ->
                links
                    .groupBy { link -> link.parentId }
                    .forEach { (parentId, links) ->
                        if (parentId != null) {
                            block(parentId, links)
                        }
                    }
            }
    }
}
