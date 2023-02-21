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
package me.proton.core.drive.crypto.domain.usecase.link

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.cryptobase.domain.usecase.EncryptAndSignTextWithCompression
import me.proton.core.drive.file.base.domain.entity.XAttr
import me.proton.core.drive.file.base.domain.extension.asJson
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import javax.inject.Inject

class EncryptAndSignXAttr @Inject constructor(
    private val getAddressKeys: GetAddressKeys,
    private val encryptAndSignTextWithCompression: EncryptAndSignTextWithCompression,
) {
    suspend operator fun invoke(
        userId: UserId,
        encryptKey: Key.Node,
        signatureAddress: String,
        xAttr: XAttr,
    ) =
        encryptAndSignTextWithCompression(
            encryptKey = encryptKey.keyHolder,
            signKey = getAddressKeys(userId, signatureAddress).keyHolder,
            text = xAttr.asJson()
        )
}
