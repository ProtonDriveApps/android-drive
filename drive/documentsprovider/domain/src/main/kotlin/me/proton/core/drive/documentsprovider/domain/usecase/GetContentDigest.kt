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

package me.proton.core.drive.documentsprovider.domain.usecase

import me.proton.core.drive.base.domain.extension.getHexMessageDigest
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkXAttr
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.file.base.domain.entity.XAttr
import me.proton.core.drive.file.base.domain.extension.toXAttr
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject

class GetContentDigest @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val openDocument: OpenDocument,
    private val getDriveLink: GetDriveLink,
    private val decryptLinkXAttr: DecryptLinkXAttr
) {

    suspend operator fun invoke(
        fileId: FileId,
        fallbackToRecalculateFromFile: Boolean = false,
    ): Result<String> = coRunCatching {
        invoke(
            driveLink = getDriveLink(fileId = fileId).toResult().getOrThrow(),
            fallbackToRecalculateFromFile = fallbackToRecalculateFromFile,
        ).getOrThrow()
    }

    suspend operator fun invoke(
        driveLink: DriveLink.File,
        fallbackToRecalculateFromFile: Boolean = false,
    ): Result<String> = coRunCatching {
        getContentDigestFromXAttr(driveLink)
            .recoverCatching {
                if (fallbackToRecalculateFromFile) {
                    getContentDigestFromFile(driveLink).getOrThrow()
                } else {
                    throw it
                }
            }
            .getOrThrow()
    }

    private suspend fun getContentDigestFromXAttr(
        driveLink: DriveLink.File
    ): Result<String> = coRunCatching {
        requireNotNull(driveLink.xAttr) { "No XAttr found" }
        getContentDigestFromXAttr(
            xAttr = decryptLinkXAttr(driveLink).getOrThrow().text.toXAttr().getOrThrow()
        ).getOrThrow()
    }

    private fun getContentDigestFromXAttr(
        xAttr: XAttr,
    ): Result<String> = coRunCatching {
        val digests = requireNotNull(xAttr.common.digests) { "XAttr does not contain digests" }
        requireNotNull(digests[configurationProvider.contentDigestAlgorithm]) {
            "Digests does not contain content digest algorithm ${configurationProvider.contentDigestAlgorithm}"
        }
    }

    private suspend fun getContentDigestFromFile(
        driveLink: DriveLink.File
    ): Result<String> = coRunCatching {
        val pfd = openDocument(
            documentId = DocumentId(driveLink.userId, driveLink.id),
            mode = "r",
            signal = null,
        )
        FileInputStream(pfd.fileDescriptor).use { inputStream ->
            getContentDigestFromInputStream(inputStream).getOrThrow()
        }
    }

    private suspend fun getContentDigestFromInputStream(
        inputStream: InputStream
    ): Result<String> = coRunCatching {
        requireNotNull(
            inputStream.getHexMessageDigest(configurationProvider.contentDigestAlgorithm)
        ) { "No digest found" }
    }
}
