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
package me.proton.core.drive.shareurl.base.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.shareurl.base.data.db.dao.ShareUrlDao

interface ShareUrlDatabase : Database {
    val shareUrlDao: ShareUrlDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        DROP TABLE `ShareUrlEntity`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE TABLE `ShareUrlEntity` (
                            `${Column.ID}` TEXT NOT NULL,
                            `${Column.USER_ID}` TEXT NOT NULL,
                            `${Column.VOLUME_ID}` TEXT NOT NULL,
                            `${Column.SHARE_ID}` TEXT NOT NULL,
                            `${Column.FLAGS}` INTEGER NOT NULL,
                            `${Column.NAME}` TEXT,
                            `${Column.TOKEN}` TEXT NOT NULL,
                            `${Column.CREATOR_EMAIL}` TEXT NOT NULL,
                            `${Column.PERMISSIONS}` INTEGER NOT NULL,
                            `${Column.CREATION_TIME}` INTEGER NOT NULL,
                            `${Column.EXPIRATION_TIME}` INTEGER,
                            `${Column.LAST_ACCESS_TIME}` INTEGER,
                            `${Column.MAX_ACCESSES}` INTEGER,
                            `${Column.NUMBER_OF_ACCESSES}` INTEGER NOT NULL,
                            `${Column.URL_PASSWORD_SALT}` TEXT NOT NULL,
                            `${Column.SHARE_PASSWORD_SALT}` TEXT NOT NULL,
                            `${Column.SRP_VERIFIER}` TEXT NOT NULL,
                            `${Column.SRP_MODULUS_ID}` TEXT NOT NULL,
                            `${Column.PASSWORD}` TEXT NOT NULL,
                            `${Column.SHARE_PASSPHRASE_KEY_PACKET}` TEXT NOT NULL,
                            `${Column.PUBLIC_URL}` TEXT NOT NULL DEFAULT '',
                            PRIMARY KEY(`${Column.USER_ID}`, ${Column.VOLUME_ID}, `${Column.SHARE_ID}`, `${Column.ID}`),
                            FOREIGN KEY(`${Column.USER_ID}`, `${Column.SHARE_ID}`)
                                REFERENCES `ShareEntity`(`${Column.USER_ID}`, `${Column.ID}`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_ShareUrlEntity_user_id` ON `ShareUrlEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_ShareUrlEntity_volume_id` ON `ShareUrlEntity` (`volume_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_ShareUrlEntity_share_id` ON `ShareUrlEntity` (`share_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_ShareUrlEntity_user_id_share_id` ON `ShareUrlEntity` (`user_id`, `share_id`)
                    """.trimIndent()
                )
            }
        }
    }
}
