/*
 * Copyright (c) 2023 Proton AG.
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
package me.proton.core.drive.trash.data.test.manager

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.test.manager.StubbedWorkManager
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.trash.domain.TrashManager
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class StubbedTrashManager @Inject constructor(
    private val repository: LinkTrashRepository,
    private val manager: StubbedWorkManager,
) : TrashManager {

    override suspend fun trash(
        userId: UserId,
        folderId: FolderId,
        linkIds: List<LinkId>
    ): DataResult<String> = manager.add("trash", userId, folderId, linkIds)

    override suspend fun restore(
        userId: UserId,
        shareId: ShareId,
        linkIds: List<LinkId>
    ): DataResult<String> = manager.add("restore", userId, shareId, linkIds)

    override suspend fun delete(
        userId: UserId,
        shareId: ShareId,
        linkIds: List<LinkId>
    ): DataResult<String> = manager.add("delete", userId, shareId, linkIds)

    override fun emptyTrash(userId: UserId, shareIds: Set<ShareId>) {
        manager.add("emptyTrash", userId, *shareIds.toTypedArray())
    }

    override fun getEmptyTrashState(
        userId: UserId,
        volumeId: VolumeId,
    ): Flow<TrashManager.EmptyTrashState> {
        return manager.works.flatMapLatest { works ->
            if (works.isNotEmpty()) {
                flowOf(TrashManager.EmptyTrashState.TRASHING)
            } else {
                repository.hasTrashContent(userId, volumeId).map { hasTrashContent ->
                    if (hasTrashContent) {
                        TrashManager.EmptyTrashState.INACTIVE
                    } else {
                        TrashManager.EmptyTrashState.NO_FILES_TO_TRASH
                    }
                }
            }
        }

    }
}