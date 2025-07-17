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

package me.proton.core.drive.drivelink.photo.domain.paging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.util.KeyTrackingLruCache
import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptDriveLinks
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.photo.domain.usecase.GetPhotoDriveLinks
import me.proton.core.drive.link.domain.entity.LinkId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoDriveLinks @Inject constructor(
    private val getPhotoDriveLinks: GetPhotoDriveLinks,
    private val decryptDriveLinks: DecryptDriveLinks,
    configurationProvider: ConfigurationProvider,
) {
    private val capacity = configurationProvider.dbPageSize
    private val loadTrigger = MutableSharedFlow<Set<LinkId>>(replay = 1)
    private val cache: KeyTrackingLruCache<LinkId, DriveLink> = KeyTrackingLruCache(capacity)

    fun getDriveLinksMapFlow(
        userId: UserId,
    ): Flow<Map<LinkId, DriveLink>> = loadTrigger.transformLatest { driveLinkIds ->
        val cachedDriveLinks = driveLinkIds.mapNotNull { linkId -> cache.get(linkId) }
        if (cachedDriveLinks.size == driveLinkIds.size) {
            emit(cachedDriveLinks.associateBy { driveLink -> driveLink.id })
        }
        emitAll(
            getPhotoDriveLinks(userId, driveLinkIds)
                .mapCatching { driveLinks ->
                    decryptDriveLinks(driveLinks)
                }
                .transform { result ->
                    result.onSuccess { driveLinks ->
                        driveLinks.forEach { driveLink ->
                            cache.put(driveLink.id, driveLink)
                        }
                        emit(driveLinks)
                    }.onFailure { error ->
                        error.log(PHOTO, "Cannot decrypt drive links", WARNING)
                    }
                }
                .map { driveLinks ->
                    driveLinks.associateBy { driveLink -> driveLink.id }
                }
        )
    }

    suspend fun load(linkIds: Set<LinkId>) {
        require(linkIds.size < capacity) { "Too many link ids. Try increasing capacity." }
        loadTrigger.emit(cache.keys() + linkIds)
    }
}
