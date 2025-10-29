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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.documentsprovider.data.worker.ExportToDestinationUriWorker
import me.proton.core.drive.documentsprovider.data.worker.ExportToDownloadWorker
import me.proton.core.drive.documentsprovider.domain.manager.ExportWorkManager
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import javax.inject.Inject

class ExportWorkManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val selectLinks: SelectLinks,
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

    override fun cancel(fileId: FileId) {
        workManager.cancelAllWorkByTag(fileId.uniqueExportWorkName)
    }

    override fun cancelAll(userId: UserId) {
        workManager.cancelAllWorkByTag(userId.uniqueExportWorkName)
    }
}
