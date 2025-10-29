/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.download.domain.usecase

import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptLinkContent
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.decryptedFileName
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import javax.inject.Inject

class DecryptDownloadedBlocks @Inject constructor(
    private val getPermanentFolder: GetPermanentFolder,
    private val getCacheFolder: GetCacheFolder,
    private val decryptLinkContent: DecryptLinkContent,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
    private val deleteDownloadedBlocks: DeleteDownloadedBlocks,
) {
    suspend operator fun invoke(driveLink: DriveLink.File) = runCatching {
        val volumeId = driveLink.volumeId
        val fileId = driveLink.id
        val parentFolder = if (isLinkOrAnyAncestorMarkedAsOffline(fileId)) {
            getPermanentFolder(
                userId = driveLink.userId,
                volumeId = volumeId.id,
                revisionId = driveLink.activeRevisionId,
            )
        } else {
            getCacheFolder(
                userId = driveLink.userId,
                volumeId = volumeId.id,
                revisionId = driveLink.activeRevisionId,
            )
        }

        val targetFile = File(parentFolder, driveLink.decryptedFileName)

        decryptLinkContent(driveLink, targetFile, false).getOrThrow()

        CoreLogger.i(
            LogTag.GET_FILE,
            "File ${driveLink.id.id.logId()} was successfully decrypted!"
        )
        deleteDownloadedBlocks(driveLink).getOrThrow()
    }
}
