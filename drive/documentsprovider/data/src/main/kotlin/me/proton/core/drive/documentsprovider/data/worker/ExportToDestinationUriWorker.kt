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
package me.proton.core.drive.documentsprovider.data.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.extension.firstSuccessOrError
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.documentsprovider.data.extension.exportTo
import me.proton.core.drive.documentsprovider.data.worker.WorkerKeys.KEY_DESTINATION_URI
import me.proton.core.drive.documentsprovider.data.worker.WorkerKeys.KEY_FILE_ID
import me.proton.core.drive.documentsprovider.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.documentsprovider.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.documentsprovider.domain.usecase.GetFileUri
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.domain.usecase.GetFile
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.notification.domain.usecase.AnnounceEvent
import me.proton.core.drive.share.domain.entity.ShareId

@HiltWorker
class ExportToDestinationUriWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    getFile: GetFile,
    getFileUri: GetFileUri,
    broadcastMessages: BroadcastMessages,
    private val getDriveLink: GetDecryptedDriveLink,
    announceEvent: AnnounceEvent,
) : ExportCoroutineWorker(appContext, workerParams, getFile, getFileUri, broadcastMessages, announceEvent) {
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)) { "Share id is required" })
    private val fileId = FileId(shareId, requireNotNull(inputData.getString(KEY_FILE_ID)) { "File id is required" })
    private val destinationUri = requireNotNull(
        Uri.parse(requireNotNull(inputData.getString(KEY_DESTINATION_URI)) { "Destination Uri is required" })
    ) { "Uri parsing failed for ${inputData.getString(KEY_DESTINATION_URI)}" }
    override val downloadId: String get() = "${fileId.shareId.id}.${fileId.id}"

    override suspend fun getDriveLinks(): List<DriveLink.File> =
        listOfNotNull(
            getDriveLink(fileId, failOnDecryptionError = false)
                .firstSuccessOrError()
                .onFailure { error ->
                    if (!error.isRetryable) {
                        error.log(LogTag.DOCUMENTS_PROVIDER)
                        showError()
                    }
                }
                .toResult()
                .getOrNull()
        )

    override fun exportFileUri(uri: Uri, driveLink: DriveLink.File): kotlin.Result<Unit> =
        uri.exportTo(applicationContext.contentResolver, destinationUri)

    override suspend fun handleResult(result: Result) = Unit

    companion object {
        fun getWorkRequest(
            fileId: FileId,
            destinationUri: Uri,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(ExportToDestinationUriWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, fileId.shareId.userId.id)
                        .putString(KEY_SHARE_ID, fileId.shareId.id)
                        .putString(KEY_FILE_ID, fileId.id)
                        .putString(KEY_DESTINATION_URI, destinationUri.toString())
                        .build()
                )
                .addTags(tags)
                .build()
    }
}
