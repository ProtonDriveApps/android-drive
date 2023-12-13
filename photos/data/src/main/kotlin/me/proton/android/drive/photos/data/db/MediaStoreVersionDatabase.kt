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
import me.proton.android.drive.photos.data.db.dao.MediaStoreVersionDao
import me.proton.core.data.room.db.migration.DatabaseMigration

interface MediaStoreVersionDatabase {
    val mediaStoreVersionDao: MediaStoreVersionDao

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
    }
}
