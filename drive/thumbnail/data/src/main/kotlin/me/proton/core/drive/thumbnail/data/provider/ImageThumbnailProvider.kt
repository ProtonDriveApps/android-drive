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
package me.proton.core.drive.thumbnail.data.provider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.applyCanvas
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.base.data.extension.compress
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.thumbnail.domain.usecase.CreateThumbnail
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.roundToInt

@Suppress("BlockingMethodInNonBlockingContext")
class ImageThumbnailProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : CreateThumbnail.Provider {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override suspend fun getThumbnail(
        uriString: String,
        mimeType: String,
        maxWidth: Int,
        maxHeight: Int,
        maxSize: Bytes,
    ): ByteArray? {
        if (mimeType.toFileTypeCategory() != FileTypeCategory.Image) {
            return null
        }
        return try {
            BitmapFactory.Options().run {
                inJustDecodeBounds = true
                inPreferredConfig = Bitmap.Config.RGB_565

                val uri = Uri.parse(uriString)
                context.contentResolver.openFileDescriptor(uri, "r").use { parcelFileDescriptor ->
                    parcelFileDescriptor?.fileDescriptor?.let { fd ->
                        BitmapFactory.decodeFileDescriptor(fd, null, this)
                        if (outMimeType == null) {
                            return null
                        }
                        inJustDecodeBounds = false
                        val (rotation, isFlipped) = uri.getRotation(outMimeType)
                        inSampleSize = calculateInSampleSize(maxWidth, maxHeight, rotation)
                        val bitmap = BitmapFactory.decodeFileDescriptor(fd, null, this)?.rotate(rotation, isFlipped)
                        bitmap?.compress(maxSize)?.also {
                            bitmap.recycle()
                        }
                    } ?: return null
                }
            }
        } catch (e: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun Uri.getRotation(mimeType: String?): Pair<Int, Boolean> =
        if (mimeType !in SUPPORTED_EXIF_MIME_TYPES) {
            0 to false
        } else context.contentResolver.openFileDescriptor(this, "r").use { parcelFileDescriptor ->
            parcelFileDescriptor?.fileDescriptor?.let { fd ->
                ExifInterface(fd).run { rotationDegrees to isFlipped }
            } ?: (0 to false)
        }

    private fun Bitmap.rotate(rotation: Int, isFlipped: Boolean): Bitmap {
        val (width, height) = getSizeForRotation(width, height, rotation)
        val centerX = this@rotate.width / 2f
        val centerY = this@rotate.height / 2f
        val matrix = Matrix().apply {
            if (isFlipped) {
                postScale(-1f, 1f, centerX, centerY)
            }
            if (rotation > 0) {
                postRotate(rotation.toFloat(), centerX, centerY)
            }
            val rect = RectF(0f, 0f, this@rotate.width.toFloat(), this@rotate.height.toFloat())
            mapRect(rect)
            if (rect.left != 0f || rect.top != 0f) {
                postTranslate(-rect.left, -rect.top)
            }
        }
        return Bitmap.createBitmap(width, height, config).applyCanvas {
            drawBitmap(this@rotate, matrix, paint)
        }.also {
            recycle()
        }
    }

    companion object {

        private const val ROTATE_90 = 90
        private const val ROTATE_270 = 270
        private const val MIME_TYPE_JPEG = "image/jpeg"
        private const val MIME_TYPE_WEBP = "image/webp"
        private const val MIME_TYPE_HEIC = "image/heic"
        private const val MIME_TYPE_HEIF = "image/heif"

        private val SUPPORTED_EXIF_MIME_TYPES = arrayOf(MIME_TYPE_JPEG, MIME_TYPE_WEBP, MIME_TYPE_HEIC, MIME_TYPE_HEIF)

        private fun getSizeForRotation(width: Int, height: Int, rotation: Int) = when (rotation) {
            ROTATE_90, ROTATE_270 -> height to width
            else -> width to height
        }

        @VisibleForTesting
        internal fun BitmapFactory.Options.calculateInSampleSize(reqWidth: Int, reqHeight: Int, rotation: Int): Int {
            val (width, height) = getSizeForRotation(outWidth, outHeight, rotation)
            val widthRatio = ceil(width.toFloat() / reqWidth.toFloat()).roundToInt()
            val heightRatio = ceil(height.toFloat() / reqHeight.toFloat()).roundToInt()
            return if (widthRatio <= heightRatio) heightRatio else widthRatio
        }
    }
}
