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
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.ENCRYPTION
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.usecase.DecryptText
import me.proton.core.drive.cryptobase.domain.usecase.UnlockKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetSharePrivateKey
import me.proton.core.drive.share.user.domain.entity.UserInvitation
import me.proton.core.drive.share.user.domain.entity.UserInvitationDetails
import me.proton.core.drive.share.user.domain.repository.UserInvitationRepository
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class GetDecryptedUserInvitationsFlow @Inject constructor(
    private val repository: UserInvitationRepository,
    private val getUserInvitationsFlow: GetUserInvitationsFlow,
    private val unlockKey: UnlockKey,
    private val decryptText: DecryptText,
    private val getSharePrivateKey: GetSharePrivateKey,
) {
    operator fun invoke(
        userId: UserId,
        refresh: Flow<Boolean> = flowOf { !repository.hasInvitations(userId) },
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Flow<DataResult<List<UserInvitation>>> =
        getUserInvitationsFlow(userId, refresh).mapSuccess { (_, invitations) ->
            invitations.map { invitation ->
                invitation.details?.let { details ->
                    details.decryptLinkName(userId, coroutineContext)
                        .getOrNull(ENCRYPTION, "Cannot decrypt invitation file name")
                        ?.let { decrypted -> invitation.copy(details = decrypted) }
                } ?: invitation
            }.asSuccess
        }

    private suspend fun UserInvitationDetails.decryptLinkName(
        userId: UserId,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ) = coRunCatching {
        val privateShareKey = getSharePrivateKey(userId).getOrThrow()
        val name = unlockKey(privateShareKey.keyHolder) { unlockedKey ->
            decryptText(
                unlockedKey = unlockedKey,
                text = cryptoName.value,
                coroutineContext = coroutineContext,
            ).getOrThrow()
        }.getOrThrow()
        copy(cryptoName = CryptoProperty.Decrypted(name, VerificationStatus.Unknown))
    }

    private suspend fun UserInvitationDetails.getSharePrivateKey(
        userId: UserId
    ) = coRunCatching {
        getSharePrivateKey(
            userId = userId,
            email = inviteeEmail,
            shareKey = shareKey,
            passphrase = passphrase,
        ).getOrThrow()
    }

}
