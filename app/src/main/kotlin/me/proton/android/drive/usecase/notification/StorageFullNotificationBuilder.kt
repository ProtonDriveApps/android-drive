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

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.extension.deepLinkBaseUrl
import me.proton.android.drive.ui.navigation.Screen
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.notification.domain.entity.NotificationId
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class StorageFullNotificationBuilder @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val commonBuilder: CommonNotificationBuilder,
    private val contentIntent: CreateContentPendingIntent,
) {
    operator fun invoke(notificationId: NotificationId.User, event: Event.StorageFull) =
        commonBuilder(notificationId, event)
            .setContentTitle(appContext.getString(I18N.string.notification_content_title_storage_full))
            .setContentIntent(notificationId)

    private fun NotificationCompat.Builder.setContentIntent(
        notificationId: NotificationId.User
    ): NotificationCompat.Builder = setContentIntent(
        contentIntent(
            notificationId = notificationId,
            uri = "${appContext.deepLinkBaseUrl}/${Screen.Dialogs.StorageFull(notificationId.channel.userId)}".toUri(),
        )
    )
}
