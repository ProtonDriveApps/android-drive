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
package me.proton.core.drive.thumbnail.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.extensionOrEmpty
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.provider.MimeTypeProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class CreateThumbnail @Inject constructor(
    private val providers: @JvmSuppressWildcards Set<Provider>,
    private val mimeTypeProvider: MimeTypeProvider,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(
        uri: String,
        mimeType: String?,
        type: ThumbnailType,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ) : Result<ByteArray?> = coRunCatching(coroutineContext) {
        val thumbnail = when (type) {
            ThumbnailType.DEFAULT -> configurationProvider.thumbnailDefault
            ThumbnailType.PHOTO -> configurationProvider.thumbnailPhoto
        }
        invoke(
            uri = uri,
            mimeType = mimeType,
            maxWidth = thumbnail.maxWidth,
            maxHeight = thumbnail.maxHeight,
            maxSize = thumbnail.maxSize,
            coroutineContext = coroutineContext,
        ).getOrThrow()
    }

    suspend operator fun invoke(
        uri: String,
        mimeType: String?,
        maxWidth: Int,
        maxHeight: Int,
        maxSize: Bytes,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO
    ): Result<ByteArray?> = coRunCatching(coroutineContext) {
        // The limit is with encryption but since we calculate the size prior to encryption we require 90% in order to
        // leave 10% of the size for the encryption. The same is done on the web client
        val thumbnailMaxSize = maxSize * 0.9f
        (mimeType ?: mimeTypeProvider.getMimeTypeFromExtension(uri.extensionOrEmpty))?.let { type ->
            providers.firstNotNullOfOrNull { provider ->
                provider.getThumbnail(
                    uri,
                    type,
                    maxWidth,
                    maxHeight,
                    thumbnailMaxSize,
                )
            }
        }
    }

    fun interface Provider {
        suspend fun getThumbnail(
            uriString: String,
            mimeType: String,
            maxWidth: Int,
            maxHeight: Int,
            maxSize: Bytes,
        ): ByteArray?
    }
}
