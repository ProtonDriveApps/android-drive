/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.volume.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration

interface VolumeDatabase : Database {
    val volumeDao: VolumeDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    DROP TABLE IF EXISTS `VolumeEntity`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `VolumeEntity` (
                    `id` TEXT NOT NULL,
                    `user_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `link_id` TEXT NOT NULL,
                    `create_time` INTEGER NOT NULL,
                    `used_space` INTEGER NOT NULL,
                    `state` INTEGER NOT NULL,
                    `type` INTEGER NOT NULL,

                    PRIMARY KEY(`user_id`, `id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_VolumeEntity_user_id` ON `VolumeEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_VolumeEntity_share_id` ON `VolumeEntity` (`share_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_VolumeEntity_id` ON `VolumeEntity` (`id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_VolumeEntity_type` ON `VolumeEntity` (`type`)
                    """.trimIndent()
                )
            }
        }
    }
}
