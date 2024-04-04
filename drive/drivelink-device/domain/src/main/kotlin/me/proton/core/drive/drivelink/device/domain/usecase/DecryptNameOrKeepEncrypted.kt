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

package me.proton.core.drive.drivelink.device.domain.usecase

import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkName
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject

class DecryptNameOrKeepEncrypted @Inject constructor(
    private val decryptLinkName: DecryptLinkName,
    private val getLink: GetLink,
) {

    suspend operator fun invoke(device: Device) = device.decryptNameOrKeepEncrypted()

    private suspend fun Device.decryptNameOrKeepEncrypted(): Device =
        decryptDeviceName(this)
            .fold(
                onSuccess = { cryptoName ->
                    copy(cryptoName = cryptoName)
                },
                onFailure = {
                    this
                }
            )
    private suspend fun decryptDeviceName(device: Device): Result<CryptoProperty<String>> = coRunCatching {
        with (
            decryptLinkName(
                getLink(device.rootLinkId).toResult().getOrThrow()
            ).getOrThrow()
        ) {
            CryptoProperty.Decrypted(
                value = text,
                status = status,
            )
        }
    }
}
