/*
 * Copyright (c) 2021-2023 Proton AG.
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

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.Revision
import me.proton.core.drive.file.base.domain.usecase.GetBlockFile
import me.proton.core.drive.file.base.domain.usecase.GetRevision
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.RemoveDownloadState
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class VerifyDownloadedBlocks @Inject constructor(
    private val getRevision: GetRevision,
    private val setDownloadState: SetDownloadState,
    private val removeDownloadState: RemoveDownloadState,
    private val getBlockFile: GetBlockFile,
) {
    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
    ): Result<Boolean> = coRunCatching {
        val revision = getRevision(fileId, revisionId)
            .onFailure {
                setDownloadState(fileId, DownloadState.Error)
            }
            .getOrThrow()
        invoke(volumeId, fileId, revision).getOrThrow()
    }

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        revision: Revision,
    ): Result<Boolean> = coRunCatching {
        try {
            val blocks = revision.blocks
                .map { block ->
                    val file = getBlockFile(fileId.userId, volumeId, revision.id, block)
                    block to requireNotNull(file) { "Cannot get block to verify download" }
                }
                .map { (block, file) -> block.copy(url = file.path) }
            setDownloadState(
                linkId = fileId,
                downloadState = DownloadState.Downloaded(
                    manifestSignature = revision.manifestSignature,
                    signatureAddress = revision.signatureAddress,
                ),
                blocks = blocks,
            )
            true
        } catch (e: Throwable) {
            removeDownloadState(fileId)
            false
        }
    }
}
