/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.link.selection.domain.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId

interface LinkSelectionRepository {

    /**
     * Check if we have cached any link with given selection id
     */
    suspend fun hasSelectionWithId(selectionId: SelectionId): Boolean

    /**
     * Inserts or ignores given links with selection id
     */
    suspend fun insertOrIgnoreSelection(selectionId: SelectionId, linkIds: List<LinkId>)

    /**
     * Tries to generate selection id and inserts work for given links
     */
    suspend fun insertSelection(linkIds: List<LinkId>, retries: Int = DEFAULT_RETRIES): Result<SelectionId>

    suspend fun remove(selectionId: SelectionId)

    suspend fun remove(selectionId: SelectionId, linkIds: List<LinkId>)

    suspend fun removeAll(userId: UserId)

    suspend fun duplicateSelection(selectionId: SelectionId, retries: Int = DEFAULT_RETRIES): Result<SelectionId>

    companion object {
        const val DEFAULT_RETRIES = 10
    }
}
