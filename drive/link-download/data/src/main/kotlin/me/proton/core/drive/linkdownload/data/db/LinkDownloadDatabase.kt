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
package me.proton.core.drive.linkdownload.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.recreateTable
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.linkdownload.data.db.dao.LinkDownloadDao
import me.proton.core.drive.linkdownload.data.db.dao.LinkDownloadFileSignatureVerificationFailedDao

interface LinkDownloadDatabase : Database {
    val linkDownloadDao: LinkDownloadDao
    val linkDownloadFileSignatureVerificationFailedDao: LinkDownloadFileSignatureVerificationFailedDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_LinkDownloadStateEntity_user_id_state` ON `LinkDownloadStateEntity` (`user_id`, `state`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_LinkDownloadStateEntity_user_id_share_id_link_id_revision_id` ON `LinkDownloadStateEntity` (`user_id`, `share_id`, `link_id`, `revision_id`)
                """.trimIndent())
            }
        }

        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `LinkDownloadFileSignatureVerificationFailedEntity` (
                        `user_id` TEXT NOT NULL,
                        `share_id` TEXT NOT NULL,
                        `link_id` TEXT NOT NULL,

                        PRIMARY KEY(`user_id`, `share_id`, `link_id`),
                        FOREIGN KEY(`user_id`, `share_id`, `link_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Nothing has written a download block since download moved to the SDK.
                database.execSQL("DROP TABLE IF EXISTS `DownloadBlockEntity`")
                // The manifest signature and its address went with the block verification.
                database.recreateTable(
                    table = "LinkDownloadStateEntity",
                    createTable = {
                        database.execSQL(
                            """
                                CREATE TABLE IF NOT EXISTS `LinkDownloadStateEntity` (
                                `user_id` TEXT NOT NULL,
                                `share_id` TEXT NOT NULL,
                                `link_id` TEXT NOT NULL,
                                `revision_id` TEXT NOT NULL,
                                `state` TEXT NOT NULL,

                                PRIMARY KEY(`user_id`, `share_id`, `link_id`, `revision_id`),
                                FOREIGN KEY(`user_id`, `share_id`, `link_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE )
                            """.trimIndent()
                        )
                    },
                    createIndices = {
                        listOf(
                            "user_id" to "`user_id`",
                            "share_id" to "`share_id`",
                            "link_id" to "`link_id`",
                            "revision_id" to "`revision_id`",
                            "state" to "`state`",
                            "user_id_state" to "`user_id`, `state`",
                            "user_id_share_id_link_id" to "`user_id`, `share_id`, `link_id`",
                            "user_id_share_id_link_id_revision_id" to
                                "`user_id`, `share_id`, `link_id`, `revision_id`",
                        ).forEach { (suffix, columns) ->
                            database.execSQL(
                                """
                                    CREATE INDEX IF NOT EXISTS `index_LinkDownloadStateEntity_$suffix`
                                    ON `LinkDownloadStateEntity` ($columns)
                                """.trimIndent()
                            )
                        }
                    },
                    columns = listOf(
                        Column.USER_ID,
                        Column.SHARE_ID,
                        Column.LINK_ID,
                        Column.REVISION_ID,
                        Column.STATE,
                    ),
                )
            }
        }
    }
}
