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

package me.proton.core.drive.drivelink.list.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptDriveLinks
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

class GetDecryptedDriveLinks @Inject constructor(
    private val getFolderChildrenDriveLinks: GetFolderChildrenDriveLinks,
    private val decryptDriveLinks: DecryptDriveLinks,
    private val configurationProvider: ConfigurationProvider,
) {

    operator fun invoke(parentId: FolderId, fromIndex: Int, count: Int,): Flow<Result<List<DriveLink>>> =
        getFolderChildrenDriveLinks(parentId, fromIndex, count)
            .mapCatching { driveLinks ->
                decryptDriveLinks(driveLinks)
            }

    suspend operator fun invoke(parentId: FolderId): Result<List<DriveLink>> = coRunCatching {
        val count = configurationProvider.dbPageSize
        val driveLinks = mutableListOf<DriveLink>()
        var loaded: Int
        var fromIndex = 0
        do {
            val decryptedFolderChildren = invoke(parentId, fromIndex, count).first().getOrThrow()
            fromIndex += count
            loaded = decryptedFolderChildren.size
            driveLinks.addAll(decryptedFolderChildren)
        } while (loaded == count)
        driveLinks
    }
}
