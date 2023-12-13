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

package me.proton.core.drive.trash.data.manager

import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.onSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.trash.data.manager.worker.EmptyTrashSuccessWorker
import me.proton.core.drive.trash.data.manager.worker.EmptyTrashWorker
import me.proton.core.drive.trash.data.manager.worker.PermanentlyDeleteFileNodesWorker
import me.proton.core.drive.trash.data.manager.worker.RestoreFileNodesWorker
import me.proton.core.drive.trash.data.manager.worker.TrashFileNodesWorker
import me.proton.core.drive.trash.domain.TrashManager
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@ExperimentalCoroutinesApi
class TrashManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val linkTrashRepository: LinkTrashRepository,
) : TrashManager {

    override suspend fun trash(userId: UserId, folderId: FolderId, linkIds: List<LinkId>) =
        linkTrashRepository.insertWork(linkIds).onSuccess { workId ->
            workManager.enqueue(
                TrashFileNodesWorker.getWorkRequest(
                    userId = userId,
                    folderId = folderId,
                    workId = workId,
                )
            )
        }

    override suspend fun restore(userId: UserId, shareId: ShareId, linkIds: List<LinkId>) =
        linkTrashRepository.insertWork(linkIds).onSuccess { workId ->
            workManager.enqueue(
                RestoreFileNodesWorker.getWorkRequest(
                    userId = userId,
                    shareId = shareId,
                    workId = workId,
                )
            )
        }

    override suspend fun delete(userId: UserId, shareId: ShareId, linkIds: List<LinkId>) =
        linkTrashRepository.insertWork(linkIds).onSuccess { workId ->
            workManager.enqueue(
                PermanentlyDeleteFileNodesWorker.getWorkRequest(
                    userId = userId,
                    shareId = shareId,
                    workId = workId,
                )
            )
        }

    @Suppress("EnqueueWork")
    override fun emptyTrash(userId: UserId, shareIds: Set<ShareId>) {
        require(shareIds.isNotEmpty()) { "At least one share id is required" }
        workManager.beginUniqueWork(
            userId.uniqueEmptyTrashWorkName,
            ExistingWorkPolicy.KEEP,
            shareIds.map { shareId ->
                EmptyTrashWorker.getWorkRequest(userId, shareId)
            },
        ).then(
            EmptyTrashSuccessWorker.getWorkRequest(userId)
        ).enqueue()
    }

    override fun getEmptyTrashState(userId: UserId, volumeId: VolumeId): Flow<TrashManager.EmptyTrashState> =
        workManager.getWorkInfosForUniqueWorkLiveData(userId.uniqueEmptyTrashWorkName)
            .asFlow()
            .flatMapLatest { workInfos ->
                if (workInfos.firstOrNull { workInfo -> workInfo.state == WorkInfo.State.RUNNING } != null) {
                    flowOf(TrashManager.EmptyTrashState.TRASHING)
                } else {
                    linkTrashRepository.hasTrashContent(userId, volumeId).map { hasTrashContent ->
                        if (hasTrashContent) {
                            TrashManager.EmptyTrashState.INACTIVE
                        } else {
                            TrashManager.EmptyTrashState.NO_FILES_TO_TRASH
                        }
                    }
                }
            }

    private val UserId.uniqueEmptyTrashWorkName: String get() = "EMPTY_TRASH_$id"
}
