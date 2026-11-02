/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.documentsprovider.data.manager

import android.net.Uri
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.await
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.domain.util.coRunCatchingException
import me.proton.core.drive.documentsprovider.data.worker.ExportToDestinationUriWorker
import me.proton.core.drive.documentsprovider.data.worker.ExportToDownloadWorker
import me.proton.core.drive.documentsprovider.domain.manager.ExportWorkManager
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import java.util.concurrent.Executor
import javax.inject.Inject

class ExportWorkManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val selectLinks: SelectLinks,
    private val exportDestinationPermissions: ExportDestinationPermissions,
) : ExportWorkManager {

    private val FileId.uniqueExportWorkName: String get() = id
    private val SelectionId.uniqueExportWorkName: String get() = id
    private val UserId.uniqueExportWorkName: String get() = "export=$id"

    override fun exportTo(fileId: FileId, destinationUri: Uri) {
        workManager.enqueueUniqueWork(
            fileId.uniqueExportWorkName,
            ExistingWorkPolicy.KEEP,
            ExportToDestinationUriWorker.getWorkRequest(
                fileId = fileId,
                destinationUri = destinationUri,
                tags = listOf(
                    fileId.shareId.userId.uniqueExportWorkName,
                    fileId.shareId.userId.id,
                    fileId.uniqueExportWorkName,
                ),
            )
        ).result.addListener(
            // The grant from the document picker is temporary and can be revoked before the download
            // completes, persist it so the worker can still write to the destination.
            { exportDestinationPermissions.take(destinationUri) },
            Executor(Runnable::run),
        )
    }

    override suspend fun exportToDownload(fileIds: List<FileId>): Result<Unit> = coRunCatching {
        require(fileIds.isNotEmpty()) { "List of files for export is empty" }
        val userId = fileIds.first().shareId.userId
        val selectionId = selectLinks(fileIds).getOrThrow()
        workManager.enqueueUniqueWork(
            selectionId.uniqueExportWorkName,
            ExistingWorkPolicy.KEEP,
            ExportToDownloadWorker.getWorkRequest(
                userId = userId,
                selectionId = selectionId,
                tags = listOf(
                    userId.uniqueExportWorkName,
                    userId.id,
                    selectionId.uniqueExportWorkName,
                ),
            )
        )
    }

    override suspend fun cancel(fileId: FileId) {
        coRunCatchingException {
            workManager.cancelAllWorkByTag(fileId.uniqueExportWorkName).await()
        }.onFailure { error ->
            error.log(LogTag.DOCUMENTS_PROVIDER, "Cannot cancel export work")
        }
        exportDestinationPermissions.releaseUnused()
    }

    override suspend fun cancelAll(userId: UserId) {
        coRunCatchingException {
            workManager.cancelAllWorkByTag(userId.uniqueExportWorkName).await()
        }.onFailure { error ->
            error.log(LogTag.DOCUMENTS_PROVIDER, "Cannot cancel export work")
        }
        exportDestinationPermissions.releaseUnused()
    }
}
