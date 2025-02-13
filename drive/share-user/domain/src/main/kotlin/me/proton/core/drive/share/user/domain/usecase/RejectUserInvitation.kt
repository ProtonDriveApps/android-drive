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
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.repository.fetcher
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
import me.proton.core.drive.share.user.domain.repository.UserInvitationRepository
import me.proton.core.drive.volume.domain.usecase.GetOldestActiveVolume
import javax.inject.Inject

class RejectUserInvitation @Inject constructor(
    private val repository: UserInvitationRepository,
    private val getShare: GetShare,
    private val updateEventAction: UpdateEventAction,
) {
    operator fun invoke(
        invitationId: UserInvitationId,
    ): Flow<DataResult<Unit>> = flow {
        val userId = invitationId.shareId.userId
        updateEventAction(userId, invitationId.volumeId) {
            fetcher {
                repository.rejectInvitation(invitationId)
                emit(DataResult.Success(ResponseSource.Remote, Unit))
            }
        }
    }
}
