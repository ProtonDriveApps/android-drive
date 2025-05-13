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

package me.proton.android.drive.photos.domain.usecase

import me.proton.core.drive.backup.domain.repository.FindDuplicatesRepository
import me.proton.core.drive.base.domain.usecase.GetOrCreateClientUid
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.CopyInfo
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.MoveMultipleInfo
import me.proton.core.drive.link.domain.entity.ParentId
import javax.inject.Inject

class FindAndCheckDuplicates @Inject constructor(
    private val findDuplicatesRepository: FindDuplicatesRepository,
    private val getOrCreateClientUid: GetOrCreateClientUid,
) {
    suspend operator fun invoke(
        newParentId: ParentId,
        moveMultipleInfo: MoveMultipleInfo,
    ): Result<Set<FileId>> = coRunCatching {
        val nameAndContentHashMap = moveMultipleInfo.links.map { file ->
            val contentHash = if (file is MoveMultipleInfo.MoveInfo.PhotoFile) {
                file.contentHash
            } else null
            FileHash(file.linkId as FileId, file.hash, contentHash)
        }
        invoke(newParentId, nameAndContentHashMap)
    }
    suspend operator fun invoke(
        newParentId: ParentId,
        copyInfos: Map<FileId, CopyInfo>,
    ): Result<Set<FileId>> = coRunCatching {
        val nameAndContentHashMap = copyInfos.map { (linkId, info) ->
            val contentHash = if (info is CopyInfo.Photo) {
                info.photos.contentHash
            } else null
            FileHash(linkId, info.hash, contentHash)
        }
        invoke(newParentId, nameAndContentHashMap)
    }

    private suspend fun invoke(
        newParentId: ParentId,
        fileHashes: List<FileHash>
    ): Set<FileId> {
        val clientUid = getOrCreateClientUid().getOrThrow()
        val duplicates = findDuplicatesRepository.findDuplicates(
            parentId = newParentId,
            nameHashes = fileHashes.map { it.hash },
            clientUids = listOf(clientUid)
        )

        val drafts = duplicates.filter { duplicate ->
            duplicate.linkState == Link.State.DRAFT
        }
        val possibleDuplicates = duplicates - drafts.toSet()
        return possibleDuplicates.mapNotNull { duplicate ->
            val fileHash = fileHashes.first { it.hash == duplicate.hash }
            if (fileHash.contentHash == duplicate.contentHash) {
                fileHash.id
            } else {
                null
            }
        }.toSet()
    }

    private data class FileHash(
        val id: FileId,
        val hash: String,
        val contentHash: String?,
    )
}
