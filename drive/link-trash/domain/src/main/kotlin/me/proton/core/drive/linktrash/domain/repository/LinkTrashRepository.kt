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
package me.proton.core.drive.linktrash.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

interface LinkTrashRepository {

    /**
     * Inserts or updates given trash state for given linkIds
     */
    suspend fun insertOrUpdateTrashState(linkIds: List<LinkId>, trashState: TrashState)

    /**
     * Removes trash state for given linkIds
     */
    suspend fun removeTrashState(linkIds: List<LinkId>)

    /**
     * Mark all trashed links in a given Share as DELETED
     */
    suspend fun markTrashedLinkAsDeleted(shareId: ShareId)

    /**
     * Check if there is trash content for given user id and volume id
     */
    fun hasTrashContent(userId: UserId, volumeId: VolumeId): Flow<Boolean>

    /**
     * Check if we have cached any work with given work id
     */
    suspend fun hasWorkWithId(workId: String): Boolean

    /**
     * Inserts or ignores given links with work id
     */
    suspend fun insertOrIgnoreWorkId(linkIds: List<LinkId>, workId: String)

    /**
     * Tries to generate work id and inserts work for given links
     */
    suspend fun insertWork(linkIds: List<LinkId>, retries: Int = DEFAULT_RETRIES): DataResult<String>

    /**
     * Gets links for given work id and removes work from cache
     */
    suspend fun getLinksAndRemoveWorkFromCache(workId: String): List<Link>

    /**
     * Marks trash content for a given Volume as fetched
     */
    suspend fun markTrashContentAsFetched(userId: UserId, volumeId: VolumeId)

    /**
     * Checks if trash content were already fetched
     */
    suspend fun shouldInitiallyFetchTrashContent(userId: UserId, volumeId: VolumeId): Boolean

    /**
     * Checks if given link is trashed
     */
    suspend fun isTrashed(linkId: LinkId): Boolean

    /**
     * Checks if any of given links is trashed
     */
    suspend fun isAnyTrashed(linkIds: Set<LinkId>): Boolean

    companion object {
        const val DEFAULT_RETRIES = 10
    }
}
