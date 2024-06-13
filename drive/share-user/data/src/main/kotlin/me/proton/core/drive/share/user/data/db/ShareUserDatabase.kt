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
import me.proton.core.data.room.db.extension.dropTable
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.share.user.data.db.dao.ShareInvitationDao
import me.proton.core.drive.share.user.data.db.dao.ShareMemberDao
import me.proton.core.drive.share.user.data.db.dao.SharedByMeListingDao
import me.proton.core.drive.share.user.data.db.dao.SharedWithMeListingDao

interface ShareUserDatabase : Database {
    val shareInvitationDao: ShareInvitationDao
    val shareMemberDao: ShareMemberDao
    val sharedWithMeListingDao: SharedWithMeListingDao
    val sharedByMeListingDao: SharedByMeListingDao

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
                         ON UPDATE NO ACTION ON DELETE CASCADE,
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
                        ON UPDATE NO ACTION ON DELETE CASCADE,
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
        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.dropTable("ShareMemberEntity")
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
        val MIGRATION_3 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `SharedWithMeListingEntity` (
                        `user_id` TEXT NOT NULL,
                        `volume_id` TEXT NOT NULL,
                        `share_id` TEXT NOT NULL,
                        `link_id` TEXT NOT NULL,
                        PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `link_id`),
                        FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedWithMeListingEntity_user_id` ON `SharedWithMeListingEntity` (`user_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedWithMeListingEntity_volume_id` ON `SharedWithMeListingEntity` (`volume_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedWithMeListingEntity_share_id` ON `SharedWithMeListingEntity` (`share_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedWithMeListingEntity_link_id` ON `SharedWithMeListingEntity` (`link_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedWithMeListingEntity_user_id_volume_id_share_id` ON `SharedWithMeListingEntity` (`user_id`, `volume_id`, `share_id`)
                """.trimIndent()
                )
            }
        }
        val MIGRATION_4 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `SharedByMeListingEntity` (
                        `user_id` TEXT NOT NULL,
                        `volume_id` TEXT NOT NULL,
                        `share_id` TEXT NOT NULL,
                        `link_id` TEXT NOT NULL,
                        PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `link_id`),
                        FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedByMeListingEntity_user_id` ON `SharedByMeListingEntity` (`user_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedByMeListingEntity_volume_id` ON `SharedByMeListingEntity` (`volume_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedByMeListingEntity_share_id` ON `SharedByMeListingEntity` (`share_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedByMeListingEntity_link_id` ON `SharedByMeListingEntity` (`link_id`)
                """.trimIndent()
                )
                database.execSQL(
                    """
                   CREATE INDEX IF NOT EXISTS `index_SharedByMeListingEntity_user_id_volume_id_share_id` ON `SharedByMeListingEntity` (`user_id`, `volume_id`, `share_id`)
                """.trimIndent()
                )
            }
        }
    }
}
