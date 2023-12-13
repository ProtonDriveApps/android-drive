/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.thumbnail.presentation.coil.decode

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.crypto.domain.usecase.DecryptThumbnail
import me.proton.core.drive.thumbnail.presentation.coil.fetch.ThumbnailFetcher
import me.proton.core.util.kotlin.CoreLogger

@OptIn(ExperimentalCoilApi::class, ExperimentalCoroutinesApi::class)
class ThumbnailDecoder(
    private val decryptThumbnail: DecryptThumbnail,
    private val source: ImageSource,
    private val options: Options
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val metadata = source.metadata as ThumbnailFetcher.ThumbnailMetadata
        return decryptThumbnail(
            fileId = metadata.fileId,
            inputStream = source.source().inputStream(),
        ).map { decryptedData ->
            val byteArray = BitmapFactory.decodeByteArray(decryptedData.data, 0, decryptedData.data.size)
            DecodeResult(
                drawable = BitmapDrawable(null, byteArray),
                isSampled = false
            )
        }.onFailure { error ->
            CoreLogger.d(LogTag.THUMBNAIL, error, "Unable to decrypt thumbnail fileId: ${metadata.fileId}")
        }.getOrThrow()
    }

    class Factory(private val decryptThumbnail: DecryptThumbnail) : Decoder.Factory {
        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? = when {
            result.mimeType != ThumbnailFetcher.MIME_TYPE -> null
            else -> ThumbnailDecoder(decryptThumbnail, result.source, options)
        }
    }
}
