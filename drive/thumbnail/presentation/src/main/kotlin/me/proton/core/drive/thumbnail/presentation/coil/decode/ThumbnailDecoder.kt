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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.crypto.common.pgp.DecryptedData
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.crypto.domain.usecase.DecryptThumbnail
import me.proton.core.drive.thumbnail.presentation.coil.fetch.ThumbnailFetcher
import me.proton.core.util.kotlin.CoreLogger

@OptIn(ExperimentalCoilApi::class, ExperimentalCoroutinesApi::class)
@Suppress("unused")
class ThumbnailDecoder(
    private val context: Context,
    private val maxThumbnail: ConfigurationProvider.Thumbnail,
    private val decryptThumbnail: DecryptThumbnail,
    private val source: ImageSource,
    private val options: Options
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val metadata = source.metadata as ThumbnailFetcher.ThumbnailMetadata
        return decryptThumbnail(
            fileId = metadata.fileId,
            inputStream = source.source().inputStream(),
        ).map { decryptedData ->
            val thumbnailOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(
                    decryptedData.data,
                    0,
                    decryptedData.data.size,
                    this,
                )
            }
            val byteArray = if (thumbnailOptions.outWidth > maxThumbnail.maxWidth || thumbnailOptions.outHeight > maxThumbnail.maxHeight) {
                decryptedData.createDownscaledBitmap(
                    originalWidth = thumbnailOptions.outWidth,
                    originalHeight = thumbnailOptions.outHeight,
                )
            } else {
                BitmapFactory.decodeByteArray(
                    decryptedData.data,
                    0,
                    decryptedData.data.size,
                )
            }
            DecodeResult(
                drawable = byteArray.toDrawable(context.resources),
                isSampled = false
            )
        }.onFailure { error ->
            CoreLogger.w(LogTag.THUMBNAIL, error, "Unable to decrypt thumbnail fileId: ${metadata.fileId.id.logId()}")
        }.getOrThrow()
    }

    class Factory(
        private val context: Context,
        private val maxThumbnail: ConfigurationProvider.Thumbnail,
        private val decryptThumbnail: DecryptThumbnail
    ) : Decoder.Factory {
        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? = when {
            result.mimeType != ThumbnailFetcher.MIME_TYPE -> null
            else -> ThumbnailDecoder(
                context = context,
                maxThumbnail = maxThumbnail,
                decryptThumbnail = decryptThumbnail,
                source = result.source,
                options = options,
            )
        }
    }

    private fun DecryptedData.createDownscaledBitmap(originalWidth: Int, originalHeight: Int): Bitmap {
        val (targetWidth, targetHeight) = if (originalWidth >= originalHeight) {
            maxThumbnail.maxWidth to ((originalHeight / originalWidth.toFloat()) * maxThumbnail.maxWidth).toInt()
        } else {
            ((originalWidth / originalHeight.toFloat()) * maxThumbnail.maxHeight).toInt() to maxThumbnail.maxHeight
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inScaled = false
            inSampleSize = calculateInSampleSize(
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                originalWidth = originalWidth,
                originalHeight = originalHeight,
            )
        }
        val bitmap = BitmapFactory.decodeByteArray(
            data,
            0,
            data.size,
            decodeOptions,
        )
        return if (bitmap.width > maxThumbnail.maxWidth || bitmap.height > maxThumbnail.maxHeight) {
            bitmap.scale(targetWidth, targetHeight, true)
        } else {
            bitmap
        }
    }

    private fun calculateInSampleSize(
        targetWidth: Int,
        targetHeight: Int,
        originalWidth: Int,
        originalHeight: Int,
    ): Int {
        var inSampleSize = 1
        if (originalHeight > targetHeight || originalWidth > targetWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2

            while ((halfHeight / inSampleSize) >= targetHeight &&
                (halfWidth / inSampleSize) >= targetWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
