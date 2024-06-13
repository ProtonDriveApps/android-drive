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

package me.proton.core.drive.share.user.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.usecase.DeleteShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class LeaveShare @Inject constructor(
    private val deleteMember: DeleteMember,
    private val deleteShare: DeleteShare,
    private val deleteLocalSharedWithMe: DeleteLocalSharedWithMe,
) {
    operator fun invoke(
        volumeId: VolumeId,
        linkId: LinkId,
        memberId: String,
    ): Flow<DataResult<Unit>> = flow {
        emit(DataResult.Processing(ResponseSource.Remote))
        emitAll(
            deleteMember(
                shareId = linkId.shareId,
                memberId = memberId,
            )
        )
    }.mapSuccess { result ->
        deleteShare(
            shareId = linkId.shareId,
            locallyOnly = true
        ).getOrNull(SHARING, "Cannot remove local share")
        deleteLocalSharedWithMe(
            volumeId = volumeId,
            linkId = linkId,
        ).getOrNull(SHARING, "Cannot remove local shared with me listing")
        result
    }
}
