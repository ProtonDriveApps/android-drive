/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.device.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.device.data.db.dao.DeviceDao

interface DeviceDatabase : Database {
    val deviceDao: DeviceDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `DeviceEntity` (
                    `user_id` TEXT NOT NULL,
                    `volume_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `link_id` TEXT NOT NULL,
                    `id` TEXT NOT NULL,
                    `type` INTEGER NOT NULL,
                    `sync_state` INTEGER NOT NULL,
                    `creation_time` INTEGER NOT NULL,
                    `last_modified` INTEGER,
                    `last_synced` INTEGER,
                    `name` TEXT NOT NULL,
                    PRIMARY KEY(`user_id`, `id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`, `link_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DeviceEntity_user_id` ON `DeviceEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DeviceEntity_volume_id` ON `DeviceEntity` (`volume_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DeviceEntity_share_id` ON `DeviceEntity` (`share_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DeviceEntity_link_id` ON `DeviceEntity` (`link_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DeviceEntity_id` ON `DeviceEntity` (`id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DeviceEntity_user_id_volume_id_share_id_link_id` ON
                    `DeviceEntity` (`user_id`, `volume_id`, `share_id`, `link_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DeviceEntity_user_id_share_id` ON
                    `DeviceEntity` (`user_id`, `share_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DeviceEntity_user_id_share_id_link_id` ON
                    `DeviceEntity` (`user_id`, `share_id`, `link_id`)
                    """.trimIndent()
                )
            }
        }
    }
}
