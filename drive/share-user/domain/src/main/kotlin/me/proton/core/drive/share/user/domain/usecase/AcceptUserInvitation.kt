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
import me.proton.core.drive.base.domain.extension.toDataResult
import me.proton.core.drive.base.domain.repository.fetcher
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
import me.proton.core.drive.share.user.domain.repository.UserInvitationRepository
import javax.inject.Inject

class AcceptUserInvitation @Inject constructor(
    private val repository: UserInvitationRepository,
    private val fetchSharedWithMe: FetchSharedWithMe,
    private val getSessionKeySignature: GetSessionKeySignature,
    private val updateEventAction: UpdateEventAction,
) {
    operator fun invoke(
        invitationId: UserInvitationId,
    ): Flow<DataResult<Unit>> = flow {
        val userId = invitationId.shareId.userId
        val sessionKeySignature = getSessionKeySignature(invitationId).onFailure { error ->
            return@flow emit(DataResult.Error.Local("Cannot generate session key signature", error))
        }.getOrThrow()
        updateEventAction(userId, invitationId.volumeId) {
            fetcher {
                emit(
                    coRunCatching {
                        repository.acceptInvitation(
                            invitationId = invitationId,
                            sessionKeySignature = sessionKeySignature,
                        )
                        fetchSharedWithMe(userId, null).getOrThrow()
                        Unit
                    }.toDataResult()
                )
            }
        }
    }
}
