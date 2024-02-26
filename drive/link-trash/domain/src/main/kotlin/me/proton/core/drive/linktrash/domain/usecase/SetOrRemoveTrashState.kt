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

package me.proton.core.drive.linktrash.domain.usecase

import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class SetOrRemoveTrashState @Inject constructor(
    private val setTrashState: SetTrashState,
    private val removeTrashState: RemoveTrashState,
) {

    suspend operator fun invoke(volumeId: VolumeId, links: List<Link>) {
        links.groupBy({ link -> link.state }) { link ->
            link.id
        }.forEach { (state, linkIds) ->
            when (state) {
                Link.State.DRAFT -> invoke(volumeId, linkIds, null)
                Link.State.ACTIVE -> invoke(volumeId, linkIds, null)
                Link.State.TRASHED -> invoke(volumeId, linkIds, TrashState.TRASHED)
                Link.State.DELETED -> invoke(volumeId, linkIds, TrashState.DELETED)
                Link.State.RESTORING -> invoke(volumeId, linkIds, null)
            }
        }
    }

    suspend operator fun invoke(volumeId: VolumeId, linkIds: List<LinkId>, state: TrashState?) {
        if (state == null) {
            removeTrashState(linkIds)
        } else {
            setTrashState(volumeId, linkIds, state)
        }
    }
}
