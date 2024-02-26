/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.linkupload.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.data.room.db.extension.dropTable
import me.proton.core.data.room.db.extension.recreateTable
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.linkupload.data.db.dao.LinkUploadDao
import me.proton.core.drive.linkupload.data.db.dao.UploadBlockDao
import me.proton.core.drive.linkupload.data.db.dao.UploadBulkDao
import me.proton.core.drive.linkupload.data.db.entity.UploadBulkUriStringEntity


interface LinkUploadDatabase : Database {
    val linkUploadDao: LinkUploadDao
    val uploadBlockDao: UploadBlockDao
    val uploadBulkDao: UploadBulkDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.dropTable("UploadBulkUriStringEntity")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `UploadBulkUriStringEntity` (
                    `key` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `upload_bulk_id` INTEGER NOT NULL,
                    `uri` TEXT NOT NULL,
                    FOREIGN KEY(`upload_bulk_id`) REFERENCES `UploadBulkEntity`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_UploadBulkUriStringEntity_upload_bulk_id` ON `UploadBulkUriStringEntity` (`upload_bulk_id`)"
                )
            }
        }
        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.NETWORK_TYPE_PROVIDER_TYPE} TEXT NOT NULL DEFAULT 'DEFAULT'
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.DURATION} INTEGER DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.LATITUDE} REAL DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.LONGITUDE} REAL DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.CREATION_TIME} INTEGER DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.MODEL} TEXT DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.ORIENTATION} INTEGER DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.SUBJECT_AREA} TEXT DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.SHOULD_ANNOUNCE_EVENT} INTEGER NOT NULL DEFAULT true
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.CACHE_OPTION} TEXT NOT NULL DEFAULT 'ALL'
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.PRIORITY} INTEGER NOT NULL DEFAULT ${Long.MAX_VALUE}
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `UploadBlockEntity` ADD COLUMN ${Column.TYPE} TEXT NOT NULL DEFAULT 'FILE'
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `UploadBulkEntity` ADD COLUMN ${Column.NETWORK_TYPE_PROVIDER_TYPE} TEXT NOT NULL DEFAULT 'DEFAULT'
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `UploadBulkEntity` ADD COLUMN ${Column.SHOULD_ANNOUNCE_EVENT} INTEGER NOT NULL DEFAULT true
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `UploadBulkEntity` ADD COLUMN ${Column.CACHE_OPTION} TEXT NOT NULL DEFAULT 'ALL'
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `UploadBulkEntity` ADD COLUMN ${Column.PRIORITY} INTEGER NOT NULL DEFAULT ${Long.MAX_VALUE}
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.UPLOAD_CREATION_TIME} INTEGER DEFAULT NULL
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_3 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        ALTER TABLE `LinkUploadEntity` ADD COLUMN ${Column.SHOULD_BROADCAST_ERROR_MESSAGE} INTEGER NOT NULL DEFAULT true
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `UploadBulkEntity` ADD COLUMN ${Column.SHOULD_BROADCAST_ERROR_MESSAGE} INTEGER NOT NULL DEFAULT true
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_4 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addTableColumn("UploadBulkUriStringEntity", Column.NAME, "TEXT")
                database.addTableColumn("UploadBulkUriStringEntity", Column.MIME_TYPE, "TEXT")
                database.addTableColumn("UploadBulkUriStringEntity", Column.SIZE, "INTEGER")
                database.addTableColumn("UploadBulkUriStringEntity", Column.LAST_MODIFIED, "INTEGER")
            }
        }
        val MIGRATION_5 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.recreateTable(
                    table = "UploadBulkUriStringEntity",
                    createTable = {
                        database.execSQL(
                            """
                                CREATE TABLE IF NOT EXISTS `UploadBulkUriStringEntity` (
                                `key` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `upload_bulk_id` INTEGER NOT NULL,
                                `uri` TEXT NOT NULL,
                                `name` TEXT,
                                `mime_type` TEXT,
                                `size` INTEGER,
                                `last_modified` INTEGER,
                                FOREIGN KEY(`upload_bulk_id`) REFERENCES `UploadBulkEntity`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE )
                            """.trimIndent()
                        )
                    },
                    createIndices = {
                        database.execSQL(
                            """
                                CREATE INDEX IF NOT EXISTS `index_UploadBulkUriStringEntity_upload_bulk_id` ON `UploadBulkUriStringEntity` (`upload_bulk_id`)
                            """.trimIndent()
                        )
                        database.execSQL(
                            """
                                CREATE INDEX IF NOT EXISTS `index_UploadBulkUriStringEntity_uri` ON `UploadBulkUriStringEntity` (`uri`)
                            """.trimIndent()
                        )
                    },
                )
            }
        }
    }
}
