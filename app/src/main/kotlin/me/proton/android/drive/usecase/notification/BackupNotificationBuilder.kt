/*
 * Copyright (c) 2023-2024 Proton AG.
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

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.extension.deepLinkBaseUrl
import me.proton.android.drive.ui.navigation.Screen
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.i18n.R
import me.proton.core.drive.notification.domain.entity.NotificationId
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N


class BackupNotificationBuilder @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val commonBuilder: CommonNotificationBuilder,
    private val contentIntent: CreateContentPendingIntent,
) {
    operator fun invoke(
        notificationId: NotificationId.User,
        event: Event.Backup,
    ) =
        commonBuilder(notificationId, event).apply {
            setContentIntent(notificationId)
            setContentTitle(
                appContext.getString(R.string.notification_content_title_backup)
                    .format(appContext.getString(R.string.app_name))
            )
            setContentText(event.text)
            setSilent(true)
            setLocalOnly(true)
            if (event.state == Event.Backup.BackupState.IN_PROGRESS) {
                setProgress(
                    event.totalBackupPhotos,
                    event.totalBackupPhotos - event.pendingBackupPhotos,
                    false,
                )
            }
        }

    private fun NotificationCompat.Builder.setContentIntent(
        notificationId: NotificationId.User,
    ): NotificationCompat.Builder = setContentIntent(
        contentIntent(
            notificationId = notificationId,
            uri = "${appContext.deepLinkBaseUrl}/${Screen.Photos(notificationId.channel.userId)}".toUri(),
        )
    )

    private val Event.Backup.text
        get() = when (state) {
            Event.Backup.BackupState.IN_PROGRESS ->
                appContext.resources.getQuantityString(
                    I18N.plurals.notification_content_text_backup_in_progress,
                    pendingBackupPhotos,
                    NumberFormat.getNumberInstance(Locale.getDefault())
                        .format(pendingBackupPhotos),
                )

            Event.Backup.BackupState.COMPLETE ->
                appContext.getString(I18N.string.notification_content_text_backup_complete)

            Event.Backup.BackupState.UNCOMPLETED ->
                appContext.getString(I18N.string.notification_content_text_backup_uncompleted)

            Event.Backup.BackupState.FAILED ->
                appContext.getString(I18N.string.notification_content_text_backup_failed)

            Event.Backup.BackupState.FAILED_CONNECTIVITY ->
                appContext.getString(I18N.string.notification_content_text_backup_failed_connectivity)

            Event.Backup.BackupState.FAILED_WIFI_CONNECTIVITY ->
                appContext.getString(I18N.string.notification_content_text_backup_failed_wifi_connectivity)

            Event.Backup.BackupState.FAILED_PERMISSION ->
                appContext.getString(I18N.string.notification_content_text_backup_failed_permission)

            Event.Backup.BackupState.FAILED_LOCAL_STORAGE ->
                appContext.getString(I18N.string.notification_content_text_backup_failed_local_storage)

            Event.Backup.BackupState.FAILED_DRIVE_STORAGE ->
                appContext.getString(I18N.string.notification_content_text_backup_failed_drive_storage)

            Event.Backup.BackupState.FAILED_PHOTOS_UPLOAD_NOT_ALLOWED ->
                appContext.getString(I18N.string.notification_content_text_backup_failed_photos_upload_not_allowed)

            Event.Backup.BackupState.PAUSED_DISABLED ->
                appContext.getString(I18N.string.notification_content_text_backup_paused_disabled)

            Event.Backup.BackupState.PAUSE_BACKGROUND_RESTRICTIONS ->
                appContext.getString(I18N.string.notification_content_text_backup_paused_background_restrictions)
        }
}
