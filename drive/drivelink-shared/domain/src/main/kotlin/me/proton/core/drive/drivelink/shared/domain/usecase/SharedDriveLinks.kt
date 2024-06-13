/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.drivelink.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptDriveLinks
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.LinkId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class SharedDriveLinks @Inject constructor(
    configurationProvider: ConfigurationProvider,
    private val getSharedDriveLinks: GetDriveLinks,
    private val decryptDriveLinks: DecryptDriveLinks,
) {
    private val capacity = configurationProvider.dbPageSize
    private val driveLinkIds = MutableStateFlow<Set<LinkId>>(emptySet())

    fun getSharedDriveLinksMapFlow(): Flow<Map<LinkId, DriveLink>> =
        getDriveLinksMapFlow { linkIds ->
            getSharedDriveLinks(linkIds)
        }

    private fun getDriveLinksMapFlow(
        driveLinks: (Set<LinkId>) -> Flow<Result<List<DriveLink>>>,
    ): Flow<Map<LinkId, DriveLink>> = driveLinkIds.transformLatest { linkIds ->
        emitAll(
            driveLinks(linkIds)
                .mapCatching { driveLinks ->
                    driveLinks
                        .groupBy { driveLink -> driveLink.id.shareId }
                        .map { (_, driveLinks) ->
                            decryptDriveLinks(driveLinks)
                        }
                        .flatten()
                }
                .transform { result ->
                    result.onSuccess { driveLinks ->
                        emit(driveLinks)
                    }
                }
                .map { driveLinks ->
                    driveLinks.associateBy { driveLink -> driveLink.id }
                }
        )
    }

    suspend fun load(linkIds: Set<LinkId>) {
        require(linkIds.size < capacity) { "Too many link ids. Try increasing capacity." }
        val latestLinkIds = driveLinkIds.value + linkIds
        val overflow = (latestLinkIds.size) - capacity
        driveLinkIds.emit(
            if (overflow > 0) {
                latestLinkIds.drop(overflow).toSet()
            } else {
                latestLinkIds
            }
        )
    }

    suspend fun refresh(linkIds: Set<LinkId>) = coRunCatching {
        getSharedDriveLinks(linkIds.intersect(driveLinkIds.value), refresh = true).first().getOrThrow()
    }
}
