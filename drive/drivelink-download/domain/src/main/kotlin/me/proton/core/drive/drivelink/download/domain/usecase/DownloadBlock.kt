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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.exception.VerificationException
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.file.base.domain.extension.verifyOrDelete
import me.proton.core.drive.file.base.domain.usecase.DownloadUrl
import me.proton.core.drive.volume.domain.entity.VolumeId
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DownloadBlock @Inject constructor(
    private val downloadUrl: DownloadUrl,
    private val getCacheFolder: GetCacheFolder,
) {
    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        revisionId: String,
        block: Block,
        downloadingProgress: MutableStateFlow<Long>,
        isCancelled: () -> Boolean,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO
    ): Result<File> = coRunCatching(coroutineContext) {
        require(revisionId.isNotBlank()) { "Valid revision ID must be provided" }
        val file = File(getCacheFolder(userId, volumeId.id, revisionId), block.index.toString())
        downloadUrl(
            userId = userId,
            url = block.url,
            destination = file,
            progress = downloadingProgress,
            isCancelled = isCancelled,
        )
            .onSuccess { blockFile ->
                if (!blockFile.verifyOrDelete(block.hashSha256, coroutineContext)) {
                    throw VerificationException("Hash from downloaded block does not match expected hash")
                }
            }
            .onFailure {
                file.delete()
            }
            .getOrThrow()
    }
}
