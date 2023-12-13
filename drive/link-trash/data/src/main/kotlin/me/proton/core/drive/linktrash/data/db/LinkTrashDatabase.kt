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
package me.proton.core.drive.linktrash.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.linktrash.data.db.dao.LinkTrashDao
import me.proton.core.drive.linktrash.data.db.dao.TrashMetadataDao
import me.proton.core.drive.linktrash.data.db.dao.TrashWorkDao

interface LinkTrashDatabase : Database {
    val linkTrashDao: LinkTrashDao
    val trashWorkDao: TrashWorkDao
    val trashMetadataDao: TrashMetadataDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        DROP TABLE `TrashMetadataEntity`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE TABLE `TrashMetadataEntity` (
                            `${Column.USER_ID}` TEXT NOT NULL,
                            `${Column.VOLUME_ID}` TEXT NOT NULL,
                            `${Column.LAST_FETCH_TRASH_TIMESTAMP}` INTEGER,
                            PRIMARY KEY(`${Column.USER_ID}`, `${Column.VOLUME_ID}`),
                            FOREIGN KEY(`${Column.USER_ID}`, `${Column.VOLUME_ID}`)
                                REFERENCES `VolumeEntity`(`${Column.USER_ID}`, `${Column.ID}`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())
            }
        }
    }
}
