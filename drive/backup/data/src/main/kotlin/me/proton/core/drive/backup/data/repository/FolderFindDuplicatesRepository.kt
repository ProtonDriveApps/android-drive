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

package me.proton.core.drive.backup.data.repository

import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.repository.FindDuplicatesRepository
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.link.domain.entity.CheckAvailableHashesInfo
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.repository.LinkRepository
import javax.inject.Inject

class FolderFindDuplicatesRepository @Inject constructor(
    private val linkRepository: LinkRepository,
) : FindDuplicatesRepository {

    override suspend fun findDuplicates(
        parentId: ParentId,
        nameHashes: List<String>,
        clientUids: List<ClientUid>,
    ): List<BackupDuplicate> {
        val clientUid = clientUids.firstOrNull()
        val (availableHashes, pendingHashes) = linkRepository.checkAvailableHashes(
            linkId = parentId,
            checkAvailableHashesInfo = CheckAvailableHashesInfo(
                hashes = nameHashes,
                clientUid = clientUid
            )
        ).getOrThrow()
        val notAvailableHashes = nameHashes - availableHashes.toSet() - pendingHashes.map {
            it.hash
        }.toSet()
        val notAvailable = notAvailableHashes.map { availableHash ->
            BackupDuplicate(
                parentId = parentId,
                hash = availableHash,
                contentHash = null,
                linkId = null,
                linkState = Link.State.ACTIVE,
                revisionId = null,
                clientUid = clientUid,
            )
        }
        val draft = pendingHashes.filter { hash -> hash.clientUid == clientUid }
            .map { pendingHash ->
                BackupDuplicate(
                    parentId = parentId,
                    hash = pendingHash.hash,
                    contentHash = null,
                    linkId = pendingHash.fileId,
                    linkState = Link.State.DRAFT,
                    revisionId = pendingHash.revisionId,
                    clientUid = pendingHash.clientUid,
                )
            }
        return notAvailable + draft
    }

}
