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
package me.proton.core.drive.link.selection.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.SELECTION_ID
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.link.selection.data.db.dao.LinkSelectionDao

interface LinkSelectionDatabase : Database {
    val linkSelectionDao: LinkSelectionDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `LinkSelectionEntity` (
                        `${USER_ID}` TEXT NOT NULL,
                        `${SHARE_ID}` TEXT NOT NULL,
                        `${LINK_ID}` TEXT NOT NULL,
                        `${SELECTION_ID}` TEXT NOT NULL,
                        PRIMARY KEY(`${USER_ID}`, `${SHARE_ID}`, `${LINK_ID}`, `${SELECTION_ID}`),
                        FOREIGN KEY(`${USER_ID}`, `${SHARE_ID}`, `${LINK_ID}`)
                            REFERENCES `LinkEntity`(`${USER_ID}`, `${SHARE_ID}`, `${ID}`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_LinkSelectionEntity_user_id_share_id_link_id`
                        ON `LinkSelectionEntity` (`${USER_ID}`, `${SHARE_ID}`, `${LINK_ID}`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_LinkSelectionEntity_selection_id`
                        ON `LinkSelectionEntity` (`${SELECTION_ID}`)
                """.trimIndent())
            }
        }
    }
}
