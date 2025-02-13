/*
 * Copyright (c) 2025 Proton AG.
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

import android.util.Base64
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.GetUnarmored
import me.proton.core.drive.cryptobase.domain.usecase.SignData
import me.proton.core.drive.cryptobase.domain.usecase.SignatureContexts
import me.proton.core.drive.cryptobase.domain.usecase.UseSessionKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.share.user.domain.entity.UserInvitationDetails
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
import javax.inject.Inject

class GetSessionKeySignature @Inject constructor(
    private val getUserInvitation: GetUserInvitation,
    private val getAddressKeys: GetAddressKeys,
    private val useSessionKey: UseSessionKey,
    private val signData: SignData,
    private val getUnarmored: GetUnarmored,
) {
    suspend operator fun invoke(id: UserInvitationId) = coRunCatching {
        val invitation = getUserInvitation(id).getOrThrow()
        requireNotNull(invitation.details) { "Cannot get session key signature without invitation details" }
        invoke(invitation.details).getOrThrow()
    }

    suspend operator fun invoke(details: UserInvitationDetails) = coRunCatching {
        val userId = details.id.shareId.userId
        val addressKeys = getAddressKeys(userId, details.inviteeEmail)
        useSessionKey(
            decryptKey = addressKeys.keyHolder,
            encryptedKeyPacket = Base64.decode(details.keyPacket, Base64.NO_WRAP),
        ) { sessionKey ->
            val sessionKeySignature = signData(
                signKey = addressKeys.keyHolder,
                input = sessionKey.key,
                signatureContext = SignatureContexts.DRIVE_SHARE_MEMBER_MEMBER
            ).getOrThrow()
            Base64.encodeToString(
                getUnarmored(sessionKeySignature).getOrThrow(),
                Base64.NO_WRAP
            )
        }.getOrThrow()
    }
}
