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

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.GetSessionKeyFromEncryptedMessage
import me.proton.core.drive.cryptobase.domain.usecase.VerifyData
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.key.domain.extension.publicKeyRing
import javax.inject.Inject

class VerifyExternalInvitationSignature @Inject constructor(
    private val getShare: GetShare,
    private val getAddressKeys: GetAddressKeys,
    private val getSessionKeyFromEncryptedMessage: GetSessionKeyFromEncryptedMessage,
    private val cryptoContext: CryptoContext,
    private val verifyData: VerifyData,
) {

    suspend operator fun invoke(
        shareId: ShareId,
        signature: String
    ): Result<Boolean> = coRunCatching{
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

        verifyData(
            verifyKeyRing = addressKeys.keyHolder.publicKeyRing(cryptoContext),
            input = sessionKey.key,
            signature = signature
        ).getOrThrow()
    }
}
