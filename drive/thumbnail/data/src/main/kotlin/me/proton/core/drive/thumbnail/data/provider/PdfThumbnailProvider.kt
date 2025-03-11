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
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.base.data.usecase.CompressBitmap
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.thumbnail.domain.usecase.CreateThumbnail
import javax.inject.Inject
import kotlin.math.roundToInt

class PdfThumbnailProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compressBitmap: CompressBitmap,
) : CreateThumbnail.Provider {

    override suspend fun getThumbnail(
        uriString: String,
        mimeType: String,
        maxWidth: Int,
        maxHeight: Int,
        maxSize: Bytes,
    ): ByteArray? {
        if (mimeType.toFileTypeCategory() != FileTypeCategory.Pdf) {
            return null
        }
        return try {
            val uri = Uri.parse(uriString)
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            PdfRenderer(fd).use { pdfRenderer ->
                val page = pdfRenderer.openPage(0) ?: return null
                page.use {
                    val ratio = page.computeRatio(maxWidth, maxHeight)
                    val bitmap = Bitmap.createBitmap(
                        (page.width / ratio).roundToInt(),
                        (page.height / ratio).roundToInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    compressBitmap(bitmap, maxSize)
                        .getOrNull(LogTag.THUMBNAIL, "Compressing bitmap failed")
                        .also {
                            bitmap.recycle()
                        }
                }
            }
        } catch (e: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: SecurityException) {
            // when pdf is password protected we do not generate thumbnail
            null
        }
    }

    companion object {
        @VisibleForTesting
        internal fun PdfRenderer.Page.computeRatio(reqWidth: Int, reqHeight: Int): Float {
            val widthRatio = width.toFloat() / reqWidth.toFloat()
            val heightRatio = height.toFloat() / reqHeight.toFloat()
            return if (widthRatio <= heightRatio) heightRatio else widthRatio
        }
    }
}
