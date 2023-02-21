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
package me.proton.core.drive.crypto.domain.usecase.upload

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.EncryptedSignature
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.usecase.EncryptFile
import me.proton.core.drive.cryptobase.domain.usecase.UnlockKey
import me.proton.core.drive.cryptobase.domain.usecase.UseSessionKey
import me.proton.core.drive.key.domain.entity.ContentKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.extension.keyHolder
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EncryptUploadBlocks @Inject constructor(
    private val encryptFile: EncryptFile,
    private val unlockKey: UnlockKey,
    private val useSessionKey: UseSessionKey,
    private val encryptedSignature: EncryptedSignature,
) {

    suspend operator fun <T> invoke(
        contentKey: ContentKey,
        encryptKey: Key.Node,
        signKey: Key,
        input: List<File>,
        output: List<File>,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecryptWithIO.coroutineContext,
        block: suspend (index: Int, rawBlock: File, encryptedBlock: File, encSignature: String) -> T,
    ): Result<List<T>> = coRunCatching(coroutineContext) {
        unlockKey(encryptKey.keyHolder) { unlockedSignatureEncryptionKey ->
            unlockKey(signKey.keyHolder) { unlockedFileSignKey ->
                useSessionKey(
                    decryptKey = contentKey.decryptKey.keyHolder,
                    encryptedKeyPacket = contentKey.encryptedKeyPacket,
                    coroutineContext = coroutineContext,
                ) { sessionKey ->
                    input.mapIndexed { index, input ->
                        if (input.length() == 0L) {
                            block(
                                index,
                                input,
                                output[index].apply { createNewFile() },
                                encryptedSignature(
                                    unlockedEncryptKey = unlockedSignatureEncryptionKey,
                                    unlockedSignKey = unlockedFileSignKey,
                                    input = ByteArray(size = 0),
                                    coroutineContext = coroutineContext,
                                ).getOrThrow()
                            )
                        } else {
                            block(
                                index,
                                input,
                                encryptFile(
                                    encryptKey = sessionKey,
                                    source = input,
                                    destination = output[index],
                                    coroutineContext = coroutineContext,
                                ).getOrThrow(),
                                encryptedSignature(
                                    unlockedEncryptKey = unlockedSignatureEncryptionKey,
                                    unlockedSignKey = unlockedFileSignKey,
                                    file = input,
                                    coroutineContext = coroutineContext,
                                ).getOrThrow()
                            )
                        }
                    }
                }.getOrThrow()
            }.getOrThrow()
        }.getOrThrow()
    }
}
