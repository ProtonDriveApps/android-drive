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

package me.proton.core.drive.feature.flag.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.feature.flag.data.db.dao.DriveFeatureFlagRefreshDao

interface DriveFeatureFlagDatabase : Database {
    val driveFeatureFlagRefreshDao: DriveFeatureFlagRefreshDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `DriveFeatureFlagEntity` (
                    `user_id` TEXT NOT NULL,
                    `id` TEXT NOT NULL,
                    `value` INTEGER NOT NULL,
                    PRIMARY KEY(`user_id`, `id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DriveFeatureFlagEntity_user_id` ON `DriveFeatureFlagEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DriveFeatureFlagEntity_id` ON `DriveFeatureFlagEntity` (`id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `DriveFeatureFlagRefreshEntity` (
                    `user_id` TEXT NOT NULL,
                    `id` TEXT NOT NULL,
                    `last_fetch_timestamp` INTEGER,
                    PRIMARY KEY(`user_id`, `id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_DriveFeatureFlagRefreshEntity_user_id` ON `DriveFeatureFlagRefreshEntity` (`user_id`)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        DROP TABLE IF EXISTS `DriveFeatureFlagEntity`
                    """.trimIndent()
                )
            }
        }
    }
}
