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
package me.proton.core.drive.base.data.extension

import android.graphics.Bitmap
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.log.LogTag.THUMBNAIL
import me.proton.core.util.kotlin.CoreLogger
import java.io.ByteArrayOutputStream

@Suppress("MagicNumber")
fun Bitmap.compress(maxSize: Bytes): ByteArray? {
    ByteArrayOutputStream().use { stream ->
        listOf(95, 90, 85, 80, 70, 60, 50, 40, 30, 20, 15, 10, 5, 0).forEach { quality ->
            stream.reset()
            if (compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                stream.toByteArray().takeIf { bytes -> bytes.size <= maxSize.value.toInt() }
                    ?.let { bytes -> return bytes }
            } else {
                CoreLogger.i(THUMBNAIL, "Compression failed, quality: $quality")
                CoreLogger.d(THUMBNAIL, "Source: $width $height $byteCount")
            }
        }
    }
    return null
}
