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
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.resultValueOrThrow
import me.proton.core.drive.db.test.NullableVolumeEntity
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.get
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.routing
import me.proton.core.drive.volume.data.api.response.GetVolumesResponse
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
class GetVolumesTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var getVolumes: GetVolumes

    @Before
    fun setUp() = runTest {
        driveRule.db.user {}
    }

    @Test
    fun remote() = runTest {

        driveRule.server.routing {
            get("/drive/volumes") {
                jsonResponse {
                    GetVolumesResponse(
                        code = ProtonApiCode.SUCCESS,
                        volumeDtos = listOf(
                            NullableVolumeDto("volume-1"),
                            NullableVolumeDto("volume-2"),
                        )
                    )
                }
            }
        }

        val volume = getVolumes(userId).resultValueOrThrow()

        assertEquals(
            listOf(
                NullableVolume(VolumeId("volume-1")),
                NullableVolume(VolumeId("volume-2")),
            ),
            volume
        )
    }

    @Test
    fun local() = runTest {

        driveRule.db.volumeDao.insertOrIgnore(
            NullableVolumeEntity("volume-1"),
            NullableVolumeEntity("volume-2"),
        )

        val volumes = getVolumes(userId).resultValueOrThrow()

        assertEquals(
            listOf(
                NullableVolume(VolumeId("volume-1")),
                NullableVolume(VolumeId("volume-2")),
            ),
            volumes
        )
    }
}
