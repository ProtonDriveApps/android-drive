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
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.domain.provider.MimeTypeProvider
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class CreateThumbnail @Inject constructor(
    private val providers: @JvmSuppressWildcards Set<Provider>,
    private val mimeTypeProvider: MimeTypeProvider,
    private val configurationProvider: ConfigurationProvider,
) {
    // The limit is with encryption but since we calculate the size prior to encryption we require 90% in order to
    // leave 10% of the size for the encryption. The same is done on the web client
    private val thumbnailMaxSize = configurationProvider.thumbnailMaxSize * 0.9f

    suspend operator fun invoke(
        uri: String,
        mimeType: String?,
        maxWidth: Int = configurationProvider.thumbnailMaxWidth,
        maxHeight: Int = configurationProvider.thumbnailMaxHeight,
        maxSize: Bytes = thumbnailMaxSize,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO
    ): Result<ByteArray?> = coRunCatching(coroutineContext) {
        (mimeType ?: mimeTypeProvider.getMimeTypeFromExtension(uri.extensionOrEmpty))?.let { type ->
            providers.firstNotNullOfOrNull { provider ->
                provider.getThumbnail(
                    uri,
                    type,
                    maxWidth,
                    maxHeight,
                    maxSize
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
