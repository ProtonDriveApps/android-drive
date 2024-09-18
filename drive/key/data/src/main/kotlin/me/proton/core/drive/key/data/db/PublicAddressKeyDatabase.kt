/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.key.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.key.data.db.dao.StalePublicAddressKeyDao
import me.proton.core.drive.key.data.db.dao.PublicAddressKeyDataDao

interface PublicAddressKeyDatabase {
    val stalePublicAddressKeyDao: StalePublicAddressKeyDao
    val publicAddressKeyDataDao: PublicAddressKeyDataDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `StalePublicAddressKeyEntity` (
                        `user_id` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `key` TEXT NOT NULL,
                        PRIMARY KEY(`email`, `key`),
                        FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`email`, `key`) REFERENCES `PublicAddressKeyDataEntity`(`email`, `publicKey`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_StalePublicAddressKeyEntity_email` ON `StalePublicAddressKeyEntity` (`email`)
                    """.trimIndent()
                )
            }
        }
    }
}
