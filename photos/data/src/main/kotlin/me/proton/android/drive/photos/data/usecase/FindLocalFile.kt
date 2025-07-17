/*
 * Copyright (c) 2024-2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.data.usecase

import me.proton.core.drive.base.domain.extension.getHexMessageDigest
import me.proton.core.drive.base.domain.extension.requireIsInstance
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkName
import me.proton.core.drive.crypto.domain.usecase.file.GetContentHash
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.photo.domain.usecase.ScanFileByName
import me.proton.core.drive.upload.domain.resolver.UriResolver
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class FindLocalFile @Inject constructor(
    private val scanFileByName: ScanFileByName,
    private val getLink: GetLink,
    private val uriResolver: UriResolver,
    private val getNodeKey: GetNodeKey,
    private val decryptLinkName: DecryptLinkName,
    private val getContentHash: GetContentHash,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
    ): Result<String?> = coRunCatching {
        val file = getLink(fileId).toResult().getOrThrow()
        val folderId = requireIsInstance<FolderId>(file.parentId) { "Parent must be a folder" }
        val parentNodeKey = getNodeKey(folderId).getOrThrow()
        coRunCatching {
            scanFileByName(
                decryptLinkName(file).getOrThrow().text,
            ).firstOrNull { uriString ->
                uriString.checkContentHash(
                    contentHash = requireNotNull(file.photoContentHash) { "Content hash should not be null" },
                    folderId = folderId,
                    folderKey = parentNodeKey,
                )
            }
        }.getOrThrow()
    }

    private suspend fun String.checkContentHash(
        contentHash: String?,
        folderId: FolderId,
        folderKey: Key.Node,
    ) = uriResolver.useInputStream(this) { inputStream ->
        inputStream.getHexMessageDigest(configurationProvider.contentDigestAlgorithm)
            ?.let { hexMessageDigest ->
                val localFileContentHash = this@FindLocalFile.getContentHash(
                    folderId = folderId,
                    folderKey = folderKey,
                    input = hexMessageDigest,
                ).getOrThrow()
                localFileContentHash == contentHash
            }
    } ?: false
}
