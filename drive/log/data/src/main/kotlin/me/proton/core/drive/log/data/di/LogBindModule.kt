/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.log.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.log.data.repository.LogRepositoryImpl
import me.proton.core.drive.log.data.usecase.ExportLogImpl
import me.proton.core.drive.log.data.usecase.GetPagedLogsImpl
import me.proton.core.drive.log.domain.repository.LogRepository
import me.proton.core.drive.log.domain.usecase.ExportLog
import me.proton.core.drive.log.domain.usecase.GetPagedLogs
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface LogBindModule {

    @Binds
    @Singleton
    fun bindsRepositoryImpl(impl: LogRepositoryImpl): LogRepository

    @Binds
    fun bindsGetPagedLogsImpl(impl: GetPagedLogsImpl): GetPagedLogs

    @Binds
    fun bindsExportLogImpl(impl: ExportLogImpl): ExportLog
}
