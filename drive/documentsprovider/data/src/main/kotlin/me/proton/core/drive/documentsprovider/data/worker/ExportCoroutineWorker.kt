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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.documentsprovider.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.documentsprovider.domain.usecase.GetFileUri
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.domain.usecase.GetFile
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.drive.i18n.R as I18N

abstract class ExportCoroutineWorker constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    private val getFile: GetFile,
    private val getFileUri: GetFileUri,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
    private val announceEvent: AnnounceEvent,
) : CoroutineWorker(appContext, workerParams) {
    protected val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)) { "User id is required" })
    abstract protected val downloadId: String

    override suspend fun doWork(): Result {
        showInfo(applicationContext.getString(I18N.string.common_in_app_notification_download_started))
        val driveLinks = getDriveLinks()
        val succeeded = mutableListOf<DriveLink>()
        driveLinks.forEach { driveLink ->
            getFile(driveLink, false)
                .transform { state ->
                    when (state) {
                        is GetFile.State.Ready -> emit(kotlin.Result.success(Unit))
                        is GetFile.State.Error -> emit(kotlin.Result.failure(RuntimeException()))
                        else -> Unit
                    }
                }
                .first()
                .onFailure {
                    CoreLogger.d(LogTag.DOCUMENTS_PROVIDER, "getFile failed")
                    showError(driveLink)
                    return@forEach
                }
            val uri = getFileUri(userId, driveLink.id)
            exportFileUri(uri, driveLink)
                .onSuccess { succeeded.add(driveLink) }
                .onFailure { error ->
                    error.log(LogTag.DOCUMENTS_PROVIDER, "Cannot export file: $uri")
                    showExportError(error)
                }
        }
        if (succeeded.isNotEmpty()) {
            showInfo(succeeded.message)
            announceEvent(
                userId = userId,
                event = Event.Download(downloadId, succeeded.size, driveLinks.size)
            )
        }
        return Result.success().also { result -> handleResult(result) }
    }

    private val List<DriveLink>.message: String get() = when (size) {
        1 -> applicationContext.getString(
                I18N.string.common_in_app_notification_download_complete,
                first().name,
            )
        else -> applicationContext.resources.getQuantityString(
            I18N.plurals.common_in_app_notification_files_download_complete,
            size,
        )
    }

    protected fun showInfo(infoMessage: String) =
        broadcastMessages(
            userId = userId,
            message = infoMessage,
            type = BroadcastMessage.Type.INFO,
        )

    protected fun showError(driveLink : DriveLink.File? = null) {
        val message = driveLink?.let {
            applicationContext.getString(
                I18N.string.common_in_app_notification_downloading_failed,
                driveLink.name,
            )
        } ?: applicationContext.getString(I18N.string.common_in_app_notification_download_failed)
        broadcastMessages(
            userId = userId,
            message = message,
            type = BroadcastMessage.Type.ERROR,
        )
    }

    private fun showExportError(error:Throwable) {
        broadcastMessages(
            userId = userId,
            message = error.getDefaultMessage(applicationContext, configurationProvider.useExceptionMessage),
            type = BroadcastMessage.Type.ERROR,
        )
    }

    abstract suspend fun getDriveLinks(): List<DriveLink.File>

    abstract fun exportFileUri(uri: Uri, driveLink: DriveLink.File): kotlin.Result<Unit>

    abstract suspend fun handleResult(result: Result)
}
