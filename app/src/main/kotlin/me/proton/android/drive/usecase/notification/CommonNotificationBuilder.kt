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
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.R
import me.proton.core.drive.notification.data.extension.id
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.entity.NotificationId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommonNotificationBuilder @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val deleteIntent: CreateDeletePendingIntent,
) {

    operator fun invoke(
        notificationId: NotificationId,
        notificationEvent: NotificationEvent
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(appContext, notificationId.channel.id)
            .setSmallIcon(R.drawable.ic_app_notification)
            .setDeleteIntent(deleteIntent(notificationId, notificationEvent))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
}
