/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.upload.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toHex
import me.proton.core.drive.base.domain.log.LogTag.UploadTag.logTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.RawBlock
import me.proton.core.drive.linkupload.domain.entity.UploadDigests
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.InsertRawUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.RemoveAllRawUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.UpdateDigests
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.upload.domain.extension.injectMessageDigests
import me.proton.core.drive.upload.domain.extension.saveToBlocks
import me.proton.core.drive.upload.domain.outputstream.MultipleFileOutputStream
import me.proton.core.drive.upload.domain.resolver.UriResolver
import me.proton.core.util.kotlin.takeIfNotEmpty
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class SplitUriToBlocks @Inject constructor(
    private val updateUploadState: UpdateUploadState,
    private val uriResolver: UriResolver,
    private val getBlockFolder: GetBlockFolder,
    private val updateDigests: UpdateDigests,
    private val getSplitRawBlocks: GetSplitRawBlocks,
    private val removeAllRawBlocks: RemoveAllRawUploadBlocks,
    private val insertRawBlocks: InsertRawUploadBlocks,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        uriString: String,
        isCancelled: () -> Boolean = { false },
        coroutineContext: CoroutineContext = Dispatchers.IO,
    ): Result<UploadFileLink> = coRunCatching(coroutineContext) {
        with (uploadFileLink) {
            updateUploadState(id, UploadState.SPLITTING_URI_TO_BLOCKS).getOrThrow()
            try {
                val destinationFolder = getBlockFolder(uploadFileLink.userId, uploadFileLink).getOrThrow()
                val splitRawBlocks = getSplitRawBlocks(
                    uploadFileLinkId = uploadFileLink.id,
                    destinationFolder = destinationFolder,
                )
                    .onFailure {
                        removeAllRawBlocks(uploadFileLink.id).getOrNull(uploadFileLink.id.logTag())
                        destinationFolder.listFiles()?.forEach { file ->
                            file.delete()
                        }
                    }
                    .getOrThrow()
                    .map { (_, block) -> block }
                val (_, digests) = uploadFileLink.splitUriToBlocks(uriString, splitRawBlocks, isCancelled)
                updateDigests(
                    uploadFileLinkId = uploadFileLink.id,
                    digests = digests
                ).getOrThrow()
            } finally {
                updateUploadState(id, UploadState.IDLE)
            }
        }
    }

    private suspend fun UploadFileLink.splitUriToBlocks(
        uriString: String,
        splitRawBlocks: List<File>,
        isCancelled: () -> Boolean = { false },
    ): Pair<List<File>, UploadDigests> =
        uriResolver.useInputStream(uriString) { inputStream ->
            val (digestsInputStream, messageDigests) = inputStream.injectMessageDigests(
                algorithms = configurationProvider.digestAlgorithms,
            )
            digestsInputStream.skip(splitRawBlocks.totalLength())
            val listener = MultipleFileOutputStreamListener(isCancelled)
            val files = try {
                digestsInputStream.saveToBlocks(
                    destinationFolder = getBlockFolder(userId, this).getOrThrow(),
                    blockMaxSize = configurationProvider.blockMaxSize,
                    listener = listener,
                )
            } finally {
                listener.processFiles(id, isCancelled, indexOffset = splitRawBlocks.size)
            }
            val emptyFiles = files.filter { file -> file.length() == 0L }
            require(emptyFiles.isEmpty()) {
                "Split should not generate empty file: " +
                        "\n${emptyFiles.size}/${files.size}" +
                        "\n${emptyFiles.map { file -> file.name }}"
            }
            val uploadDigests = messageDigests
                .associate { messageDigest ->
                    messageDigest.algorithm to messageDigest.digest().toHex()
                }.let(::UploadDigests)
            files to uploadDigests
        } ?: (emptyList<File>() to UploadDigests())

    private fun List<File>.totalLength(): Long =
        takeIfNotEmpty()
            ?.sumOf { file -> file.length() }
            ?: 0L

    class MultipleFileOutputStreamListener(
        private val isCancelled: () -> Boolean = { false },
    ) : MultipleFileOutputStream.Listener {
        internal val createdFiles = mutableSetOf<File>()
        internal val closedFiles = mutableSetOf<File>()

        override fun onAddedNewFile(file: File, previousFile: File?) {
            if (previousFile != null) {
                closedFiles.add(previousFile)
            }
            createdFiles.add(file)
            if (isCancelled()) {
                throw CancellationException(RuntimeException("Cancelled by the caller"))
            }
        }
    }

    private suspend fun MultipleFileOutputStreamListener.processFiles(
        uploadFileLinkId: Long,
        isCancelled: () -> Boolean,
        indexOffset: Int = 0,
    ) = withContext(NonCancellable) {
        insertRawBlocks(
            closedFiles
                .mapIndexed { index, file ->
                    RawBlock(
                        uploadFileLinkId = uploadFileLinkId,
                        index = indexOffset + index.toLong(),
                        name = file.name,
                    )
                }
                .toSet()
        ).getOrThrow()
        val leftovers = createdFiles
            .mapIndexedNotNull { index, file ->
                if (closedFiles.contains(file).not()) {
                    file to RawBlock(
                        uploadFileLinkId = uploadFileLinkId,
                        index = indexOffset + index.toLong(),
                        name = file.name,
                    )
                } else {
                    null
                }
            }
        if (isCancelled()) {
            leftovers.forEach { (file, _) ->
                file.delete()
            }
        } else {
            insertRawBlocks(leftovers.map { (_, rawBlock) -> rawBlock }.toSet()).getOrThrow()
        }
    }
}
