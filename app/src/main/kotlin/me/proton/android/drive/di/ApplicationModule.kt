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

import android.app.ActivityManager
import android.content.ClipboardManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.log.DriveLogger
import me.proton.android.drive.notification.AppNotificationBuilderProvider
import me.proton.android.drive.notification.NotificationEventHandler
import me.proton.android.drive.photos.domain.handler.PhotosEventHandler
import me.proton.android.drive.provider.BuildConfigurationProvider
import me.proton.android.drive.repository.BridgeFindDuplicatesRepository
import me.proton.android.drive.repository.ClientUidRepositoryImpl
import me.proton.android.drive.settings.DebugSettings
import me.proton.android.drive.stats.StatsEventHandler
import me.proton.android.drive.telemetry.TelemetryEventHandler
import me.proton.android.drive.usecase.GetDocumentsProviderRootsImpl
import me.proton.android.drive.usecase.notification.UploadNotificationEventWorkerNotifier
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.domain.entity.AppStore
import me.proton.core.domain.entity.Product
import me.proton.core.drive.backup.domain.repository.FindDuplicatesRepository
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.ClientUidRepository
import me.proton.core.drive.documentsprovider.domain.usecase.GetDocumentsProviderRoots
import me.proton.core.drive.notification.data.provider.NotificationBuilderProvider
import me.proton.core.drive.upload.data.worker.UploadEventWorker
import me.proton.drive.android.settings.data.datastore.AppUiSettingsDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context) = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideNotificationManagerCompat(@ApplicationContext context: Context): NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    @Provides
    @Singleton
    fun provideBuildConfigurationProvider() =
        BuildConfigurationProvider()

    @Provides
    @Singleton
    fun provideDebugSettings(
        @ApplicationContext context: Context,
        buildConfigurationProvider: BuildConfigurationProvider,
    ): DebugSettings =
        DebugSettings(context, buildConfigurationProvider)

    @Provides
    @Singleton
    fun provideConfigurationProvider(
        debugSettings: DebugSettings,
        buildConfigurationProvider: BuildConfigurationProvider,
    ): ConfigurationProvider =
        if (BuildConfig.DEBUG || BuildConfig.FLAVOR == BuildConfig.FLAVOR_ALPHA) {
            debugSettings
        } else {
            buildConfigurationProvider
        }

    @Provides
    @Singleton
    fun provideDriveLogger(@ApplicationContext context: Context): DriveLogger = DriveLogger(context)

    @Provides
    @Singleton
    fun provideProduct(): Product =
        Product.Drive

    @Provides
    @Singleton
    fun provideAppStore() =
        AppStore.GooglePlay

    @Provides
    @Singleton
    fun provideRequiredAccountType(): AccountType =
        AccountType.External

    @Provides
    @Singleton
    fun provideAppUiSettingsDataStore(@ApplicationContext context: Context) =
        AppUiSettingsDataStore(context)

    @Provides
    @Singleton
    fun provideClipboardManager(@ApplicationContext context: Context): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    @Singleton
    fun provideActivityManager(@ApplicationContext context: Context): ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideEventHandlers(
        notification: NotificationEventHandler,
        stats: StatsEventHandler,
        photos: PhotosEventHandler,
        telemetry: TelemetryEventHandler,
    ) = setOf(notification, telemetry, photos, stats)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationBindsModule {

    @Binds
    @Singleton
    abstract fun bindsNotificationBuilderProvider(
        impl: AppNotificationBuilderProvider,
    ): NotificationBuilderProvider

    @Binds
    @Singleton
    abstract fun bindsGetDocumentsProviderRootsImpl(impl: GetDocumentsProviderRootsImpl): GetDocumentsProviderRoots

    @Binds
    @Singleton
    abstract fun bindsClientUidRepositoryImpl(impl: ClientUidRepositoryImpl): ClientUidRepository

    @Binds
    @Singleton
    abstract fun bindsBridgeFindDuplicatesRepository(impl: BridgeFindDuplicatesRepository): FindDuplicatesRepository

    @Binds
    abstract fun bindsUploadNotificationEventWorkerNotifier(impl: UploadNotificationEventWorkerNotifier): UploadEventWorker.Notifier
}
