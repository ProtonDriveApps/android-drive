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

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.DeviceInfo
import me.proton.core.drive.base.domain.usecase.GetCacheTempFolder
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.log.data.interceptor.LogInterceptor
import me.proton.core.drive.log.data.provider.BugReportLogProviderImpl
import me.proton.core.drive.log.domain.repository.LogRepository
import me.proton.core.network.domain.interceptor.InterceptorInfo
import me.proton.core.report.domain.provider.BugReportLogProvider
import okhttp3.Interceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LogModule {

    @Provides
    @Singleton
    fun provideLogInterceptor(): LogInterceptor = LogInterceptor()

    @Provides
    @Singleton
    @IntoSet
    fun provideInterceptor(
        logInterceptor: LogInterceptor,
    ): Pair<InterceptorInfo, Interceptor> =
        InterceptorInfo() to logInterceptor

    @Provides
    @Singleton
    fun provideBugReportLogProvider(
        getCacheTempFolder: GetCacheTempFolder,
        repository: LogRepository,
        dateTimeFormatter: DateTimeFormatter,
        accountManager: AccountManager,
        deviceInfo: DeviceInfo,
        configurationProvider: ConfigurationProvider,
        getFeatureFlag: GetFeatureFlag,
    ): BugReportLogProvider =
        BugReportLogProviderImpl(
            getCacheTempFolder = getCacheTempFolder,
            repository = repository,
            dateTimeFormatter = dateTimeFormatter,
            accountManager = accountManager,
            deviceInfo = deviceInfo,
            configurationProvider = configurationProvider,
            getFeatureFlag = getFeatureFlag,
        )
}
