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

package me.proton.core.drive.folder.domain.usecase

import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShares
import javax.inject.Inject

class HasAnyCachedFolderChildren @Inject constructor(
    private val getShares: GetShares,
    private val hasFolderChildren: HasFolderChildren,
    private val linkRepository: LinkRepository,
) {
    suspend operator fun invoke(userId: UserId, filesOnly: Boolean = false): Boolean {
        val shares = listOfNotNull(
            getShares(userId, Share.Type.MAIN, flowOf(false)).toResult().getOrNull(),
            getShares(userId, Share.Type.PHOTO, flowOf(false)).toResult().getOrNull(),
            getShares(userId, Share.Type.DEVICE, flowOf(false)).toResult().getOrNull(),
        )
        return if (filesOnly) {
            shares
                .map { shares ->
                    shares.map { share -> share.id }
                }
                .any { shareIds -> shareIds.any { shareId -> linkRepository.hasAnyFileLink(shareId) } }
        } else {
            shares
                .map { shares ->
                    shares.map { share: Share -> FolderId(share.id, share.rootLinkId) }
                }.map { folderIds ->
                    folderIds.map { folderId -> hasFolderChildren(folderId) }
                }.any { hasChildrens -> hasChildrens.any { hasChildren -> hasChildren } }
        }
    }
}
