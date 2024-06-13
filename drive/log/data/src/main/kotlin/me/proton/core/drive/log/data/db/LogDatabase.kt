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

package me.proton.core.drive.log.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.log.data.db.dao.LogDao
import me.proton.core.drive.log.data.db.dao.LogOriginDao
import me.proton.core.drive.log.data.db.dao.LogLevelDao

interface LogDatabase : Database {
    val logDao: LogDao
    val logLevelDao: LogLevelDao
    val logOriginDao: LogOriginDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `LogEntity` (
                    `id` INTEGER NOT NULL,
                    `user_id` TEXT NOT NULL,
                    `creation_time` INTEGER NOT NULL,
                    `value` TEXT NOT NULL,
                    `content` TEXT,
                    `type` TEXT NOT NULL,
                    `origin` TEXT NOT NULL,

                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_LogEntity_user_id` ON `LogEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `LogLevelEntity` (
                    `user_id` TEXT NOT NULL,
                    `level` TEXT NOT NULL,

                    PRIMARY KEY(`user_id`, `level`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_LogLevelEntity_user_id` ON `LogLevelEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `LogOriginEntity` (
                    `user_id` TEXT NOT NULL,
                    `origin` TEXT NOT NULL,

                    PRIMARY KEY(`user_id`, `origin`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_LogOriginEntity_user_id` ON `LogOriginEntity` (`user_id`)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    DROP TABLE IF EXISTS `LogEntity`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `LogEntity` (
                    `id` INTEGER NOT NULL,
                    `user_id` TEXT NOT NULL,
                    `creation_time` INTEGER NOT NULL,
                    `value` TEXT NOT NULL,
                    `content` TEXT DEFAULT NULL,
                    `level` TEXT NOT NULL,
                    `origin` TEXT NOT NULL,

                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_LogEntity_user_id` ON `LogEntity` (`user_id`)
                    """.trimIndent()
                )
            }
        }
    }
}
