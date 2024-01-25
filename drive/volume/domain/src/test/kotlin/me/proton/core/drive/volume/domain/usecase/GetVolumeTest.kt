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

package me.proton.core.drive.volume.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.data.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.resultValueOrThrow
import me.proton.core.drive.db.test.NullableVolumeEntity
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.get
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.routing
import me.proton.core.drive.volume.data.api.response.GetVolumeResponse
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetVolumeTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var getVolume: GetVolume

    @Before
    fun setUp() = runTest {
        driveRule.db.user {}
    }

    @Test
    fun remote() = runTest {

        driveRule.server.routing {
            get("/drive/volumes/{enc_volumeId}") {
                jsonResponse {
                    GetVolumeResponse(
                        code = ProtonApiCode.SUCCESS,
                        volumeDto = NullableVolumeDto(id = parameters.getValue("enc_volumeId"))
                    )
                }
            }
        }

        val id = VolumeId(volumeId)

        val volume = getVolume(userId, id).resultValueOrThrow()

        assertEquals(
            NullableVolume(id),
            volume
        )
    }

    @Test
    fun local() = runTest {

        driveRule.db.volumeDao.insertOrIgnore(
            NullableVolumeEntity()
        )

        val id = VolumeId(volumeId)

        val volume = getVolume(userId, id).resultValueOrThrow()

        assertEquals(
            NullableVolume(id),
            volume
        )
    }
}
