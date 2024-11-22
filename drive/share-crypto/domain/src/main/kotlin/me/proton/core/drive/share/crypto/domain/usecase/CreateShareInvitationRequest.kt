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
import me.proton.core.drive.base.domain.usecase.GetUserEmail
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.SignData
import me.proton.core.drive.cryptobase.domain.usecase.GetSessionKeyFromEncryptedMessage
import me.proton.core.drive.cryptobase.domain.usecase.GetUnarmored
import me.proton.core.drive.cryptobase.domain.usecase.SignatureContexts
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.extension.primaryPublicKey
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.key.domain.usecase.GetPublicAddressInfo
import me.proton.core.drive.share.crypto.domain.entity.ShareInvitationRequest
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetAddressId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.key.domain.encryptSessionKey
import me.proton.core.key.domain.entity.key.PublicAddressInfo
import javax.inject.Inject

class CreateShareInvitationRequest @Inject constructor(
    private val getShare: GetShare,
    private val getPublicAddressInfo: GetPublicAddressInfo,
    private val getAddressId: GetAddressId,
    private val getAddressKeys: GetAddressKeys,
    private val getUserEmail: GetUserEmail,
    private val cryptoContext: CryptoContext,
    private val getSessionKeyFromEncryptedMessage: GetSessionKeyFromEncryptedMessage,
    private val signData: SignData,
    private val getUnarmored: GetUnarmored,
) {
    suspend operator fun invoke(
        shareId: ShareId,
        inviteeEmail: String,
        permissions: Permissions,
        message: String? = null,
        itemName: String? = null,
        externalInvitationId: String? = null,
    ): Result<ShareInvitationRequest> = coRunCatching {
        val publicAddress = getPublicAddressInfo(
            userId = shareId.userId,
            email = inviteeEmail,
            unverified = true,
        ).getOrThrow()
        if (publicAddress != null) {
            createInternalRequest(
                shareId = shareId,
                inviteeEmail = inviteeEmail,
                permissions = permissions,
                publicAddressInfo = publicAddress,
                message = message,
                itemName = itemName,
                externalInvitationId = externalInvitationId,
            )
        } else {
            createExternalRequest(
                shareId = shareId,
                inviteeEmail = inviteeEmail,
                permissions = permissions,
                message = message,
                itemName = itemName,
            )
        }
    }

    private suspend fun createInternalRequest(
        shareId: ShareId,
        inviteeEmail: String,
        permissions: Permissions,
        message: String?,
        itemName: String?,
        publicAddressInfo: PublicAddressInfo,
        externalInvitationId: String? = null,
    ): ShareInvitationRequest.Internal {
        val share = getShare(shareId).filterSuccessOrError().toResult().getOrThrow()
        val addressId = getAddressId(shareId.userId, share.volumeId).getOrThrow()
        val addressKeys = getAddressKeys(
            userId = shareId.userId,
            addressId = addressId
        )
        val sessionKey = getSessionKeyFromEncryptedMessage(
            decryptKey = addressKeys.keyHolder,
            message = share.passphrase
        ).getOrThrow()
        val encryptedKeyPacket = publicAddressInfo
            .primaryPublicKey(unverified = true)
            .publicKey
            .encryptSessionKey(cryptoContext, sessionKey)
        val contentKeyPacketSignature = signData(
            signKey = addressKeys,
            input = encryptedKeyPacket,
            signatureContext = SignatureContexts.DRIVE_SHARE_MEMBER_INVITER
        ).getOrThrow()

        return ShareInvitationRequest.Internal(
            inviterEmail = getUserEmail(shareId.userId, addressId),
            inviteeEmail = inviteeEmail,
            permissions = permissions,
            keyPacket = Base64.encodeToString(encryptedKeyPacket, Base64.NO_WRAP),
            keyPacketSignature = Base64.encodeToString(
                getUnarmored(contentKeyPacketSignature).getOrThrow(),
                Base64.NO_WRAP
            ),
            message = message,
            itemName = itemName,
            externalInvitationId = externalInvitationId,
        )
    }

    private suspend fun createExternalRequest(
        shareId: ShareId,
        inviteeEmail: String,
        permissions: Permissions,
        message: String?,
        itemName: String?,
    ): ShareInvitationRequest.External {
        val share = getShare(shareId).filterSuccessOrError().toResult().getOrThrow()
        val addressId = requireNotNull(share.addressId)
        val addressKeys = getAddressKeys(
            userId = shareId.userId,
            addressId = addressId
        )
        val sessionKey = getSessionKeyFromEncryptedMessage(
            decryptKey = addressKeys.keyHolder,
            message = share.passphrase
        ).getOrThrow()
        val key = "$inviteeEmail|${Base64.encodeToString(sessionKey.key, Base64.NO_WRAP)}".toByteArray()
        val signature = signData(
            signKey = addressKeys,
            input = key,
            signatureContext = SignatureContexts.DRIVE_SHARE_MEMBER_EXTERNAL_INVITATION
        ).getOrThrow()

        return ShareInvitationRequest.External(
            inviterAddressId = addressId,
            inviteeEmail = inviteeEmail,
            permissions = permissions,
            invitationSignature = Base64.encodeToString(
                getUnarmored(signature).getOrThrow(),
                Base64.NO_WRAP
            ),
            message = message,
            itemName = itemName,
        )
    }
}
