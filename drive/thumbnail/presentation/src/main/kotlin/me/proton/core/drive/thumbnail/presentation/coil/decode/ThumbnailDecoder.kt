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
import coil.bitmap.BitmapPool
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.Options
import coil.size.Size
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.crypto.domain.usecase.DecryptThumbnail
import me.proton.core.drive.thumbnail.presentation.coil.fetch.ThumbnailFetcher
import okio.BufferedSource
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ThumbnailDecoder @Inject constructor(
    private val decryptThumbnail: DecryptThumbnail,
) : Decoder {

    override suspend fun decode(pool: BitmapPool, source: BufferedSource, size: Size, options: Options): DecodeResult {
        require(source is ThumbnailFetcher.Source)
        return decryptThumbnail(
            fileId = source.thumbnailVO.fileId,
            inputStream = source.inputStream(),
        ).map { decryptedData ->
            DecodeResult(
                drawable = BitmapDrawable(
                    null,
                    BitmapFactory.decodeByteArray(decryptedData.data, 0, decryptedData.data.size)
                ),
                isSampled = false
            )
        }.getOrThrow()
    }

    override fun handles(source: BufferedSource, mimeType: String?) = source is ThumbnailFetcher.Source
}
