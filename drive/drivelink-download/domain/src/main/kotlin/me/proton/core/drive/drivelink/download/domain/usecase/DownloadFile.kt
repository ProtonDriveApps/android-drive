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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.download.domain.exception.InvalidBlocksException
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.file.base.domain.extension.verifyOrDelete
import me.proton.core.drive.file.base.domain.usecase.GetBlockFile
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailPermanentFile
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class DownloadFile @Inject constructor(
    private val setDownloadingAndGetRevision: SetDownloadingAndGetRevision,
    private val getBlockFile: GetBlockFile,
    private val downloadBlock: DownloadBlock,
    private val verifyDownloadedBlocks: VerifyDownloadedBlocks,
    private val configurationProvider: ConfigurationProvider,
    private val setDownloadState: SetDownloadState,
    private val getThumbnailPermanentFile: GetThumbnailPermanentFile,
    private val moveFileIfExists: MoveFileIfExists,
    private val getDriveLink: GetDriveLink,
    private val decryptDownloadedBlocks: DecryptDownloadedBlocks,
    private val deleteDownloadedBlocks: DeleteDownloadedBlocks,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
        isCancelled: () -> Boolean,
        progress: MutableStateFlow<Percentage>,
    ) = coRunCatching {
        val driveLink = getDriveLink(fileId).toResult().getOrThrow()
        val revision = setDownloadingAndGetRevision(fileId, revisionId).getOrThrow()
        getThumbnailPermanentFile(volumeId, driveLink.link, revisionId).getOrThrow()
        val file = moveFileIfExists(fileId).getOrThrow()
        if (file != null && file.exists()) {
            CoreLogger.d(LogTag.DOWNLOAD, "File already downloaded")
            setDownloadState(driveLink.link, DownloadState.Ready)
            return@coRunCatching
        }
        coroutineScope {
            val downloadProgress = MutableStateFlow(0L)
            val mutex = Mutex()

            val job = downloadProgress
                .onEach { downloaded ->
                    progress.value = Percentage(downloaded / revision.fileSize.toFloat())
                }
                .launchIn(this)
            val blocks = revision
                .blocks
                .blocksForDownload(
                    userId = fileId.userId,
                    volumeId = volumeId,
                    revisionId = revisionId,
                    progress = downloadProgress,
                    mutex = mutex,
                )
                .toMutableList()
            (1..configurationProvider.downloadBlocksInParallel)
                .map {
                    async(Dispatchers.IO) {
                        blocks.doDownloadBlock(
                            userId = fileId.userId,
                            volumeId = volumeId,
                            revisionId = revisionId,
                            progress = downloadProgress,
                            mutex = mutex,
                            coroutineScope = this,
                            isCancelled = isCancelled,
                        )
                    }
                }.awaitAll()
            job.cancel()
        }
        CoreLogger.d(LogTag.DOWNLOAD, "Verifying downloaded blocks")
        val verified = verifyDownloadedBlocks(volumeId, fileId, revision)
            .onSuccess { isVerified ->
                if (!isVerified) {
                    deleteDownloadedBlocks(driveLink).getOrThrow()
                    throw InvalidBlocksException()
                }
            }
            .getOrThrow()
        CoreLogger.d(LogTag.DOWNLOAD, "Downloaded blocks verification: isSuccessful=$verified")
        decryptDownloadedBlocks(driveLink).onSuccess {
            CoreLogger.i(LogTag.DOWNLOAD, "File ${driveLink.id.id.logId()} was successfully decrypted!")
            deleteDownloadedBlocks(driveLink).getOrThrow()
        }.onFailure { error ->
            CoreLogger.e(LogTag.DOWNLOAD, error,"There was an error decrypting file ${driveLink.id.id.logId()}")
        }.getOrThrow()
    }.onFailure { setDownloadState(fileId, DownloadState.Error) }

    private suspend fun List<Block>.blocksForDownload(
        userId: UserId,
        volumeId: VolumeId,
        revisionId: String,
        progress: MutableStateFlow<Long>,
        mutex: Mutex,
    ): List<Block> = mapNotNull { block ->
        val blockFile = getBlockFile(userId, volumeId, revisionId, block)
        if (blockFile != null) {
            if (blockFile.verifyOrDelete(block.hashSha256)) {
                CoreLogger.d(
                    LogTag.DOWNLOAD,
                    "block exists and is valid, skip it ${blockFile.name} ${blockFile.length()}"
                )
                mutex.withLock {
                    progress.value += blockFile.length()
                }
                null
            } else {
                block
            }
        } else {
            block
        }
    }

    private suspend fun MutableList<Block>.removeFirstOrNull(
        mutex: Mutex,
    ): Block? = mutex.withLock {
        if (isNotEmpty()) {
            removeAt(0)
        } else {
            null
        }
    }

    private suspend fun MutableList<Block>.doDownloadBlock(
        userId: UserId,
        volumeId: VolumeId,
        revisionId: String,
        progress: MutableStateFlow<Long>,
        mutex: Mutex,
        coroutineScope: CoroutineScope,
        isCancelled: () -> Boolean,
    ) {
        var block = removeFirstOrNull(mutex)
        while (block != null) {
            val blockDownloadProgress = MutableStateFlow(0L)
            val blockJob = blockDownloadProgress
                .mapWithPrevious { previous, current ->
                    previous to current
                }
                .onEach { (previous, currentProgress) ->
                    mutex.withLock {
                        progress.value += (currentProgress - (previous ?: 0))
                    }
                }
                .launchIn(coroutineScope)
            CoreLogger.d(LogTag.DOWNLOAD, "Downloading block ${block.index}")
            downloadBlock(
                userId = userId,
                volumeId = volumeId,
                revisionId = revisionId,
                block = block,
                downloadingProgress = blockDownloadProgress,
                isCancelled = isCancelled,
                coroutineContext = coroutineScope.coroutineContext,
            ).getOrThrow()
            blockJob.cancel()
            CoreLogger.d(LogTag.DOWNLOAD, "Block ${block.index} downloaded!")
            block = removeFirstOrNull(mutex)
        }
    }
}
