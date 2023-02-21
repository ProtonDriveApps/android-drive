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

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.CHANNEL_TYPE
import me.proton.core.drive.base.data.db.Column.NOTIFICATION_EVENT
import me.proton.core.drive.base.data.db.Column.NOTIFICATION_EVENT_ID
import me.proton.core.drive.base.data.db.Column.NOTIFICATION_ID
import me.proton.core.drive.base.data.db.Column.NOTIFICATION_TAG
import me.proton.core.drive.base.data.db.Column.TYPE
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.notification.data.db.dao.NotificationChannelDao
import me.proton.core.drive.notification.data.db.dao.NotificationEventDao

interface NotificationDatabase : Database {
    val channelDao: NotificationChannelDao
    val eventDao: NotificationEventDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `NotificationChannelEntity` (
                        `${USER_ID}` TEXT NOT NULL,
                        `${TYPE}` TEXT NOT NULL,
                        PRIMARY KEY(`${USER_ID}`, `${TYPE}`),
                        FOREIGN KEY(`${USER_ID}`)
                            REFERENCES `AccountEntity`(`${Column.Core.USER_ID}`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_NotificationChannelEntity_user_id`
                        ON `NotificationChannelEntity` (`${USER_ID}`)
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `NotificationEventEntity` (
                        `${USER_ID}` TEXT NOT NULL,
                        `${CHANNEL_TYPE}` TEXT NOT NULL,
                        `${NOTIFICATION_TAG}` TEXT NOT NULL,
                        `${NOTIFICATION_ID}` INTEGER NOT NULL,
                        `${NOTIFICATION_EVENT_ID}` TEXT NOT NULL,
                        `${NOTIFICATION_EVENT}` TEXT NOT NULL,
                        PRIMARY KEY(
                            `${USER_ID}`,
                            `${CHANNEL_TYPE}`,
                            `${NOTIFICATION_TAG}`,
                            `${NOTIFICATION_ID}`,
                            `${NOTIFICATION_EVENT_ID}`
                        ),
                        FOREIGN KEY(`${USER_ID}`, `${CHANNEL_TYPE}`)
                            REFERENCES `NotificationChannelEntity`(`${USER_ID}`, `${TYPE}`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_NotificationEventEntity_user_id_channel_type_notification_tag_notification_id`
                        ON `NotificationEventEntity` (
                            `${USER_ID}`, `${CHANNEL_TYPE}`, `${NOTIFICATION_TAG}`, `${NOTIFICATION_ID}`
                        )
                """.trimIndent())
            }
        }
    }
}
