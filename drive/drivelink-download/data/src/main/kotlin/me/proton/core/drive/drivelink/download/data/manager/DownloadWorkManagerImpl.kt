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
package me.proton.core.drive.drivelink.download.data.manager

import androidx.lifecycle.asFlow
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.getLong
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.log.LogTag.DOWNLOAD
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.data.extension.isNotDownloading
import me.proton.core.drive.drivelink.download.data.extension.logTag
import me.proton.core.drive.drivelink.download.data.extension.uniqueWorkName
import me.proton.core.drive.drivelink.download.data.worker.DownloadCleanupWorker
import me.proton.core.drive.drivelink.download.data.worker.FileDownloadWorker
import me.proton.core.drive.drivelink.download.data.worker.FolderDownloadWorker
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_SIZE
import me.proton.core.drive.drivelink.download.domain.entity.NetworkType
import me.proton.core.drive.drivelink.download.domain.manager.DownloadWorkManager
import me.proton.core.drive.drivelink.download.domain.usecase.GetDownloadingDriveLinks
import me.proton.core.drive.file.base.domain.usecase.GetRevision
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import androidx.work.NetworkType as WorkNetworkType

class DownloadWorkManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val getRevision: GetRevision,
    private val getDownloadingDriveLinks: GetDownloadingDriveLinks,
) : DownloadWorkManager {

    override suspend fun download(
        driveLink: DriveLink,
        retryable: Boolean,
        networkType: NetworkType,
    ) {
        val works = workManager.getWorkInfosByTagLiveData(driveLink.uniqueWorkName).asFlow().firstOrNull()
        if (works == null || works.all { workInfo -> workInfo.state.isFinished }) {
            val workNetworkType = when(networkType) {
                NetworkType.UNMETERED -> WorkNetworkType.UNMETERED
                NetworkType.METERED -> WorkNetworkType.METERED
                NetworkType.ANY -> WorkNetworkType.CONNECTED
            }
            workManager.enqueue(
                when (driveLink) {
                    is DriveLink.Folder -> FolderDownloadWorker.getWorkRequest(
                        driveLink = driveLink,
                        networkType = workNetworkType,
                    )
                    is DriveLink.File -> FileDownloadWorker.getWorkRequest(
                        driveLink = driveLink,
                        retryable = retryable,
                        networkType = workNetworkType,
                    )
                    is DriveLink.Album -> error("Albums are not supported on download work manager")
                }
            )
        } else {
            CoreLogger.d(driveLink.id.logTag, "Ignore download: link already downloading")
        }
    }

    override fun cancel(driveLink: DriveLink): Unit = with (driveLink) {
        workManager.cancelAllWorkByTag(driveLink.uniqueWorkName)
        workManager.enqueue(DownloadCleanupWorker.getWorkRequest(userId, volumeId, id))
    }

    override suspend fun cancelAll(userId: UserId) {
        getDownloadingDriveLinks(userId).first().forEach { driveLink ->
            cancel(driveLink)
        }
    }

    override fun getProgressFlow(driveLink: DriveLink.File): Flow<Percentage>? {
        return if (driveLink.isNotDownloading) {
            null
        } else {
            workManager.pruneWork()
            flow {
                getRevision(
                    driveLink.id,
                    driveLink.activeRevisionId,
                ).onSuccess { revision ->
                    if (revision.fileSize <= 0) {
                        emit(Percentage(100))
                    } else {
                        emitAll(
                            workManager.getWorkInfosByTagLiveData(driveLink.uniqueWorkName)
                                .asFlow()
                                .transform { workInfos ->
                                    emit(
                                        Percentage(
                                            (workInfos.sumOf { workInfo ->
                                                workInfo.getLong(KEY_SIZE)
                                            }.toFloat() / revision.fileSize)
                                        )
                                    )
                                }
                        )
                    }
                }.onFailure { error ->
                    error.log(DOWNLOAD, "Cannot get active revision for ${driveLink.id.id.logId()}", WARNING)
                }
            }
        }
    }
}
