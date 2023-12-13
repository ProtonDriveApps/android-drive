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

package me.proton.core.drive.backup.data.db

import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.backup.data.db.dao.BackupDuplicateDao
import me.proton.core.drive.backup.data.db.dao.BackupErrorDao
import me.proton.core.drive.backup.data.db.dao.BackupFileDao
import me.proton.core.drive.backup.data.db.dao.BackupFolderDao
import me.proton.core.drive.base.data.db.Column

interface BackupDatabase : Database {
    val backupFileDao: BackupFileDao
    val backupFolderDao: BackupFolderDao
    val backupErrorDao: BackupErrorDao
    val backupDuplicateDao: BackupDuplicateDao

    @DeleteTable(tableName = "PeriodEntity")
    @DeleteColumn(tableName = "BackupFileEntity", columnName = "state")
    class AutoMigration1 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            // Invoked once auto migration is done
        }
    }

    companion object {
        // To prevent excessive memory allocations,
        // the maximum value of a host parameter number is SQLITE_MAX_VARIABLE_NUMBER,
        // which defaults to 999 for SQLite versions prior to 3.32.0 (2020-05-22)
        // or 32766 for SQLite versions after 3.32.0.
        // https://www.sqlite.org/limits.html
        internal const val MAX_VARIABLE_NUMBER = 500

        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `BackupDuplicateEntity` (
                    `user_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `parent_id` TEXT NOT NULL,
                    `hash` TEXT NOT NULL,
                    `content_hash` TEXT,
                    `link_id` TEXT,
                    `state` TEXT,
                    `revision_id` TEXT,
                    `client_uid` TEXT,
                    PRIMARY KEY(`user_id`, `share_id`, `parent_id`, `hash`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`, `parent_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `BackupErrorEntity` (
                    `user_id` TEXT NOT NULL,
                    `error` TEXT NOT NULL,
                    `retryable` INTEGER NOT NULL,
                    PRIMARY KEY(`user_id`, `error`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `BackupFileEntity` (
                    `user_id` TEXT NOT NULL,
                    `folder` TEXT NOT NULL,
                    `uri` TEXT NOT NULL,
                    `mime_type` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `hash` TEXT NOT NULL,
                    `size` INTEGER NOT NULL,
                    `state` TEXT NOT NULL DEFAULT 'IDLE',
                    `creation_time` INTEGER NOT NULL,
                    `priority` INTEGER NOT NULL DEFAULT ${Long.MAX_VALUE},
                    PRIMARY KEY(`user_id`, `uri`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `folder`) REFERENCES `BackupFolderEntity`(`user_id`, `folder`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_BackupFileEntity_user_id_folder` ON `BackupFileEntity` (`user_id`, `folder`)    
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `BackupFolderEntity` (
                    `user_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `parent_id` TEXT NOT NULL,
                    `folder` TEXT NOT NULL,
                    `update_time` INTEGER,
                    PRIMARY KEY(`user_id`, `folder`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    UPDATE `BackupFileEntity` SET `state` = 'IDLE' WHERE `state` = 'POSSIBLE_DUPLICATE'
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    DROP TABLE IF EXISTS `BackupDuplicateEntity`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `BackupDuplicateEntity` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `user_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `parent_id` TEXT NOT NULL, 
                    `hash` TEXT NOT NULL, 
                    `content_hash` TEXT, 
                    `link_id` TEXT, 
                    `state` TEXT, 
                    `revision_id` TEXT, 
                    `client_uid` TEXT, 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`, `parent_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE `BackupFileEntity` ADD COLUMN ${Column.ATTEMPTS} INTEGER DEFAULT 0
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    DROP TABLE IF EXISTS `BackupFileEntity`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    DROP TABLE IF EXISTS `BackupFolderEntity`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `BackupFileEntity` (
                    `user_id` TEXT NOT NULL,
                    `bucket_id` INTEGER NOT NULL,
                    `uri` TEXT NOT NULL,
                    `mime_type` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `hash` TEXT NOT NULL,
                    `size` INTEGER NOT NULL,
                    `state` TEXT NOT NULL DEFAULT 'IDLE',
                    `creation_time` INTEGER NOT NULL,
                    `priority` INTEGER NOT NULL DEFAULT ${Long.MAX_VALUE},
                    `attempts` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`user_id`, `uri`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `bucket_id`) REFERENCES `BackupFolderEntity`(`user_id`, `bucket_id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_BackupFileEntity_user_id_bucket_id` ON `BackupFileEntity` (`user_id`, `bucket_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `BackupFolderEntity` (
                    `user_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `parent_id` TEXT NOT NULL,
                    `bucket_id` INTEGER NOT NULL,
                    `update_time` INTEGER,
                    PRIMARY KEY(`user_id`, `bucket_id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
            }
        }
    }
}
