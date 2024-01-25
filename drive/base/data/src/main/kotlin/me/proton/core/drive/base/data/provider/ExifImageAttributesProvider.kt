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

package me.proton.core.drive.base.data.provider

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.data.extension.cameraExifTags
import me.proton.core.drive.base.data.extension.creationDateTime
import me.proton.core.drive.base.data.extension.creationDateTimeDigitized
import me.proton.core.drive.base.data.extension.creationDateTimeOriginal
import me.proton.core.drive.base.data.extension.location
import me.proton.core.drive.base.data.extension.mediaResolution
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.ImageAttributes
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.provider.FileAttributesProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ExifImageAttributesProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
) : FileAttributesProvider {

    override suspend fun getFileAttributes(
        uriString: String,
        mimeType: String,
    ): ImageAttributes? = coRunCatching(coroutineContext) {
        takeIf { mimeType.toFileTypeCategory() == FileTypeCategory.Image }?.let {
            appContext.contentResolver.openInputStream(Uri.parse(uriString)).use { inputStream ->
                inputStream?.let {
                    ExifInterface(inputStream).run {
                        ImageAttributes(
                            cameraExifTags = cameraExifTags,
                            creationDateTime = creationDateTimeOriginal ?: creationDateTimeDigitized ?: creationDateTime,
                            resolution = mediaResolution,
                            location = location,
                        )
                    }
                }
            }
        }
    }.getOrNull()
}
