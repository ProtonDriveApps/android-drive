/*
 * Copyright (c) 2023 Proton AG.
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
package me.proton.core.drive.thumbnail.data.provider

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.presentation.entity.FileTypeCategory
import me.proton.core.drive.base.presentation.entity.toFileTypeCategory
import me.proton.core.drive.base.presentation.extension.compress
import me.proton.core.drive.thumbnail.domain.usecase.CreateThumbnail
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
abstract class FileThumbnailProvider(
    private val context: Context,
    private val category: FileTypeCategory,
    private val prefix: String,
) : CreateThumbnail.Provider {

    override suspend fun getThumbnail(
        uriString: String,
        mimeType: String,
        maxWidth: Int,
        maxHeight: Int,
        maxSize: Bytes,
    ): ByteArray? {
        if (mimeType.toFileTypeCategory() != category) {
            return null
        }
        var tmpFile: File? = null
        return try {
            tmpFile = File.createTempFile(prefix, "", context.cacheDir)
            val uri = Uri.parse(uriString)
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            val bitmap = fileToBitmap(tmpFile, Size(maxWidth, maxHeight))
            bitmap?.compress(maxSize)?.also {
                bitmap.recycle()
            }
        } catch (e: OutOfMemoryError) {
            CoreLogger.d(LogTag.THUMBNAIL, e, "Create file thumbnail failed")
            System.gc()
            null
        } catch (e: IllegalArgumentException) {
            CoreLogger.d(LogTag.THUMBNAIL, e, "Create file thumbnail failed")
            null
        } catch (e: IOException) {
            CoreLogger.d(LogTag.THUMBNAIL, e, "Create file thumbnail failed")
            null
        } finally {
            tmpFile?.delete()
        }
    }

    abstract fun fileToBitmap(file: File, size: Size): Bitmap?
}
