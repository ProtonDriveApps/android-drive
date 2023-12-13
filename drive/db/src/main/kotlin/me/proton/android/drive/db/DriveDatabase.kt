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

package me.proton.android.drive.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.TypeConverters
import me.proton.android.drive.photos.data.db.MediaStoreVersionDatabase
import me.proton.android.drive.photos.data.db.entity.MediaStoreVersionEntity
import me.proton.core.account.data.db.AccountConverters
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.data.entity.AccountMetadataEntity
import me.proton.core.account.data.entity.SessionDetailsEntity
import me.proton.core.account.data.entity.SessionEntity
import me.proton.core.challenge.data.db.ChallengeConverters
import me.proton.core.challenge.data.db.ChallengeDatabase
import me.proton.core.challenge.data.entity.ChallengeFrameEntity
import me.proton.core.crypto.android.keystore.CryptoConverters
import me.proton.core.data.room.db.BaseDatabase
import me.proton.core.data.room.db.CommonConverters
import me.proton.core.drive.announce.event.data.db.EventConverters
import me.proton.core.drive.backup.data.db.BackupDatabase
import me.proton.core.drive.backup.data.db.entity.BackupDuplicateEntity
import me.proton.core.drive.backup.data.db.entity.BackupErrorEntity
import me.proton.core.drive.backup.data.db.entity.BackupFileEntity
import me.proton.core.drive.backup.data.db.entity.BackupFolderEntity
import me.proton.core.drive.drivelink.data.db.DriveLinkDatabase
import me.proton.core.drive.drivelink.download.data.db.DriveLinkDownloadDatabase
import me.proton.core.drive.drivelink.offline.data.db.DriveLinkOfflineDatabase
import me.proton.core.drive.drivelink.paged.data.db.DriveLinkPagedDatabase
import me.proton.core.drive.drivelink.paged.data.db.entity.DriveLinkRemoteKeyEntity
import me.proton.core.drive.drivelink.photo.data.db.DriveLinkPhotoDatabase
import me.proton.core.drive.drivelink.photo.data.db.entity.PhotoListingRemoteKeyEntity
import me.proton.core.drive.drivelink.selection.data.db.DriveLinkSelectionDatabase
import me.proton.core.drive.drivelink.shared.data.db.DriveLinkSharedDatabase
import me.proton.core.drive.drivelink.trash.data.db.DriveLinkTrashDatabase
import me.proton.core.drive.feature.flag.data.db.DriveFeatureFlagDatabase
import me.proton.core.drive.feature.flag.data.db.entity.DriveFeatureFlagRefreshEntity
import me.proton.core.drive.folder.data.db.FolderDatabase
import me.proton.core.drive.folder.data.db.FolderMetadataEntity
import me.proton.core.drive.link.data.db.LinkDatabase
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkFolderPropertiesEntity
import me.proton.core.drive.link.selection.data.db.LinkSelectionConverters
import me.proton.core.drive.link.selection.data.db.LinkSelectionDatabase
import me.proton.core.drive.link.selection.data.db.entity.LinkSelectionEntity
import me.proton.core.drive.linkdownload.data.db.LinkDownloadDatabase
import me.proton.core.drive.linkdownload.data.db.entity.DownloadBlockEntity
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadStateEntity
import me.proton.core.drive.linknode.data.db.LinkAncestorDatabase
import me.proton.core.drive.linkoffline.data.db.LinkOfflineDatabase
import me.proton.core.drive.linkoffline.data.db.LinkOfflineEntity
import me.proton.core.drive.linktrash.data.db.LinkTrashDatabase
import me.proton.core.drive.linktrash.data.db.entity.LinkTrashStateEntity
import me.proton.core.drive.linktrash.data.db.entity.TrashMetadataEntity
import me.proton.core.drive.linktrash.data.db.entity.TrashWorkEntity
import me.proton.core.drive.linkupload.data.db.LinkUploadDatabase
import me.proton.core.drive.linkupload.data.db.entity.LinkUploadEntity
import me.proton.core.drive.linkupload.data.db.entity.UploadBlockEntity
import me.proton.core.drive.linkupload.data.db.entity.UploadBulkEntity
import me.proton.core.drive.linkupload.data.db.entity.UploadBulkUriStringEntity
import me.proton.core.drive.messagequeue.data.storage.db.MessageQueueDatabase
import me.proton.core.drive.messagequeue.data.storage.db.entity.MessageEntity
import me.proton.core.drive.notification.data.db.NotificationDatabase
import me.proton.core.drive.notification.data.db.entity.NotificationChannelEntity
import me.proton.core.drive.notification.data.db.entity.NotificationEventEntity
import me.proton.core.drive.notification.data.db.entity.TaglessNotificationEventEntity
import me.proton.core.drive.photo.data.db.PhotoDatabase
import me.proton.core.drive.photo.data.db.entity.PhotoListingEntity
import me.proton.core.drive.share.data.db.ShareDatabase
import me.proton.core.drive.share.data.db.ShareEntity
import me.proton.core.drive.shareurl.base.data.db.ShareUrlDatabase
import me.proton.core.drive.shareurl.base.data.db.entity.ShareUrlEntity
import me.proton.core.drive.sorting.data.db.SortingDatabase
import me.proton.core.drive.sorting.data.db.entity.SortingEntity
import me.proton.core.drive.stats.data.db.StatsDatabase
import me.proton.core.drive.stats.data.db.entity.InitialBackupEntity
import me.proton.core.drive.stats.data.db.entity.UploadStatsEntity
import me.proton.core.drive.user.data.db.QuotaDatabase
import me.proton.core.drive.user.data.db.entity.DismissedQuotaEntity
import me.proton.core.drive.volume.data.db.VolumeDatabase
import me.proton.core.drive.volume.data.db.VolumeEntity
import me.proton.core.drive.worker.data.db.WorkerDatabase
import me.proton.core.drive.worker.data.db.WorkerRunEntity
import me.proton.core.eventmanager.data.db.EventManagerConverters
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.eventmanager.data.entity.EventMetadataEntity
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.featureflag.data.entity.FeatureFlagEntity
import me.proton.core.humanverification.data.db.HumanVerificationConverters
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.humanverification.data.entity.HumanVerificationEntity
import me.proton.core.key.data.db.KeySaltDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.key.data.entity.KeySaltEntity
import me.proton.core.key.data.entity.PublicAddressEntity
import me.proton.core.key.data.entity.PublicAddressKeyEntity
import me.proton.core.keytransparency.data.local.KeyTransparencyDatabase
import me.proton.core.keytransparency.data.local.entity.AddressChangeEntity
import me.proton.core.keytransparency.data.local.entity.SelfAuditResultEntity
import me.proton.core.notification.data.local.db.NotificationEntity
import me.proton.core.observability.data.db.ObservabilityDatabase
import me.proton.core.observability.data.entity.ObservabilityEventEntity
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.payment.data.local.entity.GooglePurchaseEntity
import me.proton.core.push.data.local.db.PushConverters
import me.proton.core.push.data.local.db.PushDatabase
import me.proton.core.push.data.local.db.PushEntity
import me.proton.core.telemetry.data.db.TelemetryDatabase
import me.proton.core.telemetry.data.entity.TelemetryEventEntity
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserConverters
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.data.entity.UserKeyEntity
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsConverters
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import me.proton.core.usersettings.data.entity.OrganizationEntity
import me.proton.core.usersettings.data.entity.OrganizationKeysEntity
import me.proton.core.usersettings.data.entity.UserSettingsEntity
import me.proton.drive.android.settings.data.db.AppUiSettingsDatabase
import me.proton.drive.android.settings.data.db.entity.UiSettingsEntity
import me.proton.core.notification.data.local.db.NotificationConverters as CoreNotificationConverters
import me.proton.core.notification.data.local.db.NotificationDatabase as CoreNotificationDatabase

@Database(
    entities = [
        // Core
        AccountEntity::class,
        AccountMetadataEntity::class,
        SessionEntity::class,
        SessionDetailsEntity::class,
        UserEntity::class,
        UserKeyEntity::class,
        AddressEntity::class,
        AddressKeyEntity::class,
        KeySaltEntity::class,
        PublicAddressEntity::class,
        PublicAddressKeyEntity::class,
        HumanVerificationEntity::class,
        UserSettingsEntity::class,
        OrganizationEntity::class,
        OrganizationKeysEntity::class,
        EventMetadataEntity::class,
        FeatureFlagEntity::class,
        ChallengeFrameEntity::class,
        GooglePurchaseEntity::class,
        ObservabilityEventEntity::class,
        AddressChangeEntity::class,
        SelfAuditResultEntity::class,
        NotificationEntity::class,
        PushEntity::class,
        TelemetryEventEntity::class,
        // Drive
        VolumeEntity::class,
        ShareEntity::class,
        ShareUrlEntity::class,
        LinkEntity::class,
        LinkFilePropertiesEntity::class,
        LinkFolderPropertiesEntity::class,
        LinkOfflineEntity::class,
        LinkDownloadStateEntity::class,
        DownloadBlockEntity::class,
        LinkTrashStateEntity::class,
        // Trash
        TrashWorkEntity::class,
        // MessageQueue
        MessageEntity::class,
        // AppUiSettings
        UiSettingsEntity::class,
        // DriveLinkPaged
        DriveLinkRemoteKeyEntity::class,
        // Sorting
        SortingEntity::class,
        // Upload
        LinkUploadEntity::class,
        UploadBlockEntity::class,
        UploadBulkEntity::class,
        UploadBulkUriStringEntity::class,
        FolderMetadataEntity::class,
        TrashMetadataEntity::class,
        // Backup
        BackupDuplicateEntity::class,
        BackupErrorEntity::class,
        BackupFileEntity::class,
        BackupFolderEntity::class,
        // Stats
        InitialBackupEntity::class,
        UploadStatsEntity::class,
        // Quota
        DismissedQuotaEntity::class,
        // Notification
        NotificationChannelEntity::class,
        NotificationEventEntity::class,
        TaglessNotificationEventEntity::class,
        // Selection
        LinkSelectionEntity::class,
        // Worker
        WorkerRunEntity::class,
        // Photos
        PhotoListingEntity::class,
        PhotoListingRemoteKeyEntity::class,
        // FeatureFlag
        DriveFeatureFlagRefreshEntity::class,
        MediaStoreVersionEntity::class,
    ],
    version = DriveDatabase.VERSION,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18, spec = ShareDatabase.DeleteBlockSizeFromShareEntity::class),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24),
        AutoMigration(from = 26, to = 27),
    ],
    exportSchema = true
)
@TypeConverters(
    // Core
    CommonConverters::class,
    AccountConverters::class,
    UserConverters::class,
    CryptoConverters::class,
    HumanVerificationConverters::class,
    UserSettingsConverters::class,
    EventManagerConverters::class,
    ChallengeConverters::class,
    CoreNotificationConverters::class,
    PushConverters::class,
    // Drive
    EventConverters::class,
    LinkSelectionConverters::class
)
abstract class DriveDatabase :
    BaseDatabase(),
    AccountDatabase,
    UserDatabase,
    AddressDatabase,
    KeySaltDatabase,
    HumanVerificationDatabase,
    PublicAddressDatabase,
    UserSettingsDatabase,
    OrganizationDatabase,
    FeatureFlagDatabase,
    VolumeDatabase,
    ShareDatabase,
    ShareUrlDatabase,
    LinkDatabase,
    FolderDatabase,
    LinkAncestorDatabase,
    LinkOfflineDatabase,
    LinkDownloadDatabase,
    LinkTrashDatabase,
    LinkSelectionDatabase,
    MessageQueueDatabase,
    AppUiSettingsDatabase,
    EventMetadataDatabase,
    ChallengeDatabase,
    SortingDatabase,
    LinkUploadDatabase,
    StatsDatabase,
    DriveLinkDatabase,
    DriveLinkPagedDatabase,
    DriveLinkTrashDatabase,
    DriveLinkOfflineDatabase,
    DriveLinkDownloadDatabase,
    DriveLinkSharedDatabase,
    DriveLinkSelectionDatabase,
    NotificationDatabase,
    PaymentDatabase,
    BackupDatabase,
    QuotaDatabase,
    ObservabilityDatabase,
    KeyTransparencyDatabase,
    WorkerDatabase,
    CoreNotificationDatabase,
    PushDatabase,
    TelemetryDatabase,
    PhotoDatabase,
    DriveLinkPhotoDatabase,
    DriveFeatureFlagDatabase,
    MediaStoreVersionDatabase {

    companion object {
        const val VERSION = 38

        private val migrations = listOf(
            DriveDatabaseMigrations.MIGRATION_1_2,
            DriveDatabaseMigrations.MIGRATION_2_3,
            DriveDatabaseMigrations.MIGRATION_3_4,
            // AutoMigration(from = 4, to = 5)
            // AutoMigration(from = 5, to = 6)
            DriveDatabaseMigrations.MIGRATION_6_7,
            // AutoMigration(from = 7, to = 8)
            DriveDatabaseMigrations.MIGRATION_8_9,
            // AutoMigration(from = 9, to = 10)
            DriveDatabaseMigrations.MIGRATION_10_11,
            DriveDatabaseMigrations.MIGRATION_11_12,
            DriveDatabaseMigrations.MIGRATION_12_13,
            // AutoMigration(from = 13, to = 14)
            DriveDatabaseMigrations.MIGRATION_14_15,
            // AutoMigration(from = 15, to = 16)
            // AutoMigration(from = 16, to = 17)
            // AutoMigration(from = 17, to = 18)
            // AutoMigration(from = 18, to = 19)
            DriveDatabaseMigrations.MIGRATION_19_20,
            DriveDatabaseMigrations.MIGRATION_20_21,
            DriveDatabaseMigrations.MIGRATION_21_22,
            // AutoMigration(from = 22, to = 23)
            // AutoMigration(from = 23, to = 24)
            DriveDatabaseMigrations.MIGRATION_24_25,
            DriveDatabaseMigrations.MIGRATION_25_26,
            // AutoMigration(from = 26, to = 27)
            DriveDatabaseMigrations.MIGRATION_27_28,
            DriveDatabaseMigrations.MIGRATION_28_29,
            DriveDatabaseMigrations.MIGRATION_29_30,
            DriveDatabaseMigrations.MIGRATION_30_31,
            DriveDatabaseMigrations.MIGRATION_31_32,
            DriveDatabaseMigrations.MIGRATION_32_33,
            DriveDatabaseMigrations.MIGRATION_33_34,
            DriveDatabaseMigrations.MIGRATION_34_35,
            DriveDatabaseMigrations.MIGRATION_35_36,
            DriveDatabaseMigrations.MIGRATION_36_37,
            DriveDatabaseMigrations.MIGRATION_37_38,
        )

        fun buildDatabase(context: Context): DriveDatabase =
            databaseBuilder<DriveDatabase>(context, "db-drive")
                .apply { migrations.forEach { addMigrations(it) } }
                .build()
    }
}
