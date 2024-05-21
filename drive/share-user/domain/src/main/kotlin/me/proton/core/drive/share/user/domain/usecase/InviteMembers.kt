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
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.share.crypto.domain.usecase.GetOrCreateShare
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.user.domain.entity.CreateShareInvitationsResult
import me.proton.core.drive.share.user.domain.entity.ShareUsersInvitation
import javax.inject.Inject

class InviteMembers @Inject constructor(
    private val getOrCreateShare: GetOrCreateShare,
    private val getShare: GetShare,
    private val createShareInvitations: CreateShareInvitations,
) {
    operator fun invoke(
        shareUsersInvitation: ShareUsersInvitation,
    ): Flow<DataResult<CreateShareInvitationsResult>> = flow {
        emit(DataResult.Processing(ResponseSource.Local))
        val volumeId = getShare(
            shareId = shareUsersInvitation.linkId.shareId,
        ).toResult().onFailure { error ->
            return@flow emit(DataResult.Error.Local("Cannot get share", error))
        }.getOrThrow().volumeId

        val linkShare = getOrCreateShare(
            volumeId = volumeId,
            linkId = shareUsersInvitation.linkId
        ).toResult().onFailure { error ->
            return@flow emit(DataResult.Error.Remote("Cannot create share", error))
        }.getOrThrow()

        emitAll(createShareInvitations(linkShare.id, shareUsersInvitation))
    }
}
