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

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkName
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.usecase.GetDevices
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject

class GetDecryptedDevices @Inject constructor(
    private val getDevices: GetDevices,
    private val decryptLinkName: DecryptLinkName,
    private val getLink: GetLink,
) {

    operator fun invoke(userId: UserId): Flow<DataResult<List<Device>>> =
        getDevices(userId)
            .mapSuccess { devices ->
                devices.value.map { device ->
                    if (device.cryptoName is CryptoProperty.Decrypted) {
                        device
                    } else {
                        device.decryptNameOrKeepEncrypted()
                    }
                }.asSuccess
            }

    private suspend fun Device.decryptNameOrKeepEncrypted(): Device =
        decryptName()
            .fold(
                onSuccess = { cryptoName ->
                    copy(cryptoName = cryptoName)
                },
                onFailure = {
                    this
                }
            )
    private suspend fun Device.decryptName(): Result<CryptoProperty<String>> = coRunCatching {
        with (
            decryptLinkName(
                getLink(rootLinkId).toResult().getOrThrow()
            ).getOrThrow()
        ) {
            CryptoProperty.Decrypted(
                value = text,
                status = status,
            )
        }
    }
}
