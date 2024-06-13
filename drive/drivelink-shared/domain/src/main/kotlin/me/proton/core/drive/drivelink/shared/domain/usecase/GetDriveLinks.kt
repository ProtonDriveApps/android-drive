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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLinks
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.FetchLinks
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class GetDriveLinks @Inject constructor(
    private val getShare: GetShare,
    private val getDriveLinks: GetDriveLinks,
    private val fetchLinks: FetchLinks,
) {
    operator fun invoke(
        linkIds: Set<LinkId>,
        refresh: Boolean = false,
    ): Flow<Result<List<DriveLink>>> = flow {
        linkIds
            .map { linkId -> linkId.shareId }
            .toSet()
            .forEach { shareId ->
                getShare(shareId).toResult()
                    .onSuccess {
                        fetchMissingDriveLinks(
                            linkIds = linkIds.filter { linkId ->
                                linkId.shareId == shareId
                            }.toSet(),
                            refresh = refresh,
                        ).getOrNull(LogTag.SHARING)
                    }
                    .getOrNull(LogTag.SHARING)
            }
        emitAll(
            getDriveLinks(linkIds.toList())
                .map { driveLinks ->
                    driveLinks.asSuccess
                }
        )
    }.mapCatching { driveLinks -> driveLinks }

    private suspend fun fetchMissingDriveLinks(linkIds: Set<LinkId>, refresh: Boolean = false) = coRunCatching {
        getDriveLinks(linkIds.toList())
            .map { driveLinks ->
                driveLinks.map { driveLink -> driveLink.id.shareId to driveLink.id.id }
            }
            .first()
            .let { driveLinkIds ->
                if (refresh) {
                    linkIds
                } else {
                    linkIds
                        .filterNot { linkId ->
                            driveLinkIds.contains(linkId.shareId to linkId.id)
                        }
                        .toSet()
                }
            }
            .takeIfNotEmpty()
            ?.let { missingDriveLinkIds ->
                fetch(missingDriveLinkIds).getOrThrow()
            }
    }

    private suspend fun fetch(linkIds: Set<LinkId>) = coRunCatching {
        linkIds.groupBy({ linkId -> linkId.shareId }) { linkId -> linkId.id }
            .forEach { (shareId, linkIds) ->
                fetchLinks(shareId, linkIds.toSet(), true).getOrThrow()
            }
    }
}
