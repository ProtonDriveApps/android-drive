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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.domain.arch.transformSuccess
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.contact.domain.usecase.GetContactEmails
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.user.domain.entity.ShareUser
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetShareUsers @Inject constructor(
    private val getLink: GetLink,
    private val getShare: GetShare,
    private val getContactEmails: GetContactEmails,
    private val getInvitationsFlow: GetInvitationsFlow,
    private val getMembersFlow: GetMembersFlow,
) {
    operator fun invoke(
        linkId: LinkId,
    ): Flow<DataResult<List<ShareUser>>> = getLink(linkId).transformSuccess { (_, link) ->
        val shareId = link.sharingDetails?.shareId
        if (shareId != null) {
            emitAll(
                getShare(shareId).transformSuccess { (_, _) ->
                    emitAll(
                        getInvitationsFlow(
                            shareId = shareId,
                            refresh = flowOf(true), /* Remove when events are implemented */
                        )
                    )
                }.transformSuccess { (_, invitations) ->
                    emitAll(getMembersFlow(
                        shareId = shareId,
                        refresh = flowOf(true), /* Remove when events are implemented */
                    ).mapSuccess { (_, members) ->
                        (invitations + members).asSuccess
                    })
                }.transformSuccess { (_, shareUsers) ->
                    emitAll(getContactEmails(linkId.userId).mapSuccess { (_, contactEmails) ->
                        shareUsers.map { user ->
                            val name = contactEmails.firstOrNull { it.email == user.email }?.name
                            when (user) {
                                is ShareUser.Member -> user.copy(displayName = name)
                                is ShareUser.Invitee -> user.copy(displayName = name)
                                is ShareUser.ExternalInvitee -> user.copy(displayName = name)
                            }
                        }.asSuccess
                    })
                }
            )
        } else {
            emit(emptyList<ShareUser>().asSuccess)
        }
    }
}
