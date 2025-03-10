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

package me.proton.core.drive.crypto.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.crypto.common.pgp.DecryptedData
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.exception.VerificationException
import me.proton.core.drive.cryptobase.domain.extension.failed
import me.proton.core.drive.cryptobase.domain.usecase.DecryptAndVerifyData
import me.proton.core.drive.cryptobase.domain.usecase.GetPublicKeyRing
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetContentKey
import me.proton.core.drive.key.domain.usecase.GetVerificationKeys
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.usecase.GetLink
import java.io.InputStream
import javax.inject.Inject

@ExperimentalCoroutinesApi
class DecryptThumbnail @Inject constructor(
    private val getLink: GetLink,
    private val getContentKey: GetContentKey,
    private val getVerificationKeys: GetVerificationKeys,
    private val decryptAndVerifyData: DecryptAndVerifyData,
    private val getPublicKeyRing: GetPublicKeyRing,
) {

    suspend operator fun invoke(
        fileId: FileId,
        inputStream: InputStream,
        checkSignature: Boolean = true,
    ): Result<DecryptedData> = coRunCatching {
        val file = getLink(fileId).toResult().getOrThrow()
        val nodeKey = getContentKey(file).getOrThrow()
        val verificationKeys = getVerificationKeys(fileId, file.uploadedBy).getOrThrow().keyHolder
        inputStream.use {
            decryptAndVerifyData(
                decryptKey = nodeKey.decryptKey.keyHolder,
                keyPacket = nodeKey.encryptedKeyPacket,
                verifyKeyRing = getPublicKeyRing(verificationKeys).getOrThrow(),
                data = inputStream.readBytes(),
                verificationFailedContext = javaClass.simpleName,
            ).getOrThrow().also { decryptedThumbnail ->
                if (decryptedThumbnail.status.failed && checkSignature) {
                    throw VerificationException(
                        message = "Verification status ${decryptedThumbnail.status.name} ${javaClass.simpleName}"
                    )
                }
            }
        }
    }
}
