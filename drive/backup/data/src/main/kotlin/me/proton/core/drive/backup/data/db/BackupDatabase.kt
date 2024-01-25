/*
 * Copyright (c) 2023-2024 Proton AG.
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

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.backup.data.db.dao.BackupConfigurationDao
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
    val backupConfigurationDao: BackupConfigurationDao

    companion object {

        val MIGRATION_0 = object : DatabaseMigration {
            @Suppress("LongMethod")
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

        val MIGRATION_4 = object : DatabaseMigration {
            @Suppress("LongMethod")
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `_new_BackupFolderEntity` (
                    `user_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `parent_id` TEXT NOT NULL, 
                    `bucket_id` INTEGER NOT NULL, 
                    `update_time` INTEGER, 
                    PRIMARY KEY(`user_id`, `share_id`, `parent_id`, `bucket_id`), 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`, `parent_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `_new_BackupFileEntity` (
                    `user_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `parent_id` TEXT NOT NULL, 
                    `bucket_id` INTEGER NOT NULL, 
                    `uri` TEXT NOT NULL, 
                    `mime_type` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `hash` TEXT NOT NULL, 
                    `size` INTEGER NOT NULL, 
                    `state` TEXT NOT NULL DEFAULT 'IDLE', 
                    `creation_time` INTEGER NOT NULL, 
                    `priority` INTEGER NOT NULL DEFAULT 9223372036854775807, 
                    `attempts` INTEGER NOT NULL DEFAULT 0, 
                    PRIMARY KEY(`user_id`, `share_id`, `parent_id`, `uri`), 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`, `parent_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`, `parent_id`, `bucket_id`) REFERENCES `BackupFolderEntity`(`user_id`, `share_id`, `parent_id`, `bucket_id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `_new_BackupErrorEntity` (
                    `user_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `parent_id` TEXT NOT NULL, 
                    `error` TEXT NOT NULL, 
                    `retryable` INTEGER NOT NULL, 
                    PRIMARY KEY(`user_id`, `share_id`, `parent_id`, `error`), 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`, `parent_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO `_new_BackupFolderEntity` (
                        `user_id`, 
                        `share_id`, 
                        `parent_id`, 
                        `bucket_id`, 
                        `update_time`
                    ) SELECT 
                        `user_id`, 
                        `share_id`, 
                        `parent_id`, 
                        `bucket_id`, 
                        `update_time`  
                    FROM `BackupFolderEntity`
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO `_new_BackupFileEntity` (
                        `user_id`, 
                        `share_id`, 
                        `parent_id`, 
                        `bucket_id`, 
                        `uri`, 
                        `mime_type`, 
                        `name`, 
                        `hash`, 
                        `size`, 
                        `state`, 
                        `creation_time`, 
                        `priority`, 
                        `attempts`
                    ) SELECT
                        BackupFileEntity.user_id, 
                        BackupFolderEntity.share_id, 
                        BackupFolderEntity.parent_id, 
                        BackupFileEntity.bucket_id, 
                        BackupFileEntity.uri, 
                        BackupFileEntity.mime_type, 
                        BackupFileEntity.name, 
                        BackupFileEntity.hash, 
                        BackupFileEntity.size, 
                        BackupFileEntity.state, 
                        BackupFileEntity.creation_time, 
                        BackupFileEntity.priority, 
                        BackupFileEntity.attempts 
                    FROM `BackupFileEntity` LEFT JOIN BackupFolderEntity 
                    ON BackupFolderEntity.user_id = BackupFileEntity.user_id AND
                        BackupFolderEntity.bucket_id = BackupFileEntity.bucket_id
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO `_new_BackupErrorEntity` (
                        `user_id`, 
                        `share_id`, 
                        `parent_id`, 
                        `error`, 
                        `retryable` 
                    ) SELECT DISTINCT
                        BackupErrorEntity.user_id, 
                        BackupFolderEntity.share_id, 
                        BackupFolderEntity.parent_id, 
                        BackupErrorEntity.error, 
                        BackupErrorEntity.retryable 
                    FROM `BackupErrorEntity` LEFT JOIN BackupFolderEntity 
                    ON BackupFolderEntity.user_id = BackupErrorEntity.user_id
                    """.trimIndent()
                )

                database.execSQL("DROP TABLE `BackupFolderEntity`")
                database.execSQL("DROP TABLE `BackupFileEntity`")
                database.execSQL("DROP TABLE `BackupErrorEntity`")

                database.execSQL("ALTER TABLE `_new_BackupFolderEntity` RENAME TO `BackupFolderEntity`")
                database.execSQL("ALTER TABLE `_new_BackupFileEntity` RENAME TO `BackupFileEntity`")
                database.execSQL("ALTER TABLE `_new_BackupErrorEntity` RENAME TO `BackupErrorEntity`")

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_BackupFileEntity_user_id_share_id_parent_id_bucket_id` ON `BackupFileEntity` (`user_id`, `share_id`, `parent_id`, `bucket_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_BackupFileEntity_user_id_share_id_parent_id_uri` ON `BackupFileEntity` (`user_id`, `share_id`, `parent_id`, `uri`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_BackupFileEntity_user_id_share_id_parent_id_state` ON `BackupFileEntity` (`user_id`, `share_id`, `parent_id`, `state`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_BackupFileEntity_user_id_share_id_parent_id_bucket_id_state` ON `BackupFileEntity` (`user_id`, `share_id`, `parent_id`, `bucket_id`, `state`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_BackupErrorEntity_user_id_share_id_parent_id` ON `BackupErrorEntity` (`user_id`, `share_id`, `parent_id`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addTableColumn("BackupFileEntity", Column.LAST_MODIFIED, "INTEGER")
            }
        }

        val MIGRATION_6 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `BackupConfigurationEntity` (
                    `user_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `parent_id` TEXT NOT NULL, 
                    `network_type` TEXT NOT NULL, 
                    PRIMARY KEY(`user_id`, `share_id`, `parent_id`), 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE ,
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE ,
                    FOREIGN KEY(`user_id`, `share_id`, `parent_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
