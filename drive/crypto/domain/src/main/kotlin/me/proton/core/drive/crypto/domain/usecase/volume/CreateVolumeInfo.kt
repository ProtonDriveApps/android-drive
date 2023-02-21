/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.crypto.domain.usecase.volume

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.GetAddressId
import me.proton.core.drive.cryptobase.domain.usecase.EncryptText
import me.proton.core.drive.cryptobase.domain.usecase.GenerateHashKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.extension.nodeKey
import me.proton.core.drive.key.domain.extension.nodePassphrase
import me.proton.core.drive.key.domain.extension.nodePassphraseSignature
import me.proton.core.drive.key.domain.usecase.GenerateNodeKey
import me.proton.core.drive.key.domain.usecase.GenerateShareKey
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.volume.domain.entity.VolumeInfo
import me.proton.core.drive.volume.domain.entity.VolumeInfo.Companion.DEFAULT_ROOT_FOLDER_NAME
import javax.inject.Inject

class CreateVolumeInfo @Inject constructor(
    private val getAddressId: GetAddressId,
    private val generateShareKey: GenerateShareKey,
    private val generateNodeKey: GenerateNodeKey,
    private val generateHashKey: GenerateHashKey,
    private val encryptText: EncryptText,
    private val getAddressKeys: GetAddressKeys,
) {
    suspend operator fun invoke(userId: UserId, signatureAddress: String): Result<VolumeInfo> =
        generateShareAndFolderKey(userId, signatureAddress).mapCatching { (shareKey, folderKey) ->
            val addressId = getAddressId(userId)
            VolumeInfo(
                addressId = addressId,
                shareKey = shareKey.nodeKey,
                sharePassphrase = shareKey.nodePassphrase,
                sharePassphraseSignature = shareKey.nodePassphraseSignature,
                folderName = encryptText(
                    encryptKey = shareKey.keyHolder,
                    text = DEFAULT_ROOT_FOLDER_NAME,
                    signKey = getAddressKeys(userId, addressId).keyHolder
                ).getOrThrow(),
                folderKey = folderKey.nodeKey,
                folderPassphrase = folderKey.nodePassphrase,
                folderPassphraseSignature = folderKey.nodePassphraseSignature,
                folderHashKey = generateHashKey(folderKey.keyHolder).getOrThrow()
            )
        }

    private suspend fun generateShareAndFolderKey(userId: UserId, signatureAddress: String): Result<Pair<Key.Node, Key.Node>> =
        generateShareKey(userId).mapCatching { shareKey ->
            shareKey to generateNodeKey(userId, shareKey, signatureAddress).getOrThrow()
        }
}
