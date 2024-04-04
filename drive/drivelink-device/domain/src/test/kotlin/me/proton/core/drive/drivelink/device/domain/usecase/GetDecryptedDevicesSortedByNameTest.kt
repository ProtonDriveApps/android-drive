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

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.device.domain.extension.name
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.test.kotlin.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetDecryptedDevicesSortedByNameTest  {
    private val userId = UserId("userId")
    private val getDecryptedDevices = mockk<GetDecryptedDevices>()

    @Test
    fun test() = runTest {
        // Given
        coEvery { getDecryptedDevices(userId) } returns flowOf<DataResult<List<Device>>>(
            DataResult.Success(
                source = ResponseSource.Local,
                value = listOf(
                    device,
                    device.copy(
                        cryptoName = CryptoProperty.Decrypted("MacBook Air", VerificationStatus.Unknown),
                        type = Device.Type.MAC_OS,
                    ),
                    device.copy(
                        cryptoName = CryptoProperty.Decrypted("HP zBook", VerificationStatus.Unknown),
                    ),

                    device.copy(
                        cryptoName = CryptoProperty.Decrypted("NAS", VerificationStatus.Unknown),
                        type = Device.Type.LINUX,
                    ),
                ),
            )
        )
        val getDecryptedDevicesSortedByName = GetDecryptedDevicesSortedByName(getDecryptedDevices)

        // When
        val devices = (getDecryptedDevicesSortedByName(userId).first() as DataResult.Success).value

        // Then
        val expected = listOf("HP zBook", "MacBook Air", "NAS", "e_n_c_r_y_p_t_e_d")
        devices.forEachIndexed { index, device ->
            assertEquals(expected[index], device.name) { "" }
        }
    }

    private val device = Device(
        id = DeviceId("deviceId"),
        volumeId = VolumeId("volumeId"),
        rootLinkId = FolderId(ShareId(userId, "shareId"), "linkId"),
        type = Device.Type.WINDOWS,
        syncState = Device.SyncState.ON,
        creationTime = TimestampS(),
        cryptoName = CryptoProperty.Encrypted("e_n_c_r_y_p_t_e_d"),
    )
}
