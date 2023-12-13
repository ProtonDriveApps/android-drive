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
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.drivelink.photo.data.db.dao.PhotoListingRemoteKeyDao

interface DriveLinkPhotoDatabase : Database {
    val photoListingRemoteKeyDao: PhotoListingRemoteKeyDao

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
    }
}
