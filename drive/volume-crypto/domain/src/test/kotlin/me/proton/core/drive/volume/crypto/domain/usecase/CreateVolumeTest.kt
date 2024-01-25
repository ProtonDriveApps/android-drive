/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.volume.crypto.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.drive.base.data.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.resultValueOrThrow
import me.proton.core.drive.db.test.NullableAddressKeyEntity
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.withKey
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.post
import me.proton.core.drive.test.api.routing
import me.proton.core.drive.volume.data.api.response.GetVolumeResponse
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CreateVolumeTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var createVolume: CreateVolume

    @Test
    fun `happy path`() = runTest {
        driveRule.db.user {
            withKey()
        }

        driveRule.server.routing {
            post("/drive/volumes") {
                jsonResponse {
                    GetVolumeResponse(
                        code = ProtonApiCode.SUCCESS,
                        volumeDto = NullableVolumeDto(id = "volume-id")
                    )
                }
            }
        }

        val volume = createVolume(userId).resultValueOrThrow()

        assertEquals(
            NullableVolume(VolumeId("volume-id")),
            volume
        )
    }

    @Test(expected = CryptoException::class)
    fun `fails with inactive key`() = runTest {
        driveRule.db.user {
            withKey(addressKey = NullableAddressKeyEntity(active = false))
        }

        createVolume(userId).resultValueOrThrow()
    }

}
