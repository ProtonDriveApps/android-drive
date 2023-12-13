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
package me.proton.core.drive.link.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.CheckAvailableHashes
import me.proton.core.drive.link.domain.entity.CheckAvailableHashesInfo
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.MoveInfo
import me.proton.core.drive.link.domain.entity.RenameInfo
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

interface LinkRepository {
    /**
     * Get reactive link for given user, share id and link id
     */
    fun getLinkFlow(linkId: LinkId): Flow<DataResult<Link>>

    /**
     * Check if we have cached link for given user, share id and link id
     */
    fun hasLink(linkId: LinkId): Flow<Boolean>

    /**
     * Fetches link from the server and stores it into cache
     */
    suspend fun fetchLink(linkId: LinkId)

    /**
     * Fetches all links with given share id and link id from the server
     */
    suspend fun fetchLinks(shareId: ShareId, linkIds: Set<String>): Result<Pair<List<Link>, List<Link>>>

    /**
     * Fetches all links with given share id and link id from the server and stores them into cache.
     * Make sure that Share with respective id is already in local cache as well as all parents from
     * given link ids, otherwise this operation will fail.
     */
    suspend fun fetchAndStoreLinks(shareId: ShareId, linkIds: Set<String>)

    /**
     * Fetches all links with given link id from the server and stores them into cache.
     * Make sure that Share(s) and all parents from given link ids are already in local cache,
     * otherwise this operation will fail.
     */
    suspend fun fetchAndStoreLinks(linkIds: Set<LinkId>)

    suspend fun checkAvailableHashes(
        linkId: LinkId,
        checkAvailableHashesInfo: CheckAvailableHashesInfo,
    ): Result<CheckAvailableHashes>

    suspend fun moveLink(linkId: LinkId, moveInfo: MoveInfo): Result<Unit>

    suspend fun renameLink(
        linkId: LinkId,
        renameInfo: RenameInfo,
    ): Result<Unit>

    suspend fun delete(linkIds: List<LinkId>)

    suspend fun insertOrUpdate(links: List<Link>)

    suspend fun getCachedLinks(userId: UserId, shareId: String, linkIds: Set<String>): Set<Link>

    suspend fun findLinkIds(userId: UserId, volumeId: VolumeId, linkId: String): List<LinkId>
}
