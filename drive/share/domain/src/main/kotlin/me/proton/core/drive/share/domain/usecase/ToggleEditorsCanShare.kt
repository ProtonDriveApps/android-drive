/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.share.domain.usecase

import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.repository.ShareRepository
import javax.inject.Inject

class ToggleEditorsCanShare @Inject constructor(
    private val shareRepository: ShareRepository,
    private val getShare: GetShare,
) {

    suspend operator fun invoke(shareId: ShareId) = coRunCatching {
        invoke(share = getShare(shareId).toResult().getOrThrow()).getOrThrow()
    }

    suspend operator fun invoke(share: Share) = coRunCatching {
        val editorsCanShare = share.editorsCanShare
            ?: requireNotNull(
                getShare(
                    shareId = share.id,
                    refresh = flowOf(true),
                ).toResult().getOrThrow().editorsCanShare
            )
        shareRepository.setEditorsCanShare(share.id, editorsCanShare.not())
        getShare(
            shareId = share.id,
            refresh = flowOf(true),
        ).toResult().getOrNull(LogTag.SHARE, "Failed to refresh share")
    }
}
