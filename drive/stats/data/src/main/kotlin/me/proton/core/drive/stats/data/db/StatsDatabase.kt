/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.stats.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.stats.data.db.dao.BackupStatsDao
import me.proton.core.drive.stats.data.db.dao.UploadStatsDao

interface StatsDatabase {

    val uploadStatsDao: UploadStatsDao
    val backupStatsDao: BackupStatsDao

    companion object {
        val MIGRATION_O = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `UploadStatsEntity` (
                    `user_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `link_id` TEXT NOT NULL, 
                    `count` INTEGER NOT NULL, 
                    `size` INTEGER NOT NULL, 
                    `creation_time` INTEGER NOT NULL, 
                    `capture_time` INTEGER, 
                    PRIMARY KEY(`user_id`, `share_id`, `link_id`), 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`, `link_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `InitialBackupEntity` (
                    `user_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `parent_id` TEXT NOT NULL, 
                    `creation_time` INTEGER NOT NULL, 
                    PRIMARY KEY(`user_id`, `share_id`, `parent_id`), 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE NO ACTION , 
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE NO ACTION , 
                    FOREIGN KEY(`user_id`, `share_id`, `parent_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE NO ACTION 
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    DROP TABLE IF EXISTS `InitialBackupEntity`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `InitialBackupEntity` (
                    `user_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `parent_id` TEXT NOT NULL, 
                    `creation_time` INTEGER NOT NULL, 
                    PRIMARY KEY(`user_id`, `share_id`, `parent_id`) 
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
