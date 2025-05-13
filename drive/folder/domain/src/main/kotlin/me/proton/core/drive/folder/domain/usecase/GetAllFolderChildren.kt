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
package me.proton.core.drive.folder.domain.usecase

import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.folder.domain.repository.FolderRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import javax.inject.Inject

class GetAllFolderChildren @Inject constructor(
    private val folderRepository: FolderRepository,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(
        folderId: FolderId,
        refresh: Boolean? = null,
    ): Result<List<Link>> = coRunCatching {
        val shouldRefresh = refresh ?: !folderRepository.hasFolderChildren(folderId)
        if (shouldRefresh) {
            folderRepository.fetchAllFolderChildren(folderId).getOrThrow()
        }
        getAllChildren(folderId).getOrThrow()
    }

    suspend operator fun invoke(
        folderId: FolderId,
        refresh: Boolean? = null,
        block: suspend (List<Link>) -> Unit,
    ) = coRunCatching {
        val shouldRefresh = refresh ?: !folderRepository.hasFolderChildren(folderId)
        if (shouldRefresh) {
            folderRepository.fetchAllFolderChildren(folderId).getOrThrow()
        }
        withFolderChildren(folderId) { children ->
            block(children)
        }
    }

    private suspend fun getAllChildren(folderId: FolderId): Result<List<Link>> = coRunCatching {
        val count = configurationProvider.dbPageSize
        val links = mutableListOf<Link>()
        var loaded: Int
        var fromIndex = 0
        do {
            val children = folderRepository.getFolderChildren(folderId, fromIndex, count).getOrThrow()
            fromIndex += count
            loaded = children.size
            links.addAll(children)
        } while (loaded == count)
        links
    }

    private suspend fun withFolderChildren(
        folderId: FolderId,
        block: suspend (List<Link>) -> Unit,
    ) {
        val count = configurationProvider.dbPageSize
        var loaded: Int
        var fromIndex = 0
        do {
            val children = folderRepository.getFolderChildren(folderId, fromIndex, count).getOrThrow()
            fromIndex += count
            loaded = children.size
            coRunCatching {
                if (children.isNotEmpty()) block(children)
            }.getOrNull(LogTag.FOLDER, "Operation on folder children failed")
        } while (loaded == count)
    }
}
