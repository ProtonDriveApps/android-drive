/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.usecase.RemoveNotification
import me.proton.core.drive.upload.domain.usecase.CancelAllUpload
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.deserialize
import javax.inject.Inject

@AndroidEntryPoint
class NotificationBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var removeNotification: RemoveNotification
    @Inject lateinit var cancelAllUpload: CancelAllUpload

    override fun onReceive(context: Context?, intent: Intent?) = intent?.action?.let { action ->
        val notificationIdString = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return
        val notificationId =
            deserialize<NotificationId>(notificationIdString).getOrNull() ?:
            deserialize<NotificationId.User>(notificationIdString)
                .onFailure { error ->
                    error.log(LogTag.BROADCAST_RECEIVER)
                    return@let
                }
                .getOrThrow()
        runBlocking {
            CoreLogger.d(
                tag = LogTag.BROADCAST_RECEIVER,
                message = "Received $action for ${notificationId.tag} ${notificationId.id}"
            )
            if (notificationId is NotificationId.User) {
                when (action) {
                    ACTION_DELETE -> removeNotification(notificationId)
                    ACTION_CANCEL_ALL -> cancelAllUpload(notificationId.channel.userId)
                    else -> CoreLogger.e(
                        tag = LogTag.BROADCAST_RECEIVER,
                        e = RuntimeException("Received unknown action '$action'")
                    )
                }
            }
        }
    } ?: Unit

    private inline fun<reified T: Any> deserialize(value: String): Result<T> = coRunCatching {
        value.deserialize()
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        private const val BASE_ACTION = "proton.android.intent.action"
        const val ACTION_DELETE = "$BASE_ACTION.DELETE"
        const val ACTION_CANCEL_ALL = "$BASE_ACTION.CANCEL_ALL"
    }
}
