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
package me.proton.core.drive.linkoffline.domain.repository

import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId

interface LinkOfflineRepository {

    /**
     * Check if given link is marked as offline
     */
    suspend fun isMarkedOffline(linkId: LinkId): Boolean

    /**
     * Check if any from the given link set is marked as offline
     */
    suspend fun isAnyMarkedOffline(linkIds: Set<LinkId>): Boolean

    /**
     * Mark given link as offline
     */
    suspend fun addOffline(linkId: LinkId)

    /**
     * Unmarked given link as offline
     */
    suspend fun removeOffline(linkId: LinkId)

    /**
     * Check if given file is part of an offline album
     */
    suspend fun isPartOfAnOfflineAlbum(fileId: FileId): Boolean
}
