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
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.entity.MediaResolution
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.MediaResolutionProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.entity.FileTypeCategory
import me.proton.core.drive.base.presentation.entity.toFileTypeCategory
import me.proton.core.drive.base.presentation.extension.log
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class MetadataRetrieverVideoResolutionProvider @Inject constructor(
    @ApplicationContext val appContext: Context,
    private val coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
) : MediaResolutionProvider {

    override suspend fun getResolution(
        uriString: String,
        mimeType: String,
    ): MediaResolution? = coRunCatching(coroutineContext) {
        takeIf { mimeType.toFileTypeCategory() == FileTypeCategory.Video }?.let {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(appContext, Uri.parse(uriString))
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                takeIf { width > 0 && height > 0 }?.let {
                    MediaResolution(width.toLong(), height.toLong())
                }
            } catch (e: Exception) {
                e.log(LogTag.MEDIA, "MediaMetadataRetriever failed to get resolution")
                null
            }
        }
    }.getOrNull()
}
