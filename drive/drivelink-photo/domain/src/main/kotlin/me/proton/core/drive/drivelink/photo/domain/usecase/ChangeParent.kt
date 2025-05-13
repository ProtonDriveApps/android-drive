/*
 * Copyright (c) 2025 Proton AG.
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

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.link.CreateMoveMultipleInfo
import me.proton.core.drive.documentsprovider.domain.usecase.GetFileIdContentDigestMap
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinksResult
import me.proton.core.drive.link.domain.entity.MoveMultipleInfo
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class ChangeParent @Inject constructor(
    private val linkRepository: LinkRepository,
    private val createMoveMultipleInfo: CreateMoveMultipleInfo,
    private val getShare: GetShare,
    private val updateEventAction: UpdateEventAction,
    private val getFileIdContentDigestMap: GetFileIdContentDigestMap,
) {
    suspend operator fun invoke(
        parentId: ParentId,
        fileIds: Set<FileId>,
    ): Result<LinksResult> = coRunCatching {
        invoke(
            parentId = parentId,
            moveMultipleInfo = createMoveMultipleInfo(
                newParentId = parentId,
                linksContentDigests = getFileIdContentDigestMap(fileIds),
            ).getOrThrow()
        ).getOrThrow()
    }

    suspend operator fun invoke(
        parentId: ParentId,
        moveMultipleInfo: MoveMultipleInfo,
    ): Result<LinksResult> = coRunCatching {
        val parentShare = getShare(parentId.shareId).toResult().getOrThrow()
        val userId = parentShare.id.userId
        val volumeId = parentShare.volumeId
        updateEventAction(
            userId = userId,
            volumeId = volumeId,
        ) {
            linkRepository.transferMultipleLinks(
                userId = userId,
                volumeId = volumeId,
                moveMultipleInfo = moveMultipleInfo,
            )
        }
    }
}
