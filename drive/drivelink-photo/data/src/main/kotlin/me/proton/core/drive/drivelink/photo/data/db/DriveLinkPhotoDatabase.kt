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

package me.proton.core.drive.drivelink.photo.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.data.room.db.extension.dropTable
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.drivelink.photo.data.db.dao.AlbumPhotoListingRemoteKeyDao
import me.proton.core.drive.drivelink.photo.data.db.dao.PhotoListingRemoteKeyDao

interface DriveLinkPhotoDatabase : Database {
    val photoListingRemoteKeyDao: PhotoListingRemoteKeyDao
    val albumPhotoListingRemoteKeyDao: AlbumPhotoListingRemoteKeyDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `PhotoListingRemoteKeyEntity` (
                    `key` TEXT NOT NULL,
                    `user_id` TEXT NOT NULL,
                    `volume_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `link_id` TEXT NOT NULL,
                    `capture_time` INTEGER NOT NULL,
                    `previous_key` TEXT,
                    `next_key` TEXT,
                    PRIMARY KEY(`key`, `user_id`, `volume_id`, `share_id`, `link_id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `volume_id`, `share_id`, `link_id`) REFERENCES `PhotoListingEntity`(`user_id`, `volume_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_PhotoListingRemoteKeyEntity_key` ON `PhotoListingRemoteKeyEntity` (`key`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `AlbumPhotoListingRemoteKeyEntity` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `key` TEXT NOT NULL,
                    `user_id` TEXT NOT NULL,
                    `volume_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `album_id` TEXT NOT NULL,
                    `link_id` TEXT NOT NULL,
                    `previous_key` TEXT,
                    `next_key` TEXT,
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `volume_id`, `share_id`, `album_id`, `link_id`) REFERENCES `AlbumPhotoListingEntity`(`user_id`, `volume_id`, `share_id`, `album_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_AlbumPhotoListingRemoteKeyEntity_key_user_id_volume_id_share_id_album_id_link_id` ON `AlbumPhotoListingRemoteKeyEntity` (`key`, `user_id`, `volume_id`, `share_id`, `album_id`, `link_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumPhotoListingRemoteKeyEntity_id` ON `AlbumPhotoListingRemoteKeyEntity` (`id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumPhotoListingRemoteKeyEntity_key` ON `AlbumPhotoListingRemoteKeyEntity` (`key`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.dropTable("PhotoListingRemoteKeyEntity")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `PhotoListingRemoteKeyEntity` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `key` TEXT NOT NULL, 
                    `user_id` TEXT NOT NULL, 
                    `volume_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `link_id` TEXT NOT NULL, 
                    `capture_time` INTEGER NOT NULL, 
                    `previous_key` TEXT, 
                    `next_key` TEXT, 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `volume_id`, `share_id`, `link_id`) REFERENCES `PhotoListingEntity`(`user_id`, `volume_id`, `share_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_PhotoListingRemoteKeyEntity_key` 
                    ON `PhotoListingRemoteKeyEntity` (`key`)
                """.trimIndent())
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_PhotoListingRemoteKeyEntity_key_user_id_volume_id_share_id_link_id` 
                    ON `PhotoListingRemoteKeyEntity` (`key`, `user_id`, `volume_id`, `share_id`, `link_id`)
                """.trimIndent())
            }
        }
    }
}
