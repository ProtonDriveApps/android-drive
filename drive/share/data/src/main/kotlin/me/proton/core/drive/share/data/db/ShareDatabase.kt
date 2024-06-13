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
package me.proton.core.drive.share.data.db

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column.BLOCK_SIZE
import me.proton.core.drive.base.data.db.Column.CREATION_TIME
import me.proton.core.drive.base.data.db.Column.FLAGS
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.TYPE
import me.proton.core.drive.share.data.db.ShareEntity.Companion.PRIMARY_BIT

interface ShareDatabase : Database {
    val shareDao: ShareDao
    val shareMembershipDao: ShareMembershipDao

    @DeleteColumn(tableName = "ShareEntity", columnName = BLOCK_SIZE)
    class DeleteBlockSizeFromShareEntity : AutoMigrationSpec

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE `ShareEntity` ADD COLUMN $CREATION_TIME INTEGER DEFAULT NULL
                """.trimIndent()
                )
            }
        }
        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE `ShareEntity` ADD COLUMN $TYPE INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
                )
                database.updateShareEntityType()
            }
        }
        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ShareMembershipEntity` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `share_id` TEXT NOT NULL,
                        `inviter_email` TEXT NOT NULL,
                        `invitee_email` TEXT NOT NULL,
                        `permissions` INTEGER NOT NULL,
                        `key_packet` TEXT NOT NULL,
                        `key_packet_signature` TEXT NULL,
                        `session_key_signature` TEXT NULL,
                        `create_time` INTEGER NOT NULL,
                        PRIMARY KEY(`user_id`, `share_id`, `id`),
                        FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_ShareMembershipEntity_user_id` ON `ShareMembershipEntity` (`user_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_ShareMembershipEntity_user_id_share_id` ON `ShareMembershipEntity` (`user_id`, `share_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_ShareMembershipEntity_id` ON `ShareMembershipEntity` (`id`)
                """.trimIndent()
                )
            }
        }

        private fun SupportSQLiteDatabase.updateShareEntityType() {
            query("SELECT * FROM ShareEntity")?.use { cursor ->
                while (cursor.moveToNext()) {
                    val idIndex = cursor.getColumnIndex(ID)
                    val flagsIndex = cursor.getColumnIndex(FLAGS)
                    if (idIndex < 0 || flagsIndex < 0) continue
                    val id = cursor.getString(idIndex)
                    val flags = cursor.getLong(flagsIndex)
                    val type = if ((flags and PRIMARY_BIT) == 1L) 1L else 2L
                    execSQL("""
                        UPDATE `ShareEntity` SET $TYPE = $type WHERE $ID = "$id"
                    """.trimIndent()
                    )
                }
            }
        }
    }
}
