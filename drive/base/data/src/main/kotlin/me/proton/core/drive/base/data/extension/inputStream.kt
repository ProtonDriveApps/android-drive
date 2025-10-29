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

package me.proton.core.drive.base.data.extension

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.MediaStore
import me.proton.core.drive.base.domain.util.coRunCatching
import java.io.InputStream

fun InputStream.exportToMediaStoreDownloads(
    contentResolver: ContentResolver,
    filename: String,
    mimeType: String,
) = coRunCatching {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, filename)
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    contentResolver.insert(
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        values,
    )!!.let { contentUri ->
        this.use<InputStream, Unit> { inputStream ->
            contentResolver.openOutputStream(contentUri)!!.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(contentUri, values, null, null)
        }
    }
}
