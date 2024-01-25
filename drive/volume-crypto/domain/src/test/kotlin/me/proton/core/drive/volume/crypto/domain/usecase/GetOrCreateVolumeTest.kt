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
import me.proton.core.drive.base.data.api.ProtonApiCode
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.resultValueOrThrow
import me.proton.core.drive.db.test.NullableVolumeEntity
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.db.test.withKey
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.get
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.post
import me.proton.core.drive.test.api.routing
import me.proton.core.drive.volume.data.api.response.GetVolumeResponse
import me.proton.core.drive.volume.data.api.response.GetVolumesResponse
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetOrCreateVolumeTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var createOrCreateVolume: GetOrCreateVolume


    @Test
    fun `get oldest volume from cache`() = runTest {
        driveRule.db.user {
            withKey()
            volume(NullableVolumeEntity(id = "oldest-active-volume", creationTime = 1))
            volume(NullableVolumeEntity(id = "newest-active-volume", creationTime = 2))
        }

        val volume = createOrCreateVolume(userId).resultValueOrThrow()

        assertEquals(
            NullableVolume(
                id = VolumeId("oldest-active-volume"),
                creationTime = TimestampS(1),
            ),
            volume,
        )
    }

    @Test
    fun `get volume from remote`() = runTest {
        driveRule.db.user {
            withKey()
        }
        driveRule.server.routing {
            get("/drive/volumes") {
                jsonResponse {
                    GetVolumesResponse(
                        code = ProtonApiCode.SUCCESS,
                        volumeDtos = listOf(NullableVolumeDto(id = "volume-id"))
                    )
                }
            }
        }

        val volume = createOrCreateVolume(userId).resultValueOrThrow()

        assertEquals(
            NullableVolume(VolumeId(volumeId)),
            volume
        )
    }

    @Test
    fun `create a volume if none exists`() = runTest {
        driveRule.db.user {
            withKey()
        }

        driveRule.server.routing {
            get("/drive/volumes") {
                jsonResponse {
                    GetVolumesResponse(
                        code = ProtonApiCode.SUCCESS,
                        volumeDtos = emptyList()
                    )
                }
            }
            post("/drive/volumes") {
                jsonResponse {
                    GetVolumeResponse(
                        code = ProtonApiCode.SUCCESS,
                        volumeDto = NullableVolumeDto(id = "volume-id")
                    )
                }
            }
        }

        val volume = createOrCreateVolume(userId).resultValueOrThrow()

        assertEquals(
            NullableVolume(VolumeId("volume-id")),
            volume
        )
    }

    @Test
    fun `create a new volume if no active volume exists`() = runTest {
        driveRule.db.user {
            withKey()
        }

        driveRule.server.routing {
            get("/drive/volumes") {
                jsonResponse {
                    GetVolumesResponse(
                        code = ProtonApiCode.SUCCESS,
                        volumeDtos = listOf(
                            NullableVolumeDto(
                                id = "inactive-volume-id",
                                state = 0
                            )
                        )
                    )
                }
            }
            post("/drive/volumes") {
                jsonResponse {
                    GetVolumeResponse(
                        code = ProtonApiCode.SUCCESS,
                        volumeDto = NullableVolumeDto(id = "active-volume-id")
                    )
                }
            }
        }

        val volume = createOrCreateVolume(userId).resultValueOrThrow()

        assertEquals(
            NullableVolume(VolumeId("active-volume-id")),
            volume
        )
    }

}
