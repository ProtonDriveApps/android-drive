/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.android.drive.photos.data.db.dao.AddToAlbumDao
import me.proton.android.drive.photos.data.db.dao.MediaStoreVersionDao
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration

interface PhotosDatabase : Database {
    val mediaStoreVersionDao: MediaStoreVersionDao
    val addToAlbumDao: AddToAlbumDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `MediaStoreVersionEntity` (
                    `user_id` TEXT NOT NULL,
                    `media_store_volume_name` TEXT NOT NULL,
                    `version` TEXT,
                    PRIMARY KEY(`user_id`, `media_store_volume_name`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `AddToAlbumEntity` (
                    `user_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `link_id` TEXT NOT NULL,
                    `album_id` TEXT,
                    `capture_time` INTEGER NOT NULL,
                    `hash` TEXT,
                    `content_hash` TEXT,
                    PRIMARY KEY(`user_id`, `share_id`, `link_id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    FOREIGN KEY(`user_id`, `share_id`, `link_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    FOREIGN KEY(`user_id`, `share_id`, `album_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AddToAlbumEntity_user_id` ON `AddToAlbumEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AddToAlbumEntity_album_id` ON `AddToAlbumEntity` (`album_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_AddToAlbumEntity_user_id_share_id_link_id_album_id` ON `AddToAlbumEntity` (`user_id`, `share_id`, `link_id`, `album_id`)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    DROP TABLE IF EXISTS `AddToAlbumEntity`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `AddToAlbumEntity` (
                    `user_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `link_id` TEXT NOT NULL,
                    `album_share_id` TEXT,
                    `album_id` TEXT,
                    `capture_time` INTEGER NOT NULL,
                    `hash` TEXT,
                    `content_hash` TEXT,
                    PRIMARY KEY(`user_id`, `share_id`, `link_id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    FOREIGN KEY(`user_id`, `share_id`, `link_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    FOREIGN KEY(`user_id`, `album_share_id`, `album_id`) REFERENCES `LinkEntity`(`user_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AddToAlbumEntity_user_id` ON `AddToAlbumEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AddToAlbumEntity_album_id` ON `AddToAlbumEntity` (`album_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AddToAlbumEntity_user_id_album_id` ON `AddToAlbumEntity` (`user_id`, `album_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AddToAlbumEntity_user_id_album_share_id_album_id` ON `AddToAlbumEntity` (`user_id`, `album_share_id`, `album_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_AddToAlbumEntity_user_id_share_id_link_id_album_share_id_album_id` ON `AddToAlbumEntity` (`user_id`, `share_id`, `link_id`, `album_share_id`, `album_id`)
                    """.trimIndent()
                )
            }
        }
    }
}
