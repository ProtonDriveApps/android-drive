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
package me.proton.core.drive.link.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.drive.base.domain.repository.fetcher
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.repository.LinkRepository
import javax.inject.Inject

class GetLink @Inject constructor(
    private val hasLink: HasLink,
    private val linkRepository: LinkRepository,
) {

    operator fun invoke(
        fileId: FileId,
        refresh: Flow<Boolean> = hasLink(fileId).map { hasLink -> !hasLink },
    ): Flow<DataResult<Link.File>> = invoke(fileId as LinkId, refresh).mapSuccess { (source, link) ->
        if (link is Link.File) {
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
    ): Flow<DataResult<Link.Folder>> = invoke(folderId as LinkId, refresh).mapSuccess { (source, link) ->
        if (link is Link.Folder) {
            DataResult.Success(source, link)
        } else {
            if (source == ResponseSource.Local) {
                DataResult.Error.Local("FolderId $folderId was not a folder", null)
            } else {
                DataResult.Error.Remote("FolderId $folderId was not a folder", null)
            }
        }
    }

    operator fun invoke(
        albumId: AlbumId,
        refresh: Flow<Boolean> = hasLink(albumId).map { hasLink -> !hasLink },
    ): Flow<DataResult<Link.Album>> = invoke(albumId as LinkId, refresh).mapSuccess { (source, link) ->
        if (link is Link.Album) {
            DataResult.Success(source, link)
        } else {
            if (source == ResponseSource.Local) {
                DataResult.Error.Local("AlbumId $albumId was not an album", null)
            } else {
                DataResult.Error.Remote("AlbumId $albumId was not an album", null)
            }
        }
    }

    operator fun invoke(
        parentId: ParentId,
        refresh: Flow<Boolean> = hasLink(parentId).map { hasLink -> !hasLink },
    ) = when (parentId) {
        is FolderId -> invoke(parentId, refresh)
        is AlbumId -> invoke(parentId, refresh)
    }

    operator fun invoke(
        linkId: LinkId,
        refresh: Flow<Boolean> = hasLink(linkId).map { hasLink -> !hasLink },
    ) =
        refresh.transform { shouldRefresh ->
            if (shouldRefresh) {
                fetcher<Link> { linkRepository.fetchLink(linkId) }
            }
            emitAll(linkRepository.getLinkFlow(linkId))
        }
}
