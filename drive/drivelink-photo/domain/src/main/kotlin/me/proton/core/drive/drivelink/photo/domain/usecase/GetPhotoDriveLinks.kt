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

package me.proton.core.drive.drivelink.photo.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.base.domain.extension.transformSuccess
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLinks
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLinksCount
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetPhotoDriveLinks @Inject constructor(
    private val getDriveLinks: GetDriveLinks,
    private val getDriveLinksCount: GetDriveLinksCount,
    private val getPhotoShare: GetPhotoShare,
    private val linkRepository: LinkRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    operator fun invoke(
        userId: UserId,
        linkIds: Set<LinkId>,
    ): Flow<Result<List<DriveLink>>> = getPhotoShare(userId)
        .filterSuccessOrError()
        .transformSuccess { (_, share) ->
            emitAll(
                getDriveLinksCount(share.rootFolderId)
                    .distinctUntilChanged()
                    .transform {
                        if (linkIds.isNotEmpty()) {
                            fetchMissingDriveLinks(linkIds)
                            emitAll(
                                getDriveLinks(linkIds.toList())
                                    .map { driveLinks -> driveLinks.asSuccess }
                            )
                        }
                    }
            )
        }
        .mapCatching { driveLinks -> driveLinks }

    private suspend fun fetchMissingDriveLinks(linkIds: Set<LinkId>) = coRunCatching {
        val cachedDriveLinkIds = getDriveLinks(linkIds.toList())
            .map { driveLinks ->
                driveLinks.map { driveLink -> driveLink.id }
            }.first()
        val missingDriveLinkIds = linkIds - cachedDriveLinkIds.toSet()
        if (missingDriveLinkIds.isNotEmpty()) {
            fetch(missingDriveLinkIds).getOrThrow()
        }
    }

    private suspend fun fetch(linkIds: Set<LinkId>) = coRunCatching {
        linkIds.chunked(configurationProvider.apiPageSize).forEach { chunkedLinkIds ->
            linkRepository.fetchAndStoreLinks(chunkedLinkIds.toSet())
        }
    }
}
