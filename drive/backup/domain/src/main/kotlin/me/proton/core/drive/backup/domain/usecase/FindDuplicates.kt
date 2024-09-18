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

import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BackupDuplicateRepository
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.backup.domain.repository.FindDuplicatesRepository
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetOrCreateClientUid
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class FindDuplicates @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val findDuplicatesRepository: FindDuplicatesRepository,
    private val backupFileRepository: BackupFileRepository,
    private val backupDuplicateRepository: BackupDuplicateRepository,
    private val getOrCreateClientUid: GetOrCreateClientUid,
) {
    suspend operator fun invoke(backupFolder: BackupFolder) = coRunCatching {
        val count = with(configurationProvider) { minOf(apiPageSize, dbPageSize) }
        var files: List<BackupFile>
        do {
            val folderId = backupFolder.folderId
            val bucketId = backupFolder.bucketId
            files = backupFileRepository.getAllInFolderWithState(
                folderId = folderId,
                bucketId = bucketId,
                state = BackupFileState.IDLE,
                count = count,
            )

            if (files.isNotEmpty()) {
                CoreLogger.d(BACKUP, "Finding duplicates for ${files.size} files")
                val hashes = files.mapNotNull { file -> file.hash }
                val clientUid = getOrCreateClientUid().getOrThrow()
                val backupDuplicates = findDuplicatesRepository.findDuplicates(
                    folderId = folderId,
                    nameHashes = hashes,
                    clientUids = listOf(clientUid)
                )

                val drafts = backupDuplicates.filter { backupDuplicate ->
                    backupDuplicate.linkState == Link.State.DRAFT
                }
                val possibleDuplicates = backupDuplicates - drafts.toSet()
                val possibleDuplicatesHashes =
                    possibleDuplicates.map { backupDuplicate -> backupDuplicate.hash }
                CoreLogger.d(BACKUP, "${drafts.size} drafts found")
                CoreLogger.d(BACKUP, "${possibleDuplicatesHashes.size} possible duplicates found")

                backupFileRepository.markAs(
                    folderId = folderId,
                    bucketId = bucketId,
                    hashes = hashes - possibleDuplicatesHashes.toSet(),
                    backupFileState = BackupFileState.READY,
                )

                backupFileRepository.markAs(
                    folderId = folderId,
                    bucketId = bucketId,
                    hashes = possibleDuplicatesHashes,
                    backupFileState = BackupFileState.POSSIBLE_DUPLICATE,
                )

                backupDuplicateRepository.insertDuplicates(backupDuplicates)

            }
        } while (files.isNotEmpty())
    }
}
