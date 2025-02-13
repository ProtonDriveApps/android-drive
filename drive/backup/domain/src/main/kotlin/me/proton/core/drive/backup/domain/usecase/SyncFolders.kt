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

package me.proton.core.drive.backup.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BucketUpdate
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.util.kotlin.filterNotNullValues
import javax.inject.Inject

class SyncFolders @Inject constructor(
    private val getAllFolders: GetAllFolders,
    private val backupManager: BackupManager,
    private val announceEvent: AnnounceEvent,
) {
    suspend operator fun invoke(backupFolder: BackupFolder, uploadPriority: Long) = coRunCatching {
        backupManager.sync(backupFolder, uploadPriority)
        announceEvent(backupFolder.folderId.userId, Event.BackupSync(
            folderId = backupFolder.folderId,
            bucketId = backupFolder.bucketId,
        ))
    }

    suspend operator fun invoke(folderId: FolderId, uploadPriority: Long) = coRunCatching {
        getAllFolders(folderId).getOrThrow().onEach { backupFolder ->
            invoke(backupFolder, uploadPriority)
        }
    }

    suspend operator fun invoke(userId: UserId, uploadPriority: Long) = coRunCatching {
        getAllFolders(userId).getOrThrow().onEach { backupFolder ->
            invoke(backupFolder, uploadPriority)
        }
    }

    suspend operator fun invoke(
        userId: UserId,
        bucketUpdates: List<BucketUpdate>,
        uploadPriority: Long,
        allBuckets: Boolean = false,
    ) = coRunCatching {
        getAllFolders(userId)
            .getOrThrow()
            .associateWith { backupFolder ->
                bucketUpdates.firstOrNull { bucketUpdate -> bucketUpdate.bucketId == backupFolder.bucketId }
            }.let { backupFolderBucketUpdateMap ->
                if (allBuckets) {
                    backupFolderBucketUpdateMap
                } else {
                    backupFolderBucketUpdateMap.filterNotNullValues()
                }
            }.map { (backupFolder, bucketUpdate) ->
                if (backupFolder.updateTime != null && bucketUpdate?.oldestAddedTime != null) {
                    backupFolder.copy(
                        updateTime = minOf(
                            backupFolder.updateTime,
                            bucketUpdate.oldestAddedTime
                        )
                    )
                } else {
                    backupFolder
                }
            }.onEach { backupFolder ->
                invoke(backupFolder, uploadPriority)
            }
    }
}
