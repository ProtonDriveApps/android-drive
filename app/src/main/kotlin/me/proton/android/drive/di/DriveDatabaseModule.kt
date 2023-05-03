/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.android.drive.db.DriveDatabase
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.challenge.data.db.ChallengeDatabase
import me.proton.core.drive.drivelink.data.db.DriveLinkDatabase
import me.proton.core.drive.drivelink.download.data.db.DriveLinkDownloadDatabase
import me.proton.core.drive.drivelink.offline.data.db.DriveLinkOfflineDatabase
import me.proton.core.drive.drivelink.paged.data.db.DriveLinkPagedDatabase
import me.proton.core.drive.drivelink.selection.data.db.DriveLinkSelectionDatabase
import me.proton.core.drive.drivelink.shared.data.db.DriveLinkSharedDatabase
import me.proton.core.drive.drivelink.trash.data.db.DriveLinkTrashDatabase
import me.proton.core.drive.folder.data.db.FolderDatabase
import me.proton.core.drive.link.data.db.LinkDatabase
import me.proton.core.drive.link.selection.data.db.LinkSelectionDatabase
import me.proton.core.drive.linkdownload.data.db.LinkDownloadDatabase
import me.proton.core.drive.linknode.data.db.LinkAncestorDatabase
import me.proton.core.drive.linkoffline.data.db.LinkOfflineDatabase
import me.proton.core.drive.linktrash.data.db.LinkTrashDatabase
import me.proton.core.drive.linkupload.data.db.LinkUploadDatabase
import me.proton.core.drive.messagequeue.data.storage.db.MessageQueueDatabase
import me.proton.core.drive.notification.data.db.NotificationDatabase
import me.proton.core.drive.share.data.db.ShareDatabase
import me.proton.core.drive.shareurl.base.data.db.ShareUrlDatabase
import me.proton.core.drive.sorting.data.db.SortingDatabase
import me.proton.core.drive.volume.data.db.VolumeDatabase
import me.proton.core.drive.worker.data.db.WorkerDatabase
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.key.data.db.KeySaltDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.keytransparency.data.local.KeyTransparencyDatabase
import me.proton.core.observability.data.db.ObservabilityDatabase
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import me.proton.drive.android.settings.data.db.AppUiSettingsDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DriveDatabaseModule {
    @Provides
    @Singleton
    fun provideDriveDatabase(@ApplicationContext context: Context): DriveDatabase =
        DriveDatabase.buildDatabase(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DriveDatabaseBindsModule {

    @Binds
    abstract fun provideVolumeDatabase(db: DriveDatabase): VolumeDatabase

    @Binds
    abstract fun provideShareDatabase(db: DriveDatabase): ShareDatabase

    @Binds
    abstract fun provideShareUrlDatabase(db: DriveDatabase): ShareUrlDatabase

    @Binds
    abstract fun provideLinkDatabase(db: DriveDatabase): LinkDatabase

    @Binds
    abstract fun provideFolderDatabase(db: DriveDatabase): FolderDatabase

    @Binds
    abstract fun provideLinkAncestorDatabase(db: DriveDatabase): LinkAncestorDatabase

    @Binds
    abstract fun provideLinkOfflineDatabase(db: DriveDatabase): LinkOfflineDatabase

    @Binds
    abstract fun provideLinkDownloadDatabase(db: DriveDatabase): LinkDownloadDatabase

    @Binds
    abstract fun provideLinkTrashDatabase(db: DriveDatabase): LinkTrashDatabase

    @Binds
    abstract fun provideLinkSelectionDatabase(db: DriveDatabase): LinkSelectionDatabase

    @Binds
    abstract fun provideMessageQueueDatabase(db: DriveDatabase): MessageQueueDatabase

    @Binds
    abstract fun provideSortingDatabase(db: DriveDatabase): SortingDatabase

    @Binds
    abstract fun provideLinkUploadDatabase(db: DriveDatabase): LinkUploadDatabase

    @Binds
    abstract fun provideAccountDatabase(db: DriveDatabase): AccountDatabase

    @Binds
    abstract fun provideUserDatabase(db: DriveDatabase): UserDatabase

    @Binds
    abstract fun provideAddressDatabase(db: DriveDatabase): AddressDatabase

    @Binds
    abstract fun provideFeatureFlagDatabase(db: DriveDatabase): FeatureFlagDatabase

    @Binds
    abstract fun provideKeySaltDatabase(db: DriveDatabase): KeySaltDatabase

    @Binds
    abstract fun providePublicAddressDatabase(db: DriveDatabase): PublicAddressDatabase

    @Binds
    abstract fun provideHumanVerificationDatabase(db: DriveDatabase): HumanVerificationDatabase

    @Binds
    abstract fun provideUserSettingsDatabase(db: DriveDatabase): UserSettingsDatabase

    @Binds
    abstract fun provideOrganizationDatabase(db: DriveDatabase): OrganizationDatabase

    @Binds
    abstract fun provideAppUiSettingsDatabase(db: DriveDatabase): AppUiSettingsDatabase

    @Binds
    abstract fun provideEventMetadataDatabase(db: DriveDatabase): EventMetadataDatabase

    @Binds
    abstract fun provideChallengeDatabase(driveDatabase: DriveDatabase): ChallengeDatabase

    @Binds
    abstract fun provideDriveLinkDatabase(db: DriveDatabase): DriveLinkDatabase

    @Binds
    abstract fun provideDriveLinkPagedDatabase(db: DriveDatabase): DriveLinkPagedDatabase

    @Binds
    abstract fun provideDriveLinkTrashDatabase(db: DriveDatabase): DriveLinkTrashDatabase

    @Binds
    abstract fun provideDriveLinkOfflineDatabase(db: DriveDatabase): DriveLinkOfflineDatabase

    @Binds
    abstract fun provideDriveLinkDownloadDatabase(db: DriveDatabase): DriveLinkDownloadDatabase

    @Binds
    abstract fun provideDriveLinkSharedDatabase(db: DriveDatabase): DriveLinkSharedDatabase

    @Binds
    abstract fun provideDriveLinkSelectionDatabase(db: DriveDatabase): DriveLinkSelectionDatabase

    @Binds
    abstract fun provideNotificationDatabase(db: DriveDatabase): NotificationDatabase

    @Binds
    abstract fun providePaymentDatabase(db: DriveDatabase): PaymentDatabase

    @Binds
    abstract fun provideObservabilityDatabase(db: DriveDatabase): ObservabilityDatabase

    @Binds
    abstract fun provideKeyTransparencyDatabase(db: DriveDatabase): KeyTransparencyDatabase

    @Binds
    abstract fun provideWorkerDatabase(db: DriveDatabase): WorkerDatabase
}
