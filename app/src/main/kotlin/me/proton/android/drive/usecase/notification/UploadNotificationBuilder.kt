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

package me.proton.android.drive.usecase.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.extension.deepLinkBaseUrl
import me.proton.android.drive.receiver.NotificationBroadcastReceiver
import me.proton.android.drive.receiver.NotificationBroadcastReceiver.Companion.ACTION_CANCEL_ALL
import me.proton.android.drive.receiver.NotificationBroadcastReceiver.Companion.EXTRA_NOTIFICATION_ID
import me.proton.android.drive.ui.navigation.Screen
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.util.kotlin.serialize
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class UploadNotificationBuilder @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val commonBuilder: CommonNotificationBuilder,
    private val contentIntent: CreateContentPendingIntent,
) {
    operator fun invoke(notificationId: NotificationId.User, notificationEvents: List<NotificationEvent.Upload>) =
        with (notificationEvents) {
            commonBuilder(notificationId, first())
                .setContentTitle(title)
                .setContentIntent(notificationId)
                .setContentText(text)
                .setProgress(maxProgress, progress, false)
                .addAction(notificationId, this)
        }

    private fun List<NotificationEvent.Upload>.count(vararg uploadStates: NotificationEvent.Upload.UploadState): Int =
        count { upload -> upload.state in uploadStates }

    private val List<NotificationEvent.Upload>.uploadingCount get() = count(
        NotificationEvent.Upload.UploadState.NEW_UPLOAD,
        NotificationEvent.Upload.UploadState.UPLOADING,
    )

    private val List<NotificationEvent.Upload>.title: String get() =
        if (uploadingCount > 0) {
            appContext.getString(
                I18N.string.notification_content_title_upload_uploading,
                appContext.getString(I18N.string.app_name)
            )
        } else {
            appContext.getString(I18N.string.notification_content_title_upload_complete)
        }

    private fun NotificationCompat.Builder.setContentIntent(
        notificationId: NotificationId.User
    ): NotificationCompat.Builder = setContentIntent(
        contentIntent(
            notificationId = notificationId,
            uri = "${appContext.deepLinkBaseUrl}/${Screen.Files(notificationId.channel.userId)}".toUri(),
        )
    )

    private val List<NotificationEvent.Upload>.text: String get() {
        val uploading = uploadingCount
        return if (uploading > 0) {
            appContext.resources.getQuantityString(
                I18N.plurals.notification_content_text_upload_uploading,
                uploading,
                uploading,
            )
        } else {
            val completedString = mutableListOf<String>()
            val succeeded = count(NotificationEvent.Upload.UploadState.UPLOAD_COMPLETE)
            if (succeeded > 0) {
                completedString.add(
                    appContext.quantityString(
                        I18N.plurals.notification_content_text_upload_uploaded,
                        succeeded,
                    )
                )
            }
            val cancelled = count(NotificationEvent.Upload.UploadState.UPLOAD_CANCELLED)
            if (cancelled > 0) {
                completedString.add(
                    appContext.quantityString(
                        I18N.plurals.notification_content_text_upload_cancelled,
                        cancelled,
                    )
                )
            }
            val failed = count(NotificationEvent.Upload.UploadState.UPLOAD_FAILED)
            if (failed > 0) {
                completedString.add(
                    appContext.quantityString(
                        I18N.plurals.notification_content_text_upload_failed,
                        failed,
                    )
                )
            }
            completedString.joinToString()
        }
    }

    private val List<NotificationEvent.Upload>.maxProgress: Int get() =
        uploadingCount.takeIf { it > 0 }?.let { uploading ->
            (uploading + count(NotificationEvent.Upload.UploadState.UPLOAD_COMPLETE)) * 100
        } ?: 0

    private val List<NotificationEvent.Upload>.progress: Int get() =
        uploadingCount.takeIf { it > 0 }?.let {
            ((sumOf { upload -> upload.percentage.value.toDouble() } * 100).toInt())
        } ?: 0

    private fun NotificationCompat.Builder.addAction(
        notificationId: NotificationId,
        uploadNotificationEvents: List<NotificationEvent.Upload>
    ): NotificationCompat.Builder = apply {
        if (uploadNotificationEvents.uploadingCount > 0) {
            addAction(
                NotificationCompat.Action.Builder(
                    0,
                    appContext.getString(
                        if (uploadNotificationEvents.uploadingCount > 1) {
                            I18N.string.common_cancel_all_action
                        } else {
                            I18N.string.common_cancel_action
                        }
                    ),
                    cancelAllIntent(notificationId),
                ).build()
            )
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun cancelAllIntent(
        notificationId: NotificationId,
        requestCode: Int = 9999,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            requestCode,
            Intent(appContext, NotificationBroadcastReceiver::class.java).apply {
                action = ACTION_CANCEL_ALL
                putExtra(EXTRA_NOTIFICATION_ID, notificationId.serialize())
            },
            PendingIntent.FLAG_IMMUTABLE
        )
}
