/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.backup.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BackupDuplicateRepository
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.domain.extension.getHexMessageDigest
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.GetContentHash
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.upload.domain.resolver.UriResolver
import me.proton.core.util.kotlin.CoreLogger
import java.io.FileNotFoundException
import javax.inject.Inject

class CheckDuplicates @Inject constructor(
    private val backupFileRepository: BackupFileRepository,
    private val deleteFile: DeleteFile,
    private val backupDuplicateRepository: BackupDuplicateRepository,
    private val uriResolver: UriResolver,
    private val getNodeKey: GetNodeKey,
    private val getContentHash: GetContentHash,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(
        userId: UserId,
        backupFolder: BackupFolder,
        pageSize: Int = configurationProvider.dbPageSize,
    ) = coRunCatching {
        val folderId = backupFolder.folderId
        val nodeKey = getNodeKey(folderId).getOrThrow()
        do {
            val backupFiles = backupFileRepository.getAllInFolderWithState(
                folderId = folderId,
                bucketId = backupFolder.bucketId,
                state = BackupFileState.POSSIBLE_DUPLICATE,
                count = pageSize,
            )
            CoreLogger.d(BACKUP, "Checking ${backupFiles.size} files for duplicates")
            backupFiles.filter { backupFile ->
                backupFile.hash != null
            }.map { backupFile ->
                val hash = requireNotNull(backupFile.hash)
                val duplicates = backupDuplicateRepository.getAllByHash(
                    parentId = backupFolder.folderId,
                    hash = hash
                )
                val contentHash = backupFile.getContentHash(folderId, nodeKey)
                    .onFailure { error ->
                        if (error is FileNotFoundException) {
                            backupFile.onFileNotFound()
                        } else {
                            CoreLogger.e(
                                BACKUP,
                                error,
                                "Cannot get content has for ${backupFile.uriString}"
                            )
                        }
                    }.getOrNull()
                val duplicate = duplicates.find { duplicate ->
                    duplicate.contentHash == contentHash
                }
                if (duplicate == null) {
                    markAs(backupFile, BackupFileState.READY)
                } else {
                    markAs(backupFile, BackupFileState.DUPLICATED) {
                        backupDuplicateRepository.deleteDuplicates(listOf(duplicate))
                    }
                }
            }.groupingBy { state -> state }.eachCount().forEach { (state, count) ->
                CoreLogger.d(BACKUP, "Marking $count files as $state")
            }
        } while (backupFiles.size == pageSize)
    }

    private suspend fun markAs(
        backupFile: BackupFile,
        backupFileState: BackupFileState,
        block: suspend () -> Unit = {},
    ): BackupFileState {
        backupFileRepository.markAs(
            folderId = backupFile.folderId,
            uriString = backupFile.uriString,
            backupFileState = backupFileState,
        )
        block()
        return backupFileState
    }

    private suspend fun BackupFile.onFileNotFound() {
        val uriString = uriString
        CoreLogger.d(BACKUP, "Deleting file not found: $uriString")
        deleteFile(folderId, uriString).onFailure { error ->
            CoreLogger.e(BACKUP, error, "Cannot delete file: $uriString")
        }
    }

    private suspend fun BackupFile.getContentHash(
        folderId: FolderId,
        nodeKey: Key.Node,
    ) = kotlin.runCatching {
        uriResolver.useInputStream(uriString) { inputStream ->
            inputStream.getHexMessageDigest(
                algorithm = configurationProvider.contentDigestAlgorithm
            )?.let { hexMessageDigest ->
                this@CheckDuplicates.getContentHash(folderId, nodeKey, hexMessageDigest)
                    .getOrThrow()
            }
        }
    }
}
