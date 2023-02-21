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

package me.proton.core.drive.drivelink.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.domain.arch.transformSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.asSuccessOrNullAsError
import me.proton.core.drive.base.domain.repository.fetcher
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.repository.DriveLinkRepository
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.link.domain.usecase.HasLink
import me.proton.core.drive.share.crypto.domain.usecase.GetOrCreateMainShare
import javax.inject.Inject

class GetDriveLink @Inject constructor(
    private val hasLink: HasLink,
    private val linkRepository: LinkRepository,
    private val driveLinkRepository: DriveLinkRepository,
    private val getMainShare: GetOrCreateMainShare,
    private val updateIsAnyAncestorMarkedAsOffline: UpdateIsAnyAncestorMarkedAsOffline,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        userId: UserId,
        folderId: FolderId?,
        refresh: (linkId: LinkId) -> Flow<Boolean> = { id ->
            hasLink(id).map { hasLink -> !hasLink }
        },
    ): Flow<DataResult<DriveLink.Folder>> = folderId?.let {
        invoke(folderId = folderId, refresh = refresh(folderId))
    } ?: getMainShare(userId)
        .mapSuccess { (_, share) ->
            share.rootFolderId.asSuccess
        }.transformSuccess { (_, folderId) ->
            emitAll(invoke(folderId, refresh(folderId)))
        }

    operator fun invoke(
        fileId: FileId,
        refresh: Flow<Boolean> = hasLink(fileId).map { hasLink -> !hasLink },
    ): Flow<DataResult<DriveLink.File>> = invoke(fileId as LinkId, refresh).mapSuccess { (source, link) ->
        if (link is DriveLink.File) {
            DataResult.Success(source, link)
        } else {
            if (source == ResponseSource.Local) {
                DataResult.Error.Local("FileId $fileId was not a file", null)
            } else {
                DataResult.Error.Remote("FileId $fileId was not a file", null)
            }
        }
    }

    operator fun invoke(
        folderId: FolderId,
        refresh: Flow<Boolean> = hasLink(folderId).map { hasLink -> !hasLink },
    ): Flow<DataResult<DriveLink.Folder>> = invoke(folderId as LinkId, refresh).mapSuccess { (source, link) ->
        if (link is DriveLink.Folder) {
            DataResult.Success(source, link)
        } else {
            if (source == ResponseSource.Local) {
                DataResult.Error.Local("FileId $folderId was not a folder", null)
            } else {
                DataResult.Error.Remote("FileId $folderId was not a folder", null)
            }
        }
    }

    operator fun invoke(
        linkId: LinkId,
        refresh: Flow<Boolean> = hasLink(linkId).map { hasLink -> !hasLink },
    ): Flow<DataResult<DriveLink>> = refresh.transform { shouldRefresh ->
        if (shouldRefresh) {
            fetcher<DriveLink> { linkRepository.fetchLink(linkId) }
        }
        emitAll(
            driveLinkRepository.getDriveLink(linkId)
                .map { driveLink ->
                    driveLink
                        ?.let { updateIsAnyAncestorMarkedAsOffline(listOf(driveLink)).first() }
                        .asSuccessOrNullAsError()
                }
        )
    }
}
