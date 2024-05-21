/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.key.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.usecase.GetSignatureAddress
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.DecryptNestedPrivateKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.ShareKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.extension.nestedPrivateKey
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

/**
 * Builds [Key] by decrypting [Share] key, passphrase and passphrase signature
 */
class BuildShareKey @Inject constructor(
    private val decryptNestedPrivateKey: DecryptNestedPrivateKey,
    private val getAddressKeys: GetAddressKeys,
    private val getShare: GetShare,
    private val getSignatureAddress: GetSignatureAddress,
    private val getPublicAddressKeys: GetPublicAddressKeys,
) {
    suspend operator fun invoke(share: Share): Result<Key.Node> = coRunCatching {
        val userId = share.id.userId
        ShareKey(
            key = decryptNestedPrivateKey(
                decryptKey = getAddressKeys(userId, requireNotNull(share.addressId)).keyHolder,
                key = share.nestedPrivateKey,
                verifySignatureKey = getPublicAddressKeys(
                    userId = userId,
                    email = getSignatureAddress(userId, requireNotNull(share.addressId)),
                ).getOrThrow().keyHolder,
                allowCompromisedVerificationKeys = true,
            ).getOrThrow()
        )
    }

    suspend operator fun invoke(shareId: ShareId) = coRunCatching {
        invoke(
            getShare(shareId).toResult().getOrThrow(),
        ).getOrThrow()
    }
}
