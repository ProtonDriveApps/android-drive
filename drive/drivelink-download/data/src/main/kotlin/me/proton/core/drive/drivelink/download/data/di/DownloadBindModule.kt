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
package me.proton.core.drive.drivelink.download.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.drivelink.download.data.handler.ObservabilityDownloadErrorHandler
import me.proton.core.drive.drivelink.download.data.manager.DownloadErrorManagerImpl
import me.proton.core.drive.drivelink.download.data.repository.DownloadFileRepositoryImpl
import me.proton.core.drive.drivelink.download.data.repository.DownloadParentLinkRepositoryImpl
import me.proton.core.drive.drivelink.download.data.repository.DriveLinkDownloadRepositoryImpl
import me.proton.core.drive.drivelink.download.data.usecase.DownloadMetricsNotifierImpl
import me.proton.core.drive.drivelink.download.domain.handler.DownloadErrorHandler
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.drivelink.download.domain.repository.DownloadFileRepository
import me.proton.core.drive.drivelink.download.domain.repository.DownloadParentLinkRepository
import me.proton.core.drive.drivelink.download.domain.repository.DriveLinkDownloadRepository
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadMetricsNotifier
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface DownloadBindModule {

    @Binds
    @Singleton
    fun bindsRepositoryImpl(impl: DriveLinkDownloadRepositoryImpl): DriveLinkDownloadRepository

    @Binds
    @Singleton
    fun bindsDownloadFileRepository(impl: DownloadFileRepositoryImpl): DownloadFileRepository

    @Binds
    @Singleton
    fun bindsDownloadParentLinkRepository(impl: DownloadParentLinkRepositoryImpl): DownloadParentLinkRepository

    @Binds
    @Singleton
    fun bindsDownloadErrorManager(impl: DownloadErrorManagerImpl): DownloadErrorManager

    @Binds
    @Singleton
    fun bindsDownloadErrorHandler(impl: ObservabilityDownloadErrorHandler): DownloadErrorHandler

    @Binds
    @Singleton
    fun bindsDownloadMetricsNotifier(impl: DownloadMetricsNotifierImpl): DownloadMetricsNotifier
}
