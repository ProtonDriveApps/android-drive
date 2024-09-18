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

import kotlinx.coroutines.flow.flowOf
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.GetSessionKeyFromEncryptedMessage
import me.proton.core.drive.cryptobase.domain.usecase.VerifyData
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.key.domain.extension.publicKeyRing
import javax.inject.Inject

class ConvertExternalInvitation @Inject constructor(
    private val getExternalInvitationFlow: GetExternalInvitationsFlow,
    private val getShare: GetShare,
    private val getLink: GetLink,
    private val getAddressKeys: GetAddressKeys,
    private val getSessionKeyFromEncryptedMessage: GetSessionKeyFromEncryptedMessage,
    private val cryptoContext: CryptoContext,
    private val verifyData: VerifyData,
    private val createShareInvitation: CreateShareInvitation,
) {
    suspend operator fun invoke(linkId: LinkId, id: String) = coRunCatching {
        val link = getLink(linkId, refresh = flowOf(true)).toResult().getOrThrow()

        val shareId = checkNotNull(link.sharingDetails?.shareId) { "Link is not shared: $id" }

        val share = getShare(shareId).filterSuccessOrError().toResult().getOrThrow()

        val externalInvitation = getExternalInvitationFlow(
            shareId = shareId,
            refresh = flowOf(true)
        ).toResult()
            .getOrThrow()
            .firstOrNull { externalInvitee -> externalInvitee.id == id }

        checkNotNull(externalInvitation) { "External invitation is not found: $id" }

        check(externalInvitation.state == ShareUser.ExternalInvitee.State.USER_REGISTERED) {
            "External invitation is not user registered: $id"
        }

        share.verifySignature(
            signature = externalInvitation.signature
        )

        createShareInvitation(
            shareId = shareId,
            email = externalInvitation.email,
            permissions = externalInvitation.permissions,
            externalInvitationId = externalInvitation.id,
        ).toResult().getOrThrow()
    }

    private suspend fun Share.verifySignature(
        signature: String
    ): Boolean {
        val addressId = requireNotNull(addressId)
        val addressKeys = getAddressKeys(
            userId = id.userId,
            addressId = addressId
        )
        val sessionKey = getSessionKeyFromEncryptedMessage(
            decryptKey = addressKeys.keyHolder,
            message = passphrase
        ).getOrThrow()

        return verifyData(
            verifyKeyRing = addressKeys.keyHolder.publicKeyRing(cryptoContext),
            input = sessionKey.key,
            signature = signature
        ).getOrThrow()
    }
}
