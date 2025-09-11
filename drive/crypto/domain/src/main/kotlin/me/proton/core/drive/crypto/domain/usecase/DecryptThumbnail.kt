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
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.DecryptData
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetContentKey
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.usecase.GetLink
import java.io.InputStream
import javax.inject.Inject

@ExperimentalCoroutinesApi
class DecryptThumbnail @Inject constructor(
    private val getLink: GetLink,
    private val getContentKey: GetContentKey,
    private val decryptData: DecryptData,
) {

    suspend operator fun invoke(
        fileId: FileId,
        inputStream: InputStream,
    ): Result<ByteArray> = coRunCatching {
        val file = getLink(fileId).toResult().getOrThrow()
        val nodeKey = getContentKey(file).getOrThrow()
        inputStream.use {
            decryptData(
                decryptKey = nodeKey.decryptKey.keyHolder,
                keyPacket = nodeKey.encryptedKeyPacket,
                data = inputStream.readBytes(),
            ).getOrThrow()
        }
    }
}
