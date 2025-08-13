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
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import me.proton.android.drive.worker.NotificationActionWorker
import javax.inject.Inject

@AndroidEntryPoint
class NotificationBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var workManager: WorkManager

    override fun onReceive(context: Context?, intent: Intent?) = intent?.action?.let { action ->
        val notificationIdString = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return@let
        workManager
            .enqueue(
                NotificationActionWorker.getWorkRequest(
                    action = action,
                    notificationIdString = notificationIdString
                )
            )
    } ?: Unit

    companion object {
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        private const val BASE_ACTION = "proton.android.intent.action"
        const val ACTION_DELETE = "$BASE_ACTION.DELETE"
        const val ACTION_CANCEL_ALL = "$BASE_ACTION.CANCEL_ALL"
    }
}
