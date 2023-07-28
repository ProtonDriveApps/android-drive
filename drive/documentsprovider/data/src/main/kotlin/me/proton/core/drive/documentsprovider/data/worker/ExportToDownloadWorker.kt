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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.documentsprovider.data.extension.exportToMediaStoreDownloads
import me.proton.core.drive.documentsprovider.data.worker.WorkerKeys.KEY_SELECTION_ID
import me.proton.core.drive.documentsprovider.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.documentsprovider.domain.usecase.GetFileUri
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.domain.usecase.GetFile
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDecryptedDriveLinks
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.notification.domain.usecase.AnnounceEvent

@HiltWorker
@RequiresApi(Build.VERSION_CODES.Q)
class ExportToDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    getFile: GetFile,
    getFileUri: GetFileUri,
    configurationProvider: ConfigurationProvider,
    broadcastMessages: BroadcastMessages,
    announceEvent: AnnounceEvent,
    private val getSelectedDecryptedDriveLinks: GetSelectedDecryptedDriveLinks,
    private val deselectLinks: DeselectLinks,
) : ExportCoroutineWorker(appContext, workerParams, getFile, getFileUri, configurationProvider, broadcastMessages, announceEvent) {
    private val selectionId = SelectionId(
        requireNotNull(inputData.getString(KEY_SELECTION_ID)) { "Selection id is required" }
    )
    override val downloadId: String get() = selectionId.id

    override suspend fun getDriveLinks(): List<DriveLink.File> =
        getSelectedDecryptedDriveLinks(selectionId)
            .firstOrNull()
            ?.filterIsInstance<DriveLink.File>()
            ?: emptyList()

    override fun exportFileUri(uri: Uri, driveLink: DriveLink.File): kotlin.Result<Unit> =
        uri.exportToMediaStoreDownloads(applicationContext.contentResolver, driveLink)

    override suspend fun handleResult(result: Result) {
        deselectLinks(selectionId)
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            selectionId: SelectionId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(ExportToDownloadWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putString(KEY_SELECTION_ID, selectionId.id)
                        .build()
                )
                .addTags(tags)
                .build()
    }
}
