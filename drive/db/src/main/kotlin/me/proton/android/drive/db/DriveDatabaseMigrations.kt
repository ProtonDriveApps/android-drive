/*
 * Copyright (c) 2023-2024 Proton AG.
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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.android.drive.photos.data.db.MediaStoreVersionDatabase
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.challenge.data.db.ChallengeDatabase
import me.proton.core.contact.data.local.db.ContactDatabase
import me.proton.core.drive.backup.data.db.BackupDatabase
import me.proton.core.drive.base.data.db.BaseDatabase
import me.proton.core.drive.device.data.db.DeviceDatabase
import me.proton.core.drive.drivelink.photo.data.db.DriveLinkPhotoDatabase
import me.proton.core.drive.drivelink.shared.data.db.DriveLinkSharedDatabase
import me.proton.core.drive.feature.flag.data.db.DriveFeatureFlagDatabase
import me.proton.core.drive.link.data.db.LinkDatabase
import me.proton.core.drive.link.selection.data.db.LinkSelectionDatabase
import me.proton.core.drive.linktrash.data.db.LinkTrashDatabase
import me.proton.core.drive.linkupload.data.db.LinkUploadDatabase
import me.proton.core.drive.log.data.db.LogDatabase
import me.proton.core.drive.notification.data.db.NotificationDatabase
import me.proton.core.drive.photo.data.db.PhotoDatabase
import me.proton.core.drive.share.data.db.ShareDatabase
import me.proton.core.drive.share.user.data.db.ShareUserDatabase
import me.proton.core.drive.shareurl.base.data.db.ShareUrlDatabase
import me.proton.core.drive.stats.data.db.StatsDatabase
import me.proton.core.drive.user.data.db.UserMessageDatabase
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.keytransparency.data.local.KeyTransparencyDatabase
import me.proton.core.observability.data.db.ObservabilityDatabase
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.push.data.local.db.PushDatabase
import me.proton.core.telemetry.data.db.TelemetryDatabase
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.user.data.db.UserKeyDatabase
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import me.proton.drive.android.settings.data.db.AppUiSettingsDatabase
import me.proton.core.notification.data.local.db.NotificationDatabase as CoreNotificationDatabase

@Suppress("MagicNumber")
object DriveDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            NotificationDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            FeatureFlagDatabase.MIGRATION_0.migrate(database)
            FeatureFlagDatabase.MIGRATION_1.migrate(database)
            OrganizationDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ShareDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ChallengeDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ChallengeDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            LinkSelectionDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            FeatureFlagDatabase.MIGRATION_2.migrate(database)
            FeatureFlagDatabase.MIGRATION_3.migrate(database)
            HumanVerificationDatabase.MIGRATION_1.migrate(database)
            HumanVerificationDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            PaymentDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(database: SupportSQLiteDatabase) {
            LinkDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_5.migrate(database)
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ObservabilityDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(database: SupportSQLiteDatabase) {
            OrganizationDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AddressDatabase.MIGRATION_4.migrate(database)
            PublicAddressDatabase.MIGRATION_2.migrate(database)
            KeyTransparencyDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserDatabase.MIGRATION_2.migrate(database)
            CoreNotificationDatabase.MIGRATION_0.migrate(database)
            CoreNotificationDatabase.MIGRATION_1.migrate(database)
            PushDatabase.MIGRATION_0.migrate(database)
            UserSettingsDatabase.MIGRATION_2.migrate(database)
            EventMetadataDatabase.MIGRATION_1.migrate(database)
            UserDatabase.MIGRATION_3.migrate(database)
            AccountDatabase.MIGRATION_6.migrate(database)
            ShareDatabase.MIGRATION_1.migrate(database)
            LinkTrashDatabase.MIGRATION_0.migrate(database)
            LinkUploadDatabase.MIGRATION_0.migrate(database)
            TelemetryDatabase.MIGRATION_0.migrate(database)
            UserSettingsDatabase.MIGRATION_3.migrate(database)
            LinkDatabase.MIGRATION_1.migrate(database)
            LinkUploadDatabase.MIGRATION_1.migrate(database)
            BackupDatabase.MIGRATION_0.migrate(database)
            UserMessageDatabase.MIGRATION_0.migrate(database)
            NotificationDatabase.MIGRATION_1.migrate(database)
            PhotoDatabase.MIGRATION_0.migrate(database)
            DriveLinkPhotoDatabase.MIGRATION_0.migrate(database)
            DriveFeatureFlagDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(database: SupportSQLiteDatabase) {
            BackupDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MediaStoreVersionDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ShareUrlDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(database: SupportSQLiteDatabase) {
            LinkUploadDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_32_33 = object : Migration(32, 33) {
        override fun migrate(database: SupportSQLiteDatabase) {
            EventMetadataDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_33_34 = object : Migration(33, 34) {
        override fun migrate(database: SupportSQLiteDatabase) {
            NotificationDatabase.MIGRATION_2.migrate(database)
            StatsDatabase.MIGRATION_O.migrate(database)
        }
    }

    val MIGRATION_34_35 = object : Migration(34, 35) {
        override fun migrate(database: SupportSQLiteDatabase) {
            StatsDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_35_36 = object : Migration(35, 36) {
        override fun migrate(database: SupportSQLiteDatabase) {
            BackupDatabase.MIGRATION_2.migrate(database)
            LinkUploadDatabase.MIGRATION_3.migrate(database)
            BackupDatabase.MIGRATION_3.migrate(database)
        }
    }

    val MIGRATION_36_37 = object : Migration(36, 37) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_4.migrate(database)
        }
    }

    val MIGRATION_37_38 = object : Migration(37, 38) {
        override fun migrate(database: SupportSQLiteDatabase) {
            DriveFeatureFlagDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_38_39 = object : Migration(38, 39) {
        override fun migrate(database: SupportSQLiteDatabase) {
            BackupDatabase.MIGRATION_4.migrate(database)
        }
    }

    val MIGRATION_39_40 = object : Migration(39, 40) {
        override fun migrate(database: SupportSQLiteDatabase) {
            LinkUploadDatabase.MIGRATION_4.migrate(database)
            BackupDatabase.MIGRATION_5.migrate(database)
        }
    }

    val MIGRATION_40_41 = object : Migration(40, 41) {
        override fun migrate(database: SupportSQLiteDatabase) {
            BackupDatabase.MIGRATION_6.migrate(database)
        }
    }

    val MIGRATION_41_42 = object : Migration(41, 42) {
        override fun migrate(database: SupportSQLiteDatabase) {
            NotificationDatabase.MIGRATION_3.migrate(database)
            LinkUploadDatabase.MIGRATION_5.migrate(database)
        }
    }

    val MIGRATION_42_43 = object : Migration(42, 43) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_5.migrate(database)
            UserKeyDatabase.MIGRATION_0.migrate(database)
            UserDatabase.MIGRATION_4.migrate(database)
        }
    }

    val MIGRATION_43_44 = object : Migration(43, 44) {
        override fun migrate(database: SupportSQLiteDatabase) {
            BackupDatabase.MIGRATION_7.migrate(database)
        }
    }

    val MIGRATION_44_45 = object : Migration(44, 45) {
        override fun migrate(database: SupportSQLiteDatabase) {
            DeviceDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_45_46 = object : Migration(45, 46) {
        override fun migrate(database: SupportSQLiteDatabase) {
            LinkTrashDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_46_47 = object : Migration(46, 47) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ContactDatabase.MIGRATION_0.migrate(database)
            ContactDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_47_48 = object : Migration(47, 48) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserDatabase.MIGRATION_5.migrate(database)
            AccountDatabase.MIGRATION_7.migrate(database)
        }
    }

    val MIGRATION_48_49 = object : Migration(48, 49) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserMessageDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_49_50 = object : Migration(49, 50) {
        override fun migrate(database: SupportSQLiteDatabase) {
            BaseDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_50_51 = object : Migration(50, 51) {
        override fun migrate(database: SupportSQLiteDatabase) {
            PaymentDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_51_52 = object : Migration(51, 52) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ShareUserDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_52_53 = object : Migration(52, 53) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ShareUserDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_53_54 = object : Migration(53, 54) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AppUiSettingsDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_54_55 = object : Migration(54, 55) {
        override fun migrate(database: SupportSQLiteDatabase) {
            LogDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_55_56 = object : Migration(55, 56) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ShareUserDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_56_57 = object : Migration(56, 57) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ShareUserDatabase.MIGRATION_3.migrate(database)
            DriveLinkSharedDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_57_58 = object : Migration(57, 58) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ShareUserDatabase.MIGRATION_4.migrate(database)
        }
    }

    val MIGRATION_58_59 = object : Migration(58, 59) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_6.migrate(database)
        }
    }

    val MIGRATION_59_60 = object : Migration(59, 60) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ShareDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_60_61 = object : Migration(60, 61) {
        override fun migrate(database: SupportSQLiteDatabase) {
            LogDatabase.MIGRATION_1.migrate(database)
        }
    }
}
