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
package me.proton.core.drive.documentsprovider.domain.usecase

import android.content.res.AssetFileDescriptor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.drivelink.domain.extension.getThumbnailId
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailCachedInputStream
import javax.inject.Inject

class GetDocumentThumbnail @Inject constructor(
    private val withDriveLinkFile: WithDriveLinkFile,
    private val getThumbnailCachedInputStream: GetThumbnailCachedInputStream,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
) {

    @Suppress("UNUSED_PARAMETER")
    suspend operator fun invoke(documentId: DocumentId, signal: CancellationSignal?): AssetFileDescriptor =
        withDriveLinkFile(documentId) { _, driveLink ->
            getThumbnailCachedInputStream(
                fileId = driveLink.id,
                volumeId = driveLink.volumeId,
                revisionId = driveLink.activeRevisionId,
                thumbnailId = requireNotNull(driveLink.getThumbnailId(ThumbnailType.DEFAULT)),
                inCacheFolder = !isLinkOrAnyAncestorMarkedAsOffline(driveLink.id)
            )
                .map { inputStream ->
                    val pipe = ParcelFileDescriptor.createPipe()
                    val readSide = pipe[0]
                    val writeSide = pipe[1]
                    ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { os -> inputStream.copyTo(os) }
                    AssetFileDescriptor(readSide, 0, 0)
                }.getOrThrow()
        }
}
