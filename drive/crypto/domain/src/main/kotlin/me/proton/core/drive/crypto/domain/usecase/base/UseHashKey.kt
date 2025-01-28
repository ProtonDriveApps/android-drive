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
package me.proton.core.drive.crypto.domain.usecase.base

import me.proton.core.crypto.common.pgp.HashKey
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.drive.base.domain.extension.resultValueOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.exception.VerificationException
import me.proton.core.drive.cryptobase.domain.usecase.UseHashKey
import me.proton.core.drive.key.domain.entity.NodeHashKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UseHashKey @Inject constructor(
    private val useHashKey: UseHashKey,
    private val getLink: GetLink,
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
) {
    suspend operator fun <T> invoke(
        nodeHashKey: NodeHashKey,
        checkSignature: Boolean = false,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
        block: suspend (HashKey) -> T,
    ): Result<T> =
        useHashKey(
            decryptKey = nodeHashKey.decryptKey.keyHolder,
            verifyKey = nodeHashKey.verifyKey.keyHolder,
            encryptedHashKey = nodeHashKey.encryptedHashKey,
            coroutineContext = coroutineContext,
        ) { hashKey ->
            if (hashKey.status != VerificationStatus.Success) {
                CoreLogger.w(LogTag.ENCRYPTION, "Verification of node hash key failed")
                if (checkSignature) {
                    throw VerificationException("Verification of node hash key failed")
                }
            }
            block(hashKey)
        }


    suspend operator fun <T> invoke(
        folderId: FolderId,
        checkSignature: Boolean = false,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
        block: suspend (HashKey) -> T,
    ): Result<T> {
        val folder = requireNotNull(getLink(folderId).resultValueOrNull()) {
            "Cannot found folder: $folderId"
        }
        return invoke(folder, checkSignature, coroutineContext, block)
    }

    suspend operator fun <T> invoke(
        folder: Link.Folder,
        checkSignature: Boolean = false,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
        block: suspend (HashKey) -> T,
    ) : Result<T> {
        val folderKey = getNodeKey(folder).getOrThrow()
        val folderHashKey = getNodeHashKey(folder, folderKey).getOrThrow()
        return invoke(folderHashKey, checkSignature, coroutineContext, block)
    }
}
