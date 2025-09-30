/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.download.data.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.download.data.manager.DownloadManagerImpl
import me.proton.core.drive.drivelink.download.data.manager.PipelineManagerImpl
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.drivelink.download.domain.manager.DownloadManager
import me.proton.core.drive.drivelink.download.domain.repository.DownloadFileRepository
import me.proton.core.drive.drivelink.download.domain.repository.DownloadParentLinkRepository
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadCleanup
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadFile
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadMetricsNotifier
import me.proton.core.drive.folder.domain.usecase.GetDescendants
import me.proton.core.drive.linkdownload.domain.usecase.AreAllAlbumPhotosDownloaded
import me.proton.core.drive.linkdownload.domain.usecase.AreAllFilesDownloaded
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import me.proton.core.drive.linkoffline.domain.usecase.IsMarkedAsOffline
import me.proton.core.drive.photo.domain.usecase.GetAllAlbumChildren
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext appContext: Context,
        downloadFileRepository: DownloadFileRepository,
        downloadParentLinkRepository: DownloadParentLinkRepository,
        downloadFile: DownloadFile,
        downloadCleanup: DownloadCleanup,
        workManager: WorkManager,
        configurationProvider: ConfigurationProvider,
        areAllFilesDownloaded: AreAllFilesDownloaded,
        setDownloadState: SetDownloadState,
        getDescendants: GetDescendants,
        getDriveLink: GetDriveLink,
        isMarkedAsOffline: IsMarkedAsOffline,
        getAllAlbumChildren: GetAllAlbumChildren,
        areAllAlbumPhotosDownloaded: AreAllAlbumPhotosDownloaded,
        downloadErrorManager: DownloadErrorManager,
        downloadMetricsNotifier: DownloadMetricsNotifier,
    ): DownloadManager = DownloadManagerImpl(
        appContext = appContext,
        PipelineManagerImpl(configurationProvider.downloadsInParallel, LogTag.DOWNLOAD),
        downloadFileRepository,
        downloadParentLinkRepository,
        downloadFile,
        downloadCleanup,
        workManager,
        areAllFilesDownloaded,
        setDownloadState,
        getDescendants,
        getDriveLink,
        isMarkedAsOffline,
        configurationProvider,
        getAllAlbumChildren,
        areAllAlbumPhotosDownloaded,
        downloadErrorManager,
        downloadMetricsNotifier,
    )

    @Provides
    @Singleton
    fun provideDownloadManagerFileDownloader(
        downloadManager: DownloadManager,
    ): DownloadManager.FileDownloader = downloadManager as DownloadManager.FileDownloader
}
