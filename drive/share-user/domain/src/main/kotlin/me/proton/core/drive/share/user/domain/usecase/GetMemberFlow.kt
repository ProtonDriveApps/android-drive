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
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.contact.domain.usecase.GetContactEmails
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.repository.ShareInvitationRepository
import me.proton.core.drive.share.user.domain.repository.ShareMemberRepository
import javax.inject.Inject

class GetMemberFlow @Inject constructor(
    private val repository: ShareMemberRepository,
    private val getContactEmails: GetContactEmails,
) {
    operator fun invoke(
        shareId: ShareId,
        memberId: String,
    ): Flow<ShareUser.Member> = repository.getMemberFlow(shareId, memberId).map { invitee ->
        val contactEmails = getContactEmails(shareId.userId)
            .filterSuccessOrError().toResult().getOrNull().orEmpty()

        invitee.copy(
            displayName = contactEmails.firstOrNull { contactEmail ->
                contactEmail.email == invitee.email
            }?.name
        )
    }
}
