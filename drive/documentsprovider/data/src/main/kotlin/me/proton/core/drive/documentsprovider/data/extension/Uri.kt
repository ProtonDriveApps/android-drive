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
package me.proton.core.drive.documentsprovider.data.extension

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink

fun Uri.exportTo(
    contentResolver: ContentResolver,
    destinationUri: Uri,
): Result<Unit> = coRunCatching {
    contentResolver.openInputStream(this)!!.use { inputStream ->
        contentResolver.openOutputStream(destinationUri)!!.use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Suppress("unused")
fun Uri.exportToMediaStoreDownloads(
    contentResolver: ContentResolver,
    driveLink: DriveLink.File,
): Result<Unit> = coRunCatching {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, driveLink.name)
        put(MediaStore.Downloads.MIME_TYPE, driveLink.mimeType)
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    contentResolver.insert(
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        values,
    )!!.let { contentUri ->
        contentResolver.openInputStream(this)!!.use { inputStream ->
            contentResolver.openOutputStream(contentUri)!!.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(contentUri, values, null, null)
        }
    }
}
