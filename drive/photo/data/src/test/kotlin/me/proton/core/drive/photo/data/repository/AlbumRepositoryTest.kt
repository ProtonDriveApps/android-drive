/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.photo.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.db.test.albumListings
import me.proton.core.drive.db.test.photoShare
import me.proton.core.drive.db.test.photoShareId
import me.proton.core.drive.db.test.photoVolume
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.photo.data.api.response.CreateAlbumResponse
import me.proton.core.drive.photo.data.api.response.GetAlbumListingsResponse
import me.proton.core.drive.photo.data.api.response.GetAlbumListingsResponse.AlbumListingsDto
import me.proton.core.drive.photo.data.db.entity.AlbumListingEntity
import me.proton.core.drive.photo.domain.entity.AlbumInfo
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.photo.domain.entity.UpdateAlbumInfo
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.get
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.post
import me.proton.core.drive.test.api.put
import me.proton.core.drive.test.api.routing
import me.proton.core.network.data.protonApi.ProtonErrorData
import me.proton.core.network.domain.ApiException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class AlbumRepositoryTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject lateinit var albumRepository: AlbumRepository

    @Test
    fun `successful creation of album`() = runTest {
        driveRule.server.routing {
            post("/drive/photos/volumes/{volumeID}/albums") {
                jsonResponse {
                    CreateAlbumResponse(
                        code = ProtonApiCode.SUCCESS,
                        album = CreateAlbumResponse.AlbumDto(
                            link = CreateAlbumResponse.LinkDto(
                                linkId = "album-id",
                            )
                        )
                    )
                }
            }
        }

        val albumId = albumRepository.createAlbum(userId, photoVolumeId, NullableAlbumInfo())

        assertEquals("album-id", albumId)
    }

    @Test(expected = ApiException::class)
    fun `creating album on a regular volume fails`() = runTest {
        driveRule.server.routing {
            post("/drive/photos/volumes/{volumeID}/albums") {
                jsonResponse(status = 422) {
                    ProtonErrorData(
                        code = ProtonApiCode.NOT_EXISTS,
                        error = "A photo share does not exist for this volume",
                    )
                }
            }
        }

        albumRepository.createAlbum(userId, volumeId, NullableAlbumInfo())
    }

    @Test
    fun `successful album name update`() = runTest {
        driveRule.server.routing {
            put("/drive/photos/volumes/{volumeID}/albums/{linkID}") {
                jsonResponse {
                    CodeResponse(
                        code = ProtonApiCode.SUCCESS.toInt(),
                    )
                }
            }
        }

        albumRepository.updateAlbum(
            volumeId = photoVolumeId,
            albumId = AlbumId(photoShareId, "album-id"),
            updateAlbumInfo = UpdateAlbumInfo(
                name = "new-encrypted-album-name",
            ),
        )
    }

    @Test(expected = ApiException::class)
    fun `updating album without sufficient permissions fails`() = runTest {
        driveRule.server.routing {
            put("/drive/photos/volumes/{volumeID}/albums/{linkID}") {
                jsonResponse(status = 422) {
                    ProtonErrorData(
                        code = ProtonApiCode.NOT_ALLOWED,
                        error = "Insufficient permissions",
                    )
                }
            }
        }

        albumRepository.updateAlbum(
            volumeId = photoVolumeId,
            albumId = AlbumId(photoShareId, "album-id"),
            updateAlbumInfo = UpdateAlbumInfo(
                name = "new-encrypted-album-name",
            ),
        )
    }

    @Test
    fun `get album listing provides data from database properly sorted by last activity time`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {
                    albumListings(
                        NullableAlbumListingEntity(
                            albumId = "album-1",
                            lastActivityTime = 20,
                        ),
                        NullableAlbumListingEntity(
                            albumId = "album-2",
                            lastActivityTime = 10,
                        ),
                        NullableAlbumListingEntity(
                            albumId = "album-3",
                            lastActivityTime = 15,
                        ),
                    )
                }
            }
        }

        val albumListings = albumRepository.getAlbumListings(
            userId = userId,
            volumeId = photoVolumeId,
            fromIndex = 0,
            count = 500,
        )

        assertEquals(listOf("album-1", "album-3", "album-2"), albumListings.map { it.albumId.id })
    }

    @Test
    fun `get album listing flow provides data from database properly sorted by last activity time`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {
                    albumListings(
                        NullableAlbumListingEntity(
                            albumId = "album-1",
                            lastActivityTime = 20,
                        ),
                        NullableAlbumListingEntity(
                            albumId = "album-2",
                            lastActivityTime = 10,
                        ),
                        NullableAlbumListingEntity(
                            albumId = "album-3",
                            lastActivityTime = 15,
                        ),
                    )
                }
            }
        }

        val albumListings = albumRepository.getAlbumListingsFlow(
            userId = userId,
            volumeId = photoVolumeId,
            fromIndex = 0,
            count = 500,
        ).first().getOrThrow()

        assertEquals(listOf("album-1", "album-3", "album-2"), albumListings.map { it.albumId.id })
    }

    @Test
    fun `getting album listings with ascending sorting on last activity time`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {
                    albumListings(
                        NullableAlbumListingEntity(
                            albumId = "album-1",
                            lastActivityTime = 20,
                        ),
                        NullableAlbumListingEntity(
                            albumId = "album-2",
                            lastActivityTime = 10,
                        ),
                        NullableAlbumListingEntity(
                            albumId = "album-3",
                            lastActivityTime = 15,
                        ),
                    )
                }
            }
        }

        val albumListings = albumRepository.getAlbumListings(
            userId = userId,
            volumeId = photoVolumeId,
            sortingDirection = Direction.ASCENDING,
            fromIndex = 0,
            count = 500,
        )

        assertEquals(listOf("album-2", "album-3", "album-1"), albumListings.map { it.albumId.id })
    }

    @Test
    fun `delete all album listings`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {
                    albumListings(
                        NullableAlbumListingEntity(
                            albumId = "album-1",
                            lastActivityTime = 20,
                        ),
                        NullableAlbumListingEntity(
                            albumId = "album-2",
                            lastActivityTime = 10,
                        ),
                    )
                }
            }
        }

        albumRepository.deleteAll(
            userId = userId,
            volumeId = photoVolumeId,
        )
        val albumListings = albumRepository.getAlbumListings(
            userId = userId,
            volumeId = photoVolumeId,
            fromIndex = 0,
            count = 500,
        )

        assertEquals(emptyList<AlbumListing>(), albumListings)
    }

    @Test
    fun `fetch and store clears db table and then inserts album listings from BE`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {
                    albumListings(
                        NullableAlbumListingEntity(
                            albumId = "album-1",
                            lastActivityTime = 20,
                        ),
                        NullableAlbumListingEntity(
                            albumId = "album-2",
                            lastActivityTime = 10,
                        ),
                    )
                }
            }
        }
        driveRule.server.routing {
            get("/drive/photos/volumes/{volumeID}/albums") {
                jsonResponse {
                    GetAlbumListingsResponse(
                        code = ProtonApiCode.SUCCESS,
                        anchorId = null,
                        more = false,
                        albums = listOf(
                            AlbumListingsDto(
                                linkId = "album-3",
                                volumeId = photoVolumeId.id,
                                shareId = photoShareId.id,
                                locked = false,
                                coverLinkId = null,
                                lastActivityTime = 10,
                                photoCount = 0L,
                            ),
                            AlbumListingsDto(
                                linkId = "album-4",
                                volumeId = photoVolumeId.id,
                                shareId = photoShareId.id,
                                locked = false,
                                coverLinkId = null,
                                lastActivityTime = 15,
                                photoCount = 0L,
                            ),
                            AlbumListingsDto(
                                linkId = "album-5",
                                volumeId = photoVolumeId.id,
                                shareId = photoShareId.id,
                                locked = false,
                                coverLinkId = null,
                                lastActivityTime = 20,
                                photoCount = 0L,
                            ),
                        )
                    )
                }
            }
        }

        albumRepository.fetchAndStoreAllAlbumListings(
            userId = userId,
            volumeId = photoVolumeId,
            shareId = photoShareId,
        )

        val albumListings = albumRepository.getAlbumListings(
            userId = userId,
            volumeId = photoVolumeId,
            fromIndex = 0,
            count = 500,
        )

        assertEquals(listOf("album-5", "album-4", "album-3"), albumListings.map { it.albumId.id })
    }
}

@Suppress("TestFunctionName")
internal fun NullableAlbumListingEntity(
    albumId: String,
    userId: UserId = me.proton.core.drive.db.test.userId,
    volumeId: String = photoVolumeId.id,
    shareId: String = photoShareId.id,
    locked: Boolean = false,
    photoCount: Long = 0,
    lastActivityTime: Long = 0,
    coverLinkId: String? = null,
    isShared: Boolean = false,
) = AlbumListingEntity(
    userId = userId,
    volumeId = volumeId,
    shareId = shareId,
    albumId = albumId,
    locked = locked,
    photoCount = photoCount,
    lastActivityTime = lastActivityTime,
    coverLinkId = coverLinkId,
    isShared = isShared,
)

@Suppress("TestFunctionName")
internal fun NullableAlbumInfo(
    name: String = "encrypted-album-name",
    hash: String = "name-hash",
    nodeKey: String = "album-node-key",
    nodePassphrase: String = "album-node-passphrase",
    nodePassphraseSignature: String = "album-node-passphrase-signature",
    nodeHashKey: String = "album-node-hash-key",
    signatureEmail: String = "test@proton.me",
    xAttr: String = "",
    isLocked: Boolean = false,
) = AlbumInfo(
    name = name,
    hash = hash,
    nodeKey = nodeKey,
    nodePassphrase = nodePassphrase,
    nodePassphraseSignature = nodePassphraseSignature,
    nodeHashKey = nodeHashKey,
    signatureEmail = signatureEmail,
    xAttr = xAttr,
    isLocked = isLocked,
)
