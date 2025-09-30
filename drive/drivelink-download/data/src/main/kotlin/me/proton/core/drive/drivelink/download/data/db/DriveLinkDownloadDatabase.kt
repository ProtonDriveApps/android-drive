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
package me.proton.core.drive.drivelink.download.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column.NETWORK_TYPE
import me.proton.core.drive.drivelink.download.data.db.dao.DriveLinkDownloadDao
import me.proton.core.drive.drivelink.download.data.db.dao.FileDownloadDao
import me.proton.core.drive.drivelink.download.data.db.dao.ParentLinkDownloadDao

interface DriveLinkDownloadDatabase : Database {
    val driveLinkDownloadDao: DriveLinkDownloadDao
    val fileDownloadDao: FileDownloadDao
    val parentLinkDownloadDao: ParentLinkDownloadDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `FileDownloadEntity` (
                    `id` INTEGER NOT NULL,
                    `user_id` TEXT NOT NULL,
                    `volume_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `link_id` TEXT NOT NULL,
                    `revision_id` TEXT NOT NULL,
                    `priority` INTEGER NOT NULL,
                    `retryable` INTEGER NOT NULL,
                    `state` TEXT NOT NULL,
                    `parent_id` TEXT,
                    `number_of_retries` INTEGER NOT NULL,
                    `run_at` INTEGER,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`, `link_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_FileDownloadEntity_user_id_volume_id_share_id_link_id_revision_id` ON `FileDownloadEntity` (`user_id`, `volume_id`, `share_id`, `link_id`, `revision_id`)
                """.trimIndent()
                )
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_FileDownloadEntity_user_id` ON `FileDownloadEntity` (`user_id`)
                """.trimIndent()
                )
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_FileDownloadEntity_priority` ON `FileDownloadEntity` (`priority`)
                """.trimIndent()
                )
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_FileDownloadEntity_user_id_state` ON `FileDownloadEntity` (`user_id`, `state`)
                """.trimIndent()
                )
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ParentLinkDownloadEntity` (
                    `id` INTEGER NOT NULL,
                    `user_id` TEXT NOT NULL,
                    `volume_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `link_id` TEXT NOT NULL,
                    `type` INTEGER NOT NULL,
                    `priority` INTEGER NOT NULL,
                    `retryable` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`, `link_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_ParentLinkDownloadEntity_user_id_volume_id_share_id_link_id` ON `ParentLinkDownloadEntity` (`user_id`, `volume_id`, `share_id`, `link_id`)
                """.trimIndent()
                )
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_ParentLinkDownloadEntity_user_id` ON `ParentLinkDownloadEntity` (`user_id`)
                """.trimIndent()
                )
            }
        }

        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addTableColumn(
                    table = "FileDownloadEntity",
                    column = NETWORK_TYPE,
                    type = "TEXT NOT NULL DEFAULT 'ANY'",
                )
            }
        }
    }
}
