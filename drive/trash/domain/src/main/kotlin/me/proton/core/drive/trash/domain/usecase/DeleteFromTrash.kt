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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.trash.domain.TrashManager
import javax.inject.Inject

@ExperimentalCoroutinesApi
class DeleteFromTrash @Inject constructor(
    private val linkTrashRepository: LinkTrashRepository,
    private val trashManager: TrashManager,
) {

    suspend operator fun invoke(userId: UserId, linkId: LinkId) =
        invoke(userId, listOf(linkId))

    suspend operator fun invoke(userId: UserId, linkIds: List<LinkId>) {
        linkIds.groupBy { linkId -> linkId.shareId }.forEach { (share, groupedLinks) ->
            invoke(userId, share, groupedLinks)
        }
    }

    suspend operator fun invoke(
        userId: UserId,
        shareId: ShareId,
        linkIds: List<LinkId>,
    ) {
        linkTrashRepository.insertOrUpdateTrashState(linkIds, TrashState.DELETING)
        trashManager.delete(userId, shareId, linkIds).onFailure {
            linkTrashRepository.insertOrUpdateTrashState(linkIds, TrashState.TRASHED)
        }
    }
}
