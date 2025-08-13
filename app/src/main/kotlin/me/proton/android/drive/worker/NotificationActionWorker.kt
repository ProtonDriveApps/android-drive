/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.android.drive.extension.log
import me.proton.android.drive.receiver.NotificationBroadcastReceiver.Companion.ACTION_CANCEL_ALL
import me.proton.android.drive.receiver.NotificationBroadcastReceiver.Companion.ACTION_DELETE
import me.proton.core.drive.base.domain.extension.resultValueOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.usecase.RemoveNotification
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.upload.domain.usecase.CancelAllUpload
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.deserialize

@HiltWorker
class NotificationActionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val removeNotification: RemoveNotification,
    private val getMainShare: GetMainShare,
    private val cancelAllUpload: CancelAllUpload,
) : CoroutineWorker(appContext, params) {
    private val action = inputData.getString(KEY_ACTION)
    private val notificationIdString = inputData.getString(KEY_NOTIFICATION_ID)

    override suspend fun doWork(): Result {
        if (action == null || notificationIdString == null) {
            CoreLogger.d(
                tag = LogTag.NOTIFICATION,
                message = "NotificationActionWorker invalid input data, action=$action, notificationId=$notificationIdString",
            )
            return Result.failure()
        }
        val notificationId =
            deserialize<NotificationId>(notificationIdString).getOrNull() ?:
            deserialize<NotificationId.User>(notificationIdString)
                .onFailure { error ->
                    error.log(LogTag.NOTIFICATION, "NotificationActionWorker deserializing notificationId failed")
                    return Result.failure()
                }
                .getOrThrow()
        CoreLogger.d(
            tag = LogTag.NOTIFICATION,
            message = "NotificationActionWorker $action for ${notificationId.tag} ${notificationId.id}",
        )
        if (notificationId is NotificationId.User) {
            when (action) {
                ACTION_DELETE -> removeNotification(notificationId)
                ACTION_CANCEL_ALL -> getMainShare(notificationId.channel.userId).resultValueOrNull()?.id?.let { shareId ->
                    cancelAllUpload(
                        notificationId.channel.userId,
                        shareId,
                    )
                }
                else -> RuntimeException("Unknown action '$action'").log(LogTag.NOTIFICATION)
            }
        }
        return Result.success()
    }

    private inline fun<reified T: Any> deserialize(value: String): kotlin.Result<T> = coRunCatching {
        value.deserialize()
    }

    companion object {
        const val KEY_ACTION = "key.action"
        const val KEY_NOTIFICATION_ID = "key.notificationId"

        fun getWorkRequest(
            action: String,
            notificationIdString: String,
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(NotificationActionWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, action)
                        .putString(KEY_NOTIFICATION_ID, notificationIdString)
                        .build()
                )
                .build()
    }
}
