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

package me.proton.core.drive.share.crypto.domain.usecase

import android.util.Base64
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.SignData
import me.proton.core.drive.cryptobase.domain.usecase.GetSessionKeyFromEncryptedMessage
import me.proton.core.drive.cryptobase.domain.usecase.GetUnarmored
import me.proton.core.drive.cryptobase.domain.usecase.SignatureContexts
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.key.domain.usecase.GetPublicAddress
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.crypto.domain.entity.ShareInvitationRequest
import me.proton.core.key.domain.encryptSessionKey
import javax.inject.Inject

class CreateShareInvitationRequest @Inject constructor(
    private val getShare: GetShare,
    private val getPublicAddress: GetPublicAddress,
    private val getAddressKeys: GetAddressKeys,
    private val cryptoContext: CryptoContext,
    private val getSessionKeyFromEncryptedMessage: GetSessionKeyFromEncryptedMessage,
    private val signData: SignData,
    private val getUnarmored: GetUnarmored,
) {
    suspend operator fun invoke(
        shareId: ShareId,
        inviterEmail: String,
        inviteeEmail: String,
        permissions: Permissions
    ): Result<ShareInvitationRequest> = coRunCatching {
        val share = getShare(shareId).filterSuccessOrError().toResult().getOrThrow()
        val addressKeys = getAddressKeys(
            userId = shareId.userId,
            addressId = requireNotNull(share.addressId)
        )
        val publicAddress = getPublicAddress(
            userId = shareId.userId,
            email = inviteeEmail
        ).getOrThrow()
        val sessionKey = getSessionKeyFromEncryptedMessage(
            decryptKey = addressKeys.keyHolder,
            message = share.passphrase
        ).getOrThrow()
        val encryptedKeyPacket = publicAddress.encryptSessionKey(cryptoContext, sessionKey)
        val contentKeyPacketSignature = signData(
            signKey = addressKeys,
            input = encryptedKeyPacket,
            signatureContext = SignatureContexts.DRIVE_SHARE_MEMBER_INVITER
        ).getOrThrow()

        ShareInvitationRequest(
            inviterEmail = inviterEmail,
            inviteeEmail = inviteeEmail,
            permissions = permissions,
            keyPacket = Base64.encodeToString(encryptedKeyPacket, Base64.NO_WRAP),
            keyPacketSignature = Base64.encodeToString(
                getUnarmored(contentKeyPacketSignature).getOrThrow(),
                Base64.NO_WRAP
            ),
        )
    }
}
