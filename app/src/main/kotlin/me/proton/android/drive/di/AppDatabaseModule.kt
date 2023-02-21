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
import me.proton.android.drive.db.AppDatabase
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
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.key.data.db.KeySaltDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import me.proton.drive.android.settings.data.db.AppUiSettingsDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppDatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.buildDatabase(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppDatabaseBindsModule {

    @Binds
    abstract fun provideVolumeDatabase(db: AppDatabase): VolumeDatabase

    @Binds
    abstract fun provideShareDatabase(db: AppDatabase): ShareDatabase

    @Binds
    abstract fun provideShareUrlDatabase(db: AppDatabase): ShareUrlDatabase

    @Binds
    abstract fun provideLinkDatabase(db: AppDatabase): LinkDatabase

    @Binds
    abstract fun provideFolderDatabase(db: AppDatabase): FolderDatabase

    @Binds
    abstract fun provideLinkAncestorDatabase(db: AppDatabase): LinkAncestorDatabase

    @Binds
    abstract fun provideLinkOfflineDatabase(db: AppDatabase): LinkOfflineDatabase

    @Binds
    abstract fun provideLinkDownloadDatabase(db: AppDatabase): LinkDownloadDatabase

    @Binds
    abstract fun provideLinkTrashDatabase(db: AppDatabase): LinkTrashDatabase

    @Binds
    abstract fun provideLinkSelectionDatabase(db: AppDatabase): LinkSelectionDatabase

    @Binds
    abstract fun provideMessageQueueDatabase(db: AppDatabase): MessageQueueDatabase

    @Binds
    abstract fun provideSortingDatabase(db: AppDatabase): SortingDatabase

    @Binds
    abstract fun provideLinkUploadDatabase(db: AppDatabase): LinkUploadDatabase

    @Binds
    abstract fun provideAccountDatabase(db: AppDatabase): AccountDatabase

    @Binds
    abstract fun provideUserDatabase(db: AppDatabase): UserDatabase

    @Binds
    abstract fun provideAddressDatabase(db: AppDatabase): AddressDatabase

    @Binds
    abstract fun provideFeatureFlagDatabase(db: AppDatabase): FeatureFlagDatabase

    @Binds
    abstract fun provideKeySaltDatabase(db: AppDatabase): KeySaltDatabase

    @Binds
    abstract fun providePublicAddressDatabase(db: AppDatabase): PublicAddressDatabase

    @Binds
    abstract fun provideHumanVerificationDatabase(db: AppDatabase): HumanVerificationDatabase

    @Binds
    abstract fun provideUserSettingsDatabase(db: AppDatabase): UserSettingsDatabase

    @Binds
    abstract fun provideOrganizationDatabase(db: AppDatabase): OrganizationDatabase

    @Binds
    abstract fun provideAppUiSettingsDatabase(db: AppDatabase): AppUiSettingsDatabase

    @Binds
    abstract fun provideEventMetadataDatabase(db: AppDatabase): EventMetadataDatabase

    @Binds
    abstract fun provideChallengeDatabase(appDatabase: AppDatabase): ChallengeDatabase

    @Binds
    abstract fun provideDriveLinkDatabase(db: AppDatabase): DriveLinkDatabase

    @Binds
    abstract fun provideDriveLinkPagedDatabase(db: AppDatabase): DriveLinkPagedDatabase

    @Binds
    abstract fun provideDriveLinkTrashDatabase(db: AppDatabase): DriveLinkTrashDatabase

    @Binds
    abstract fun provideDriveLinkOfflineDatabase(db: AppDatabase): DriveLinkOfflineDatabase

    @Binds
    abstract fun provideDriveLinkDownloadDatabase(db: AppDatabase): DriveLinkDownloadDatabase

    @Binds
    abstract fun provideDriveLinkSharedDatabase(db: AppDatabase): DriveLinkSharedDatabase

    @Binds
    abstract fun provideDriveLinkSelectionDatabase(db: AppDatabase): DriveLinkSelectionDatabase

    @Binds
    abstract fun provideNotificationDatabase(db: AppDatabase): NotificationDatabase

    @Binds
    abstract fun providePaymentDatabase(db: AppDatabase): PaymentDatabase
}
