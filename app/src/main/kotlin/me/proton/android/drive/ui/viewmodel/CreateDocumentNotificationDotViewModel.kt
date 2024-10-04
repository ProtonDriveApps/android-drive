/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.datastore.asFlow
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toTimestampS
import java.util.Calendar

class CreateDocumentNotificationDotViewModel(
    private val userId: UserId,
    private val getUserDataStore: GetUserDataStore,
) : NotificationDotViewModel {

    override val notificationDotRequested: Flow<Boolean>
        get() = flow {
            emitAll(
                GetUserDataStore.Keys.createDocumentActionInvoked
                    .asFlow(getUserDataStore(userId), false)
                    .map { actionInvoked -> !actionInvoked && !isOverdue }
            )
        }

    private val isOverdue: Boolean get() = notificationDueTime < TimestampS()

    private val notificationDueTime: TimestampS = TimestampMs(NOTIFICATION_DUE_CALENDAR.timeInMillis).toTimestampS()

    companion object {
        val NOTIFICATION_DUE_CALENDAR: Calendar = Calendar.getInstance()
            .apply {
                set(2024, Calendar.OCTOBER, 31, 0, 0, 0)
            }
    }
}
