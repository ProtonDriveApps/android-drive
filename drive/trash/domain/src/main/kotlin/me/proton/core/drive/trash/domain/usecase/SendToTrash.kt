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
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.trash.domain.TrashManager
import javax.inject.Inject

class SendToTrash @Inject constructor(
    private val trashRepository: LinkTrashRepository,
    private val trashManager: TrashManager,
) {

    suspend operator fun invoke(userId: UserId, link: BaseLink) =
        invoke(userId, listOf(link))

    suspend operator fun invoke(userId: UserId, links: List<BaseLink>) {
        links.applyGroupedByShareAndParentFolder { parentId, groupedLinks ->
            invoke(userId, parentId, groupedLinks.map { link -> link.id })
        }
    }

    suspend operator fun invoke(userId: UserId, folderId: FolderId, linkIds: List<LinkId>) {
        trashRepository.insertOrUpdateTrashState(linkIds, TrashState.TRASHING)
        trashManager.trash(userId, folderId, linkIds).onFailure {
            trashRepository.removeTrashState(linkIds)
        }
    }

    private inline fun List<BaseLink>.applyGroupedByShareAndParentFolder(
        block: (parentId: FolderId, nodes: List<BaseLink>) -> Unit,
    ) {
        groupBy { link -> link.id.shareId }
            .forEach { (_, links) ->
                links.groupBy { link -> link.parentId }
                    .forEach { (folderId, links) ->
                        if (folderId != null) {
                            block(folderId, links)
                        }
                    }
            }
    }
}
