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
package me.proton.core.drive.folder.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.folder.data.api.FolderApiDataSource
import me.proton.core.drive.folder.data.db.FolderDatabase
import me.proton.core.drive.folder.data.db.FolderMetadataEntity
import me.proton.core.drive.folder.domain.entity.FolderInfo
import me.proton.core.drive.folder.domain.entity.Sorting
import me.proton.core.drive.folder.domain.repository.FolderRepository
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class FolderRepositoryImpl @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val api: FolderApiDataSource,
    private val db: FolderDatabase,
) : FolderRepository {
    private val folderDao = db.folderDao
    private val folderMetadataDao = db.folderMetadataDao

    override suspend fun hasFolderChildren(folderId: FolderId): Boolean =
        folderDao.hasFolderChildren(folderId.userId, folderId.shareId.id, folderId.id)

    override suspend fun getFolderChildren(
        folderId: FolderId,
        fromIndex: Int,
        count: Int,
    ): Result<List<Link>> = coRunCatching {
        folderDao.getFolderChildren(
            folderId.userId,
            folderId.shareId.id,
            folderId.id,
            count,
            fromIndex,
        ).map { linkWithPropertiesEntity -> linkWithPropertiesEntity.toLinkWithProperties().toLink() }
    }

    override suspend fun fetchAllFolderChildren(
        folderId: FolderId,
    ): Result<List<Link>> = coRunCatching {
        var pageIndex = 0
        val fetchedChildren = mutableListOf<Link>()
        do {
            val (fetchedElements, saveAction) = fetchFolderChildren(
                folderId,
                pageIndex++,
                configurationProvider.uiPageSize,
                Sorting.DEFAULT,
            ).getOrThrow()
            saveAction()
            fetchedChildren.addAll(fetchedElements)
        } while (fetchedElements.size == configurationProvider.uiPageSize)
        fetchedChildren
    }

    override suspend fun fetchFolderChildren(
        folderId: FolderId,
        pageIndex: Int,
        pageSize: Int,
        sorting: Sorting,
    ): Result<Pair<List<Link>, SaveAction>> = coRunCatching {
        with(
            api.getFolderChildren(folderId, pageIndex, pageSize, sorting)
                .map { linkDto -> linkDto.toLinkWithProperties(folderId.shareId) }
        ) {
            map { linkWithProperties -> linkWithProperties.toLink() } to SaveAction {
                db.inTransaction {
                    folderMetadataDao.insertOrUpdate(
                        userId = folderId.userId,
                        shareId = folderId.shareId.id,
                        linkId = folderId.id,
                        lastFetchChildrenTimestamp = System.currentTimeMillis(),
                    )
                    folderDao.insertOrUpdate(*toTypedArray())
                }
            }
        }
    }

    override suspend fun createFolder(
        shareId: ShareId,
        folderInfo: FolderInfo,
    ): Result<FolderId> = coRunCatching {
        FolderId(shareId, api.createFolder(shareId, folderInfo))
    }

    override suspend fun deleteFolderChildren(
        folderId: FolderId,
        linkIds: List<LinkId>,
    ): Result<Unit> = coRunCatching {
        api.deleteFolderChildren(folderId, linkIds)
    }

    override suspend fun shouldInitiallyFetchFolderChildren(folderId: FolderId): Boolean =
        folderMetadataDao.get(folderId.userId, folderId.shareId.id, folderId.id).hasNotFetchedFolderChildren

    override suspend fun hasFolderChildrenWithHash(folderId: FolderId, hash: String): Boolean =
        folderDao.hasFolderChildrenWithHash(folderId.userId, folderId.shareId.id, folderId.id, hash)

    private val FolderMetadataEntity?.hasNotFetchedFolderChildren: Boolean
        get() = this?.lastFetchChildrenTimestamp == null
}
