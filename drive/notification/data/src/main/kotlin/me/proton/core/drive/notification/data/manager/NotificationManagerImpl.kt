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
package me.proton.core.drive.notification.data.manager

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.notification.data.extension.buildNotificationChannelCompat
import me.proton.core.drive.notification.data.extension.id
import me.proton.core.drive.notification.data.provider.NotificationBuilderProvider
import me.proton.core.drive.notification.domain.entity.Channel
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.manager.NotificationManager
import javax.inject.Inject

class NotificationManagerImpl @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat,
    private val notificationBuilderProvider: NotificationBuilderProvider,
    @ApplicationContext private val appContext: Context,
) : NotificationManager {

    override fun notify(notificationId: NotificationId, events: List<Event>) =
        runIfPermissionGranted {
            notificationManagerCompat.notify(
                notificationId.tag,
                notificationId.id,
                notificationBuilderProvider.get(notificationId, events).build(),
            )
        }

    override fun cancel(notificationId: NotificationId) =
        notificationManagerCompat.cancel(
            notificationId.tag,
            notificationId.id,
        )

    override fun createUserChannels(userId: UserId, username: String, channels: List<Channel.User>) {
        notificationManagerCompat.createNotificationChannelGroup(
            NotificationChannelGroupCompat.Builder(userId.id)
                .setName(username)
                .build()
        )
        channels.forEach { channel ->
            notificationManagerCompat.createNotificationChannel(
                channel.buildNotificationChannelCompat(appContext, userId.id)
            )
        }
    }

    override fun removeUserChannels(userId: UserId, channels: List<Channel.User>) {
        channels.forEach { channel ->
            deleteNotificationChannel(channel.id)
        }
        deleteNotificationChannelGroup(userId)
    }

    override fun createAppChannels(name: String, channels: List<Channel.App>) {
        notificationManagerCompat.createNotificationChannelGroup(
            NotificationChannelGroupCompat.Builder(APP_GROUP_ID)
                .setName(name)
                .build()
        )
        channels.forEach { channel ->
            notificationManagerCompat.createNotificationChannel(
                channel.buildNotificationChannelCompat(appContext, APP_GROUP_ID)
            )
        }
    }

    private fun deleteNotificationChannel(channelId: String) = runCatching {
        notificationManagerCompat.deleteNotificationChannel(channelId)
    }
        .recoverCatching { throwable ->
            if (throwable !is SecurityException) {
                throw throwable
            } else {
                throwable.log(
                    tag = LogTag.NOTIFICATION,
                    message = "Failed removing notification channel $channelId",
                    level = WARNING,
                )
            }
        }
        .getOrThrow()

    private fun deleteNotificationChannelGroup(userId: UserId) = runCatching {
        notificationManagerCompat.deleteNotificationChannelGroup(userId.id)
    }
        .recoverCatching { throwable ->
            if (throwable !is SecurityException) {
                throw throwable
            } else {
                throwable.log(
                    tag = LogTag.NOTIFICATION,
                    message = "Failed removing notification channel group",
                    level = WARNING,
                )
            }
        }
        .getOrThrow()

    private fun runIfPermissionGranted(block: () -> Unit) {
        if (Build.VERSION.SDK_INT < TIRAMISU || (Build.VERSION.SDK_INT >= TIRAMISU && ContextCompat.checkSelfPermission(
                appContext,
                POST_NOTIFICATIONS,
            ) == PERMISSION_GRANTED)
        ) {
            block()
        }
    }

    companion object {
        const val APP_GROUP_ID = "app_group_id"
    }
}
