/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.link.domain.handler

import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.isNotExists

/**
 * Removes a link from the local database once the backend reports that it no longer exists,
 * so that a link whose deletion was missed is not shown forever.
 */
interface LinkNotFoundHandler {

    /**
     * Starts the local cleanup of [linkId] and returns without waiting for it, so that a caller
     * holding a lock the cleanup also needs (such as the download manager's) cannot deadlock.
     * Best effort: failures are logged, never thrown.
     */
    fun onLinkNotFound(linkId: LinkId)
}

/**
 * Runs [block], a request addressed to [linkId], and removes [linkId] locally when it fails with
 * [isNotExists]. The error is rethrown either way.
 */
inline fun <T> LinkNotFoundHandler.onNotExists(linkId: LinkId, block: () -> T): T =
    try {
        block()
    } catch (e: Exception) {
        if (e.isNotExists) {
            onLinkNotFound(linkId)
        }
        throw e
    }
