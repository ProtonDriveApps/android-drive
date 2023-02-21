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
package me.proton.core.drive.base.data.provider

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.entity.MediaResolution
import me.proton.core.drive.base.domain.provider.MediaResolutionProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.entity.FileTypeCategory
import me.proton.core.drive.base.presentation.entity.toFileTypeCategory
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ExifImageResolutionProvider @Inject constructor(
    @ApplicationContext val appContext: Context,
    private val coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
) : MediaResolutionProvider {

    override suspend fun getResolution(
        uriString: String,
        mimeType: String,
    ): MediaResolution? = coRunCatching(coroutineContext) {
        takeIf { mimeType.toFileTypeCategory() == FileTypeCategory.Image }?.let {
            appContext.contentResolver.openInputStream(Uri.parse(uriString)).use { inputStream ->
                inputStream?.let {
                    ExifInterface(inputStream).run {
                        val orientation = getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED
                        )
                        val (width, height) = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90,
                            ExifInterface.ORIENTATION_ROTATE_270 -> {
                                getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0) to
                                        getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                            }
                            else -> getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0) to
                                    getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                        }
                        takeIf { width > 0 && height > 0 }?.let {
                            MediaResolution(width.toLong(), height.toLong())
                        }
                    }
                }
            }
        }
    }.getOrNull()
}
