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
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.MoveInfo
import me.proton.core.drive.link.domain.entity.RenameInfo

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

    suspend fun moveLink(linkId: LinkId, moveInfo: MoveInfo): Result<Unit>

    suspend fun renameLink(
        linkId: LinkId,
        renameInfo: RenameInfo
    ): Result<Unit>

    suspend fun delete(linkIds: List<LinkId>)

    suspend fun insertOrUpdate(links: List<Link>)
}
