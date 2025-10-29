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

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.decryptedFileName
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.file.base.domain.extension.moveTo
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import java.io.File
import javax.inject.Inject

class MoveFileIfExists @Inject constructor(
    private val getCacheFolder: GetCacheFolder,
    private val getPermanentFolder: GetPermanentFolder,
    private val getDriveLink: GetDriveLink,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
) {
    suspend operator fun invoke(fileId: FileId): Result<File?> = coRunCatching {
        invoke(getDriveLink(fileId).toResult().getOrThrow()).getOrThrow()
    }

    suspend operator fun invoke(driveLink: DriveLink.File): Result<File?> = coRunCatching {
        val userId = driveLink.userId
        val volumeId = driveLink.volumeId
        val fileId = driveLink.id
        val cacheFolder = getCacheFolder(userId, volumeId.id, driveLink.activeRevisionId)
        val permanentFolder = getPermanentFolder(userId, volumeId.id, driveLink.activeRevisionId)
        val cacheFile = File(cacheFolder, driveLink.decryptedFileName)
        val permanentFile = File(permanentFolder, driveLink.decryptedFileName)

        val markedAsOffline = isLinkOrAnyAncestorMarkedAsOffline(fileId)
        if (markedAsOffline) {
            if (cacheFile.exists()) {
                cacheFile.moveTo(permanentFile)
            }
            permanentFile
        } else {
            if (permanentFile.exists()) {
                permanentFile.moveTo(cacheFile)
            }
            cacheFile
        }
    }
}
