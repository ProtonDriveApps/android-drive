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
import kotlinx.coroutines.flow.last
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.user.domain.entity.CreateShareInvitationsResult
import me.proton.core.drive.share.user.domain.entity.ShareUsersInvitation
import javax.inject.Inject

class CreateShareInvitations @Inject constructor(
    private val updateEventAction: UpdateEventAction,
    private val getShare: GetShare,
    private val createShareInvitation: CreateShareInvitation,
) {
    operator fun invoke(
        shareId: ShareId,
        invitation: ShareUsersInvitation,
    ): Flow<DataResult<CreateShareInvitationsResult>> = flow {
        val share = getShare(shareId).filterSuccessOrError().toResult().getOrThrow()
        updateEventAction(shareId.userId, share.volumeId) {
            emit(DataResult.Processing(ResponseSource.Remote))
            invitation.members.associateWith { shareMemberInvitation ->
                createShareInvitation(
                    shareId = shareId,
                    invitation = shareMemberInvitation
                ).filterSuccessOrError().last()
            }.let { results ->
                emit(
                    CreateShareInvitationsResult(
                        failures = results.mapNotNull { (shareMemberInvitation, result) ->
                            if (result is DataResult.Error) {
                                shareMemberInvitation to result.cause
                            } else {
                                null
                            }
                        }.toMap(),
                        successes = results.mapNotNull { (shareMemberInvitation, result) ->
                            if (result is DataResult.Success) {
                                shareMemberInvitation to result.value
                            } else {
                                null
                            }
                        }.toMap(),
                    ).asSuccess
                )
            }
        }
    }
}
