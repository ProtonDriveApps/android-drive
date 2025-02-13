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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.fetcher
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.share.user.domain.entity.UserInvitation
import me.proton.core.drive.share.user.domain.repository.UserInvitationRepository
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class GetUserInvitationsFlow @Inject constructor(
    private val repository: UserInvitationRepository,
    private val configurationProvider: ConfigurationProvider,
) {
    operator fun invoke(
        userId: UserId, refresh: Flow<Boolean> = flowOf { !repository.hasInvitations(userId) }
    ): Flow<DataResult<List<UserInvitation>>> = refresh.transform { shouldRefresh ->
        if (shouldRefresh) {
            fetcher {
                repository.fetchAndStoreInvitations(
                    userId = userId,
                )
            }
        }
        emitAll(
            repository.getInvitationsFlow(
                userId = userId,
                limit = configurationProvider.dbPageSize,
            ).onEach { userInvitations ->
                userInvitations.filter { userInvitation -> userInvitation.details == null }
                    .map { userInvitation -> userInvitation.id }
                    .onEach { id ->
                        runCatching {
                            repository.fetchAndStoreInvitation(userId, id.invitationId)
                        }.onFailure { error ->
                            CoreLogger.w(
                                SHARING,
                                error,
                                "Cannot fetch details for ${id.invitationId}"
                            )
                        }
                    }
            }.map { invitations -> invitations.asSuccess }
        )
    }
}
