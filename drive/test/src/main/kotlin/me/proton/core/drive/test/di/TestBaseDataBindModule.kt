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

package me.proton.core.drive.test.di

import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import me.proton.core.drive.base.data.di.BaseBindModule
import me.proton.core.drive.base.data.formatter.DateTimeFormatterImpl
import me.proton.core.drive.base.data.repository.BaseRepositoryImpl
import me.proton.core.drive.base.data.usecase.CopyToClipboardImpl
import me.proton.core.drive.base.data.usecase.DeviceInfoImpl
import me.proton.core.drive.base.data.usecase.IsRetryableImpl
import me.proton.core.drive.base.data.usecase.LogReportError
import me.proton.core.drive.base.data.usecase.Sha256Impl
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.log.ItemLifecycle
import me.proton.core.drive.base.domain.provider.BuildConfigFieldsProvider
import me.proton.core.drive.base.domain.repository.BaseRepository
import me.proton.core.drive.base.domain.usecase.CopyToClipboard
import me.proton.core.drive.base.domain.usecase.DeviceInfo
import me.proton.core.drive.base.domain.usecase.GetInternalStorageInfo
import me.proton.core.drive.base.domain.usecase.GetMemoryInfo
import me.proton.core.drive.base.domain.usecase.IsRetryable
import me.proton.core.drive.base.domain.usecase.ReportError
import me.proton.core.drive.base.domain.usecase.Sha256
import me.proton.core.drive.test.TestBuildConfigFieldsProvider
import me.proton.core.drive.test.log.TestItemLifecycle
import me.proton.core.drive.test.usecase.TestGetInternalStorageInfo
import me.proton.core.drive.test.usecase.TestGetMemoryInfo
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [BaseBindModule::class]
)
interface TestBaseDataBindModule {

    @Binds
    @Singleton
    fun bindsDateTimeFormatterImpl(impl: DateTimeFormatterImpl): DateTimeFormatter

    @Binds
    @Singleton
    fun bindsCopyToClipboardImpl(impl: CopyToClipboardImpl): CopyToClipboard

    @Binds
    @Singleton
    fun bindsSha256Impl(impl: Sha256Impl): Sha256

    @Binds
    @Singleton
    fun bindsItemLifecycle(impl: TestItemLifecycle): ItemLifecycle

    @Binds
    @Singleton
    fun bindsGetMemoryInfoImpl(impl: TestGetMemoryInfo): GetMemoryInfo

    @Binds
    @Singleton
    fun bindsGetInternalStorageInfoImpl(impl: TestGetInternalStorageInfo): GetInternalStorageInfo

    @Binds
    @Singleton
    fun bindsRepositoryImpl(impl: BaseRepositoryImpl): BaseRepository

    @Binds
    @Singleton
    fun bindsDeviceInfoImpl(impl: DeviceInfoImpl): DeviceInfo

    @Binds
    @Singleton
    fun provideBuildConfigFieldsProvider(impl: TestBuildConfigFieldsProvider): BuildConfigFieldsProvider

    @Binds
    @Singleton
    fun provideReportError(impl: LogReportError): ReportError

    @Binds
    @Singleton
    fun bindsIsRetryable(impl: IsRetryableImpl): IsRetryable
}
