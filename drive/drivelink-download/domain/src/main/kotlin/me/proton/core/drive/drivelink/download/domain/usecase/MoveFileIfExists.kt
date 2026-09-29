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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.revisionUid
import me.proton.core.drive.file.base.domain.extension.moveTo
import me.proton.core.drive.link.domain.extension.decryptedFileName
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.drive.sdk.entity.RevisionUid
import java.io.File
import javax.inject.Inject

class MoveFileIfExists @Inject constructor(
    private val getCacheFolder: GetCacheFolder,
    private val getPermanentFolder: GetPermanentFolder,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
) {
    suspend operator fun invoke(driveLink: DriveLink.File): Result<File> =
        invoke(driveLink.id.userId, driveLink.revisionUid)

    suspend operator fun invoke(
        userId: UserId,
        revisionUid: RevisionUid,
    ): Result<File> = coRunCatching {
        move(
            userId = userId,
            revisionUid = revisionUid,
            markedAsOffline = isLinkOrAnyAncestorMarkedAsOffline(userId, revisionUid.nodeUid),
        )
    }

    private suspend fun move(
        userId: UserId,
        revisionUid: RevisionUid,
        markedAsOffline: Boolean,
    ): File {
        val cacheFolder = getCacheFolder(userId, revisionUid)
        val permanentFolder = getPermanentFolder(userId, revisionUid)
        val cacheFile = File(cacheFolder, revisionUid.decryptedFileName)
        val permanentFile = File(permanentFolder, revisionUid.decryptedFileName)

        return if (markedAsOffline) {
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
