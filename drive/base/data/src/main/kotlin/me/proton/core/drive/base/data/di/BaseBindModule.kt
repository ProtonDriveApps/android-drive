/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.base.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.base.data.formatter.DateTimeFormatterImpl
import me.proton.core.drive.base.data.repository.BaseRepositoryImpl
import me.proton.core.drive.base.data.usecase.CopyToClipboardImpl
import me.proton.core.drive.base.data.usecase.GetInternalStorageInfoImpl
import me.proton.core.drive.base.data.usecase.GetMemoryInfoImpl
import me.proton.core.drive.base.data.usecase.Sha256Impl
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.repository.BaseRepository
import me.proton.core.drive.base.domain.usecase.CopyToClipboard
import me.proton.core.drive.base.domain.usecase.GetInternalStorageInfo
import me.proton.core.drive.base.domain.usecase.GetMemoryInfo
import me.proton.core.drive.base.domain.usecase.Sha256
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface BaseBindModule {

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
    fun bindsGetMemoryInfoImpl(impl: GetMemoryInfoImpl): GetMemoryInfo

    @Binds
    @Singleton
    fun bindsGetInternalStorageInfoImpl(impl: GetInternalStorageInfoImpl): GetInternalStorageInfo

    @Binds
    @Singleton
    fun bindsRepositoryImpl(impl: BaseRepositoryImpl): BaseRepository
}
