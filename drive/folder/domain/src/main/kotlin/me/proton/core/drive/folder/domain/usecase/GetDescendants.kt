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

import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.Link
import javax.inject.Inject

class GetDescendants @Inject constructor(
    private val getAllFolderChildren: GetAllFolderChildren,
) {
    suspend operator fun invoke(
        folderLink: Link.Folder,
        refresh: Boolean,
    ): Result<List<Link>> = coRunCatching {
        val folders = mutableListOf(folderLink)
        val descendants = mutableListOf<Link>()
        val refreshFolderChildren = if (refresh) flowOf(refresh) else null
        while (folders.isNotEmpty()) {
            val parent = folders.removeFirst()
            getAllFolderChildren(
                parent.id,
                refreshFolderChildren,
            )
                .toResult()
                .getOrThrow()
                .let { links ->
                    links.forEach { link ->
                        if (link is Link.Folder) {
                            folders.add(link)
                        }
                    }
                    descendants.addAll(links)
                }
        }
        descendants
    }
}
