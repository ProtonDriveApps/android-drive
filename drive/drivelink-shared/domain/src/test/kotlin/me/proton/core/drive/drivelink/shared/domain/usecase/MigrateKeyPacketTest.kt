/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.drivelink.shared.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.api.ProtonApiCode.ENCRYPTION_VERIFICATION_FAILED
import me.proton.core.drive.db.test.mainRootId
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.standardRootId
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.data.api.request.GetLinksRequest
import me.proton.core.drive.link.data.api.response.GetLinksResponse
import me.proton.core.drive.share.data.api.request.ShareAccessWithNodeRequest
import me.proton.core.drive.share.data.api.response.UpdateUnmigratedSharesError
import me.proton.core.drive.share.data.api.response.UpdateUnmigratedSharesResponse
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.getLinks
import me.proton.core.drive.test.api.getPublicAddressKeysAll
import me.proton.core.drive.test.api.getShare
import me.proton.core.drive.test.api.getShareBootstrap
import me.proton.core.drive.test.api.getUnmigratedShares
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.post
import me.proton.core.drive.test.api.request
import me.proton.core.drive.test.api.response
import me.proton.core.drive.test.api.routing
import me.proton.core.drive.test.api.updateUnmigratedShares
import me.proton.core.drive.test.entity.NullableFileDto
import me.proton.core.drive.test.entity.NullableFolderDto
import me.proton.core.drive.test.entity.NullableShareDto
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MigrateKeyPacketTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var migrateKeyPacket: MigrateKeyPacket

    private val standardShareId = standardShareId()
    private val standardRootId = standardRootId()

    @Before
    fun setUp() = runTest {
        driveRule.db.photo { }
        driveRule.server.run {
            getPublicAddressKeysAll()
        }
    }

    @Test
    fun empty() {
        runTest {
            driveRule.server.run {
                getShare(listOf(NullableShareDto(standardShareId.id)))
                getUnmigratedShares(listOf())
            }

            migrateKeyPacket(userId).getOrThrow()
        }
    }

    @Test
    fun notFound() = runTest {
        driveRule.server.run {
            getShare(listOf(NullableShareDto(standardShareId.id)))
            getUnmigratedShares { response(404) }
        }

        migrateKeyPacket(userId).getOrThrow()
    }

    @Test
    fun migration() = runTest {
        driveRule.server.run {
            getShare(listOf(NullableShareDto(standardShareId.id)))
            getShareBootstrap("address-id-user-id@proton.test")
            getUnmigratedShares(listOf(standardShareId.id))
            getLinks {
                jsonResponse {
                    GetLinksResponse(
                        code = 1000,
                        links = listOf(
                            NullableFileDto(
                                id = request<GetLinksRequest>().linkIds.first(),
                                parentId = "folder-1",
                                shareId = standardShareId.id,
                            )
                        ),
                        parents = listOf(
                            NullableFolderDto(
                                id = "folder-1",
                                parentId = mainRootId.id,
                                shareId = null,
                            ),
                            NullableFolderDto(
                                id = mainRootId.id,
                                parentId = null,
                                shareId = mainShareId.id,
                            )
                        ),
                    )
                }
            }
            updateUnmigratedShares()
        }
        migrateKeyPacket(userId).getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun error() = runTest {
        driveRule.server.run {
            getUnmigratedShares(listOf(standardShareId.id))
            getShareBootstrap("address-id-user-id@proton.test")
            getShare(listOf(NullableShareDto(standardShareId.id)))
            getLinks {
                jsonResponse {
                    GetLinksResponse(
                        code = 1000,
                        links = listOf(
                            NullableFileDto(
                                id = request<GetLinksRequest>().linkIds.first(),
                                parentId = "folder-1",
                                shareId = standardShareId.id,
                            )
                        ),
                        parents = listOf(
                            NullableFolderDto(
                                id = "folder-1",
                                parentId = mainRootId.id,
                                shareId = null,
                            ),
                            NullableFolderDto(
                                id = mainRootId.id,
                                parentId = null,
                                shareId = mainShareId.id,
                            )
                        ),
                    )
                }
            }
            routing {
                post("/drive/migrations/shareaccesswithnode") {
                    jsonResponse(200) {
                        UpdateUnmigratedSharesResponse(
                            code = 1000,
                            shareIds = emptyList(),
                            errors = request<ShareAccessWithNodeRequest>().passphraseNodeKeyPackets.map {
                                UpdateUnmigratedSharesError(
                                    code = ENCRYPTION_VERIFICATION_FAILED.toLong(),
                                    shareId = it.shareId,
                                    error = "Invalid Key Packet"
                                )
                            },
                        )
                    }
                }
            }
        }

        migrateKeyPacket(userId).getOrThrow()
    }
}
