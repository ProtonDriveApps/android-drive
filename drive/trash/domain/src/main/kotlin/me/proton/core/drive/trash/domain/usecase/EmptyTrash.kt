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

package me.proton.core.drive.trash.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.drive.trash.domain.TrashManager
import javax.inject.Inject

@ExperimentalCoroutinesApi
class EmptyTrash @Inject constructor(
    private val trashManager: TrashManager,
    private val getShares: GetShares,
    private val getMainShare: GetMainShare,
) {
    @Deprecated(
        message = "This method is deprecated because finding proper share is ambiguous with multiple active volumes",
        replaceWith = ReplaceWith("EmptyTrash(userId: UserId, shareId: ShareId)")
    )
    suspend operator fun invoke(userId: UserId) {
        val shareIds = setOfNotNull(
            getMainShareId(userId),
            getPhotoShareId(userId)
        )
        if (shareIds.isNotEmpty()) {
            invoke(userId, shareIds)
        }
    }

    private suspend fun getMainShareId(userId: UserId): ShareId? =
        getMainShare(userId)
            .mapSuccessValueOrNull()
            .first()
            ?.id

    private suspend fun getPhotoShareId(userId: UserId): ShareId? =
        getShares(userId, Share.Type.PHOTO)
            .mapSuccessValueOrNull()
            .filterNotNull()
            .first()
            .filterNot { share -> share.isLocked }
            .takeIf { shares -> shares.isNotEmpty() }
            ?.let { shares ->
                require(shares.size == 1) { "Multiple photo shares found" }
                shares.first().id
            }

    operator fun invoke(userId: UserId, shareIds: Set<ShareId>) {
        trashManager.emptyTrash(userId, shareIds)
    }
}
