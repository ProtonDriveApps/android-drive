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
package me.proton.core.drive.upload.data.di

import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import me.proton.core.drive.base.domain.provider.MimeTypeProvider
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.upload.data.resolver.ContentUriResolver
import me.proton.core.drive.upload.data.resolver.FileUriResolver
import me.proton.core.drive.upload.data.usecase.RemoveUploadFileImpl
import me.proton.core.drive.upload.domain.resolver.UriResolver
import me.proton.core.drive.upload.domain.usecase.RemoveUploadFile
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UploadModule {
    @Provides @IntoMap
    @StringKey(SCHEME_FILE)
    fun provideFileUriResolver(mimeTypeProvider: MimeTypeProvider): UriResolver =
        FileUriResolver(mimeTypeProvider)

    @Provides @IntoMap
    @StringKey(SCHEME_CONTENT)
    fun provideContentUriResolver(@ApplicationContext appContext: Context): UriResolver =
        ContentUriResolver(appContext)

    @Provides
    @Singleton
    fun provideRemoveUploadFile(linkUploadRepository: LinkUploadRepository): RemoveUploadFile =
        RemoveUploadFileImpl(linkUploadRepository)
}
