/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.share.crypto.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.DeleteShare
import me.proton.core.drive.share.domain.usecase.GetShares
import javax.inject.Inject

class DeleteLocalShares @Inject constructor(
    private val getShares: GetShares,
    private val deleteShare: DeleteShare,
) {
    suspend operator fun invoke(linkIds: List<LinkId>) = coRunCatching {
        with (linkIds.map { linkId -> linkId.id }) {
            listOf(Share.Type.STANDARD, Share.Type.DEVICE)
                .map { shareType ->
                    getShares(linkIds.first().userId, shareType, flowOf(false))
                        .filterSuccessOrError()
                        .first()
                        .toResult()
                        .getOrNull()
                        ?.mapNotNull { share -> if (share.rootLinkId in this) share.id else null }
                        ?: emptyList()
                }
                .flatten()
                .forEach { shareId ->
                    deleteShare(
                        shareId = shareId,
                        locallyOnly = true,
                    )
                }
        }
    }
}
