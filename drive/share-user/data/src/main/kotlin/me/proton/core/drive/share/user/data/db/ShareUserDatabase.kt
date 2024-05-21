/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.share.user.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration

interface ShareUserDatabase : Database {
    val shareInvitationDao: ShareInvitationDao
    val shareMemberDao: ShareMemberDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ShareInvitationEntity` (
                        `id` TEXT NOT NULL, `user_id` TEXT NOT NULL, 
                        `share_id` TEXT NOT NULL, 
                        `inviter_email` TEXT NOT NULL, 
                        `invitee_email` TEXT NOT NULL, 
                        `permissions` INTEGER NOT NULL, 
                        `key_packet` TEXT NOT NULL, 
                        `key_packet_signature` TEXT NOT NULL, 
                        `create_time` INTEGER NOT NULL,
                         PRIMARY KEY(`user_id`, `share_id`, `id`), 
                         FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                         ON UPDATE NO ACTION ON DELETE CASCADE , 
                         FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                         ON UPDATE NO ACTION ON DELETE CASCADE 
                     )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_ShareInvitationEntity_user_id` ON `ShareInvitationEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_ShareInvitationEntity_user_id_share_id` ON `ShareInvitationEntity` (`user_id`, `share_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_ShareInvitationEntity_id` ON `ShareInvitationEntity` (`id`)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ShareMemberEntity` (
                        `id` TEXT NOT NULL, 
                        `user_id` TEXT NOT NULL, 
                        `share_id` TEXT NOT NULL, 
                        `inviter_email` TEXT NOT NULL, 
                        `invitee_email` TEXT NOT NULL, 
                        `permissions` INTEGER NOT NULL, 
                        `key_packet` TEXT NOT NULL, 
                        `key_packet_signature` TEXT NOT NULL, 
                        `session_key_signature` TEXT NOT NULL, 
                        `create_time` INTEGER NOT NULL, 
                        PRIMARY KEY(`user_id`, `share_id`, `id`), 
                        FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                        ON UPDATE NO ACTION ON DELETE CASCADE , 
                        FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                        ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_ShareMemberEntity_user_id` ON `ShareMemberEntity` (`user_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_ShareMemberEntity_user_id_share_id` ON `ShareMemberEntity` (`user_id`, `share_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_ShareMemberEntity_id` ON `ShareMemberEntity` (`id`)
                """.trimIndent()
                )
            }

        }
    }
}
