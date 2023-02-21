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
package me.proton.core.drive.notification.data.db

import androidx.room.TypeConverter
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.serialize

class NotificationConverters {
    @TypeConverter
    fun notificationEventToString(value: NotificationEvent?) = value?.serialize()

    @TypeConverter
    fun notificationEventFromString(value: String?): NotificationEvent? = value?.deserialize()
}
