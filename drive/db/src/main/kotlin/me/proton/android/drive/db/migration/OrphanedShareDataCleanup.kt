/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.android.drive.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.migration.DatabaseMigration

/**
 * Deletes rows whose `ShareEntity` no longer exists.
 *
 * `ShareDatabase.MIGRATION_4` and `ShareDatabase.MIGRATION_5` both run
 * `DELETE FROM ShareEntity WHERE type = 2` (`Share.Type.STANDARD`). Migrations execute with
 * `PRAGMA foreign_keys = OFF` - Room only enables enforcement in `onOpen`, which runs after
 * `onUpgrade` - so `ON DELETE CASCADE` never fired and every table keyed on those shares kept
 * its rows. Enabling the pragma later does not validate existing rows, so they survive
 * indefinitely.
 *
 * Orphaned `LinkEntity` rows crash `DriveLinkDao`: `base_volume_id` is read from a
 * `LEFT JOIN ShareEntity` into the non-null `DriveLinkEntity.volumeId`, producing
 * `getString(...) must not be null`.
 *
 * Every affected table carries a (user id, share id) pair resolving to `ShareEntity`, so the
 * cleanup is one statement per table and order-independent. Table and column names are literals
 * on purpose: a migration must keep describing the schema as it was, even if the entities are
 * later renamed.
 */
object OrphanedShareDataCleanup : DatabaseMigration {

    private data class ShareOwnedTable(
        val name: String,
        val userIdColumn: String = "user_id",
        val shareIdColumn: String = "share_id",
    )

    private val tables = listOf(
        // direct children of ShareEntity
        ShareOwnedTable("ShareUrlEntity"),
        ShareOwnedTable("ShareExternalInvitationEntity"),
        ShareOwnedTable("ShareInvitationEntity"),
        ShareOwnedTable("ShareMemberEntity"),
        ShareOwnedTable("ShareMembershipEntity"),
        ShareOwnedTable("LinkEntity"),
        ShareOwnedTable("FileDownloadEntity"),
        ShareOwnedTable("ParentLinkDownloadEntity"),
        ShareOwnedTable("BackupConfigurationEntity"),
        ShareOwnedTable("BackupDuplicateEntity"),
        ShareOwnedTable("BackupErrorEntity"),
        ShareOwnedTable("BackupFileEntity"),
        ShareOwnedTable("BackupFolderEntity"),
        ShareOwnedTable("UploadStatsEntity"),
        ShareOwnedTable("PhotoListingEntity"),
        ShareOwnedTable("RelatedPhotoEntity"),
        ShareOwnedTable("TaggedPhotoListingEntity"),
        ShareOwnedTable("TaggedRelatedPhotoEntity"),
        ShareOwnedTable("AlbumRelatedPhotoEntity"),
        ShareOwnedTable("AlbumPhotoListingEntity"),
        ShareOwnedTable("AlbumListingEntity"),
        ShareOwnedTable("AddToAlbumEntity"),
        ShareOwnedTable("TagsMigrationFileEntity"),
        ShareOwnedTable("DeviceEntity"),
        // children of LinkEntity
        ShareOwnedTable("LinkFilePropertiesEntity", "file_user_id", "file_share_id"),
        ShareOwnedTable("LinkFolderPropertiesEntity", "folder_user_id", "folder_share_id"),
        ShareOwnedTable("LinkAlbumPropertiesEntity", "album_user_id", "album_share_id"),
        ShareOwnedTable("LinkTagEntity", "tag_user_id", "tag_share_id"),
        ShareOwnedTable("LinkOfflineEntity"),
        ShareOwnedTable("LinkDownloadStateEntity"),
        ShareOwnedTable("LinkDownloadFileSignatureVerificationFailedEntity"),
        ShareOwnedTable("LinkTrashStateEntity"),
        ShareOwnedTable("TrashWorkEntity"),
        ShareOwnedTable("DriveLinkRemoteKeyEntity"),
        ShareOwnedTable("FolderMetadataEntity"),
        ShareOwnedTable("LinkSelectionEntity"),
        // grandchildren
        ShareOwnedTable("AlbumPhotoListingRemoteKeyEntity"),
        ShareOwnedTable("TagsMigrationFileTagEntity"),
        ShareOwnedTable("DownloadBlockEntity"),
    )

    override fun migrate(database: SupportSQLiteDatabase) {
        tables.forEach { table ->
            database.execSQL(
                """
                DELETE FROM `${table.name}` WHERE NOT EXISTS (
                    SELECT 1 FROM `ShareEntity`
                    WHERE `ShareEntity`.`user_id` = `${table.name}`.`${table.userIdColumn}`
                      AND `ShareEntity`.`id` = `${table.name}`.`${table.shareIdColumn}`
                )
                """.trimIndent()
            )
        }
    }
}
