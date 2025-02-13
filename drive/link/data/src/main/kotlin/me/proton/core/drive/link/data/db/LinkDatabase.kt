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
package me.proton.core.drive.link.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column.CAPTURE_TIME
import me.proton.core.drive.base.data.db.Column.CONTENT_HASH
import me.proton.core.drive.base.data.db.Column.MAIN_PHOTO_LINK_ID
import me.proton.core.drive.base.data.db.Column.SHARE_URL_ID
import me.proton.core.drive.base.data.db.Column.SHARE_URL_SHARE_ID
import me.proton.core.drive.base.data.db.Column.THUMBNAIL_ID_DEFAULT
import me.proton.core.drive.base.data.db.Column.THUMBNAIL_ID_PHOTO

interface LinkDatabase : Database {
    val linkDao: LinkDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        ALTER TABLE `LinkEntity` ADD COLUMN $SHARE_URL_SHARE_ID TEXT DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkEntity` ADD COLUMN $SHARE_URL_ID TEXT DEFAULT NULL
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        ALTER TABLE `LinkFilePropertiesEntity` ADD COLUMN $CAPTURE_TIME INTEGER DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkFilePropertiesEntity` ADD COLUMN $CONTENT_HASH TEXT DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkFilePropertiesEntity` ADD COLUMN $MAIN_PHOTO_LINK_ID TEXT DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkFilePropertiesEntity` ADD COLUMN $THUMBNAIL_ID_DEFAULT TEXT DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        ALTER TABLE `LinkFilePropertiesEntity` ADD COLUMN $THUMBNAIL_ID_PHOTO TEXT DEFAULT NULL
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `LinkAlbumPropertiesEntity` (
                        `album_user_id` TEXT NOT NULL,
                        `album_share_id` TEXT NOT NULL,
                        `album_link_id` TEXT NOT NULL,
                        `album_node_hash_key` TEXT NOT NULL,
                        `locked` INTEGER NOT NULL,
                        `last_activity_time` INTEGER NOT NULL,
                        `count` INTEGER NOT NULL,
                        `cover_link_id` TEXT,

                        PRIMARY KEY(`album_user_id`, `album_share_id`, `album_link_id`),
                        FOREIGN KEY(`album_user_id`, `album_share_id`, `album_link_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_LinkAlbumPropertiesEntity_album_share_id` ON `LinkAlbumPropertiesEntity` (`album_share_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_LinkAlbumPropertiesEntity_album_link_id` ON `LinkAlbumPropertiesEntity` (`album_link_id`)
                    """.trimIndent()
                )
            }
        }
    }
}
