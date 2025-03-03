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
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.NullableAlbumPhotoListingEntity
import me.proton.core.drive.db.test.album
import me.proton.core.drive.db.test.albumListings
import me.proton.core.drive.db.test.albumPhotoListings
import me.proton.core.drive.db.test.photoShare
import me.proton.core.drive.db.test.photoShareId
import me.proton.core.drive.db.test.photoVolume
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.data.api.entity.AlbumPhotoListingDto
import me.proton.core.drive.photo.data.api.response.CreateAlbumResponse
import me.proton.core.drive.photo.data.api.response.GetAlbumListingsResponse
import me.proton.core.drive.photo.data.api.response.GetAlbumListingsResponse.AlbumListingsDto
import me.proton.core.drive.photo.data.api.response.GetAlbumPhotoListingResponse
import me.proton.core.drive.photo.data.db.entity.AlbumListingEntity
import me.proton.core.drive.photo.data.db.entity.AlbumPhotoListingEntity
import me.proton.core.drive.photo.domain.entity.AlbumInfo
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.photo.domain.entity.PhotoListing
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
                            NullableAlbumListingsDto(
                                linkId = "album-3",
                                lastActivityTime = 10,
                            ),
                            NullableAlbumListingsDto(
                                linkId = "album-4",
                                lastActivityTime = 15,
                            ),
                            NullableAlbumListingsDto(
                                linkId = "album-5",
                                lastActivityTime = 20,
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

    @Test
    fun `fetch album photo listings from BE`() = runTest {
        driveRule.server.routing {
            get("/drive/photos/volumes/{volumeID}/albums/{linkID}/children") {
                jsonResponse {
                    GetAlbumPhotoListingResponse(
                        code = ProtonApiCode.SUCCESS,
                        photos = listOf(
                            NullableAlbumPhotoListingDto(
                                linkId = "link-2",
                                captureTime = 2,
                                addedTime = 2,
                            ),
                            NullableAlbumPhotoListingDto(
                                linkId = "link-1",
                                captureTime = 1,
                                addedTime = 1,
                            ),
                        )
                    )
                }
            }
        }
        val (albumListings, _) = albumRepository.fetchAlbumPhotoListings(
            userId = userId,
            volumeId = volumeId,
            albumId = AlbumId(photoShareId, "album-id"),
            anchorId = null,
            sortingBy = PhotoListing.Album.SortBy.ADDED,
            sortingDirection = Direction.DESCENDING,
        )
        assertEquals(listOf("link-2", "link-1"), albumListings.map { albumPhotoListing -> albumPhotoListing.linkId.id })
    }

    @Test
    fun `fetch and store inserts album photo listings from BE into database`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {}
            }
        }
        driveRule.server.routing {
            get("/drive/photos/volumes/{volumeID}/albums/{linkID}/children") {
                jsonResponse {
                    GetAlbumPhotoListingResponse(
                        code = ProtonApiCode.SUCCESS,
                        photos = listOf(
                            NullableAlbumPhotoListingDto(
                                linkId = "link-2",
                                captureTime = 2,
                                addedTime = 2,
                            ),
                            NullableAlbumPhotoListingDto(
                                linkId = "link-1",
                                captureTime = 1,
                                addedTime = 1,
                            ),
                        )
                    )
                }
            }
        }
        albumRepository.fetchAndStoreAlbumPhotoListings(
            userId = userId,
            volumeId = photoVolumeId,
            shareId = photoShareId,
            albumId = AlbumId(photoShareId, "album-id"),
            anchorId = null,
            sortingBy = PhotoListing.Album.SortBy.ADDED,
            sortingDirection = Direction.DESCENDING,
        )
        val albumPhotoListingEntities = driveRule.db.albumPhotoListingDao.getAlbumPhotoListings(
            userId = userId,
            volumeId = photoVolumeId.id,
            albumId = "album-id",
            sortingBy = PhotoListing.Album.SortBy.ADDED,
            direction = Direction.ASCENDING,
            limit = 500,
            offset = 0,
        )

        assertEquals(listOf("link-1", "link-2"),albumPhotoListingEntities.map { it.linkId } )
    }

    @Test
    fun `getting album photo listings with ascending sorting on capture time`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {
                    album(id = "album-id") {
                        albumPhotoListings(
                            NullableAlbumPhotoListingEntity(linkId = "photo-1", captureTime = 1),
                            NullableAlbumPhotoListingEntity(linkId = "photo-2", captureTime = 3),
                            NullableAlbumPhotoListingEntity(linkId = "photo-3", captureTime = 2),
                        )
                    }
                }
            }
        }
        val albumPhotoListings = albumRepository.getAlbumPhotoListings(
            userId = userId,
            volumeId = photoVolumeId,
            albumId = AlbumId(photoShareId, "album-id"),
            fromIndex = 0,
            count = 500,
            sortingBy = PhotoListing.Album.SortBy.CAPTURED,
            sortingDirection = Direction.ASCENDING,
        )

        assertEquals(
            listOf("photo-1", "photo-3", "photo-2"),
            albumPhotoListings.map { albumPhotoListing -> albumPhotoListing.linkId.id }
        )
    }

    @Test
    fun `getting album photo listings with descending sorting on added time`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {
                    album(id = "album-1") {
                        albumPhotoListings(
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-1", captureTime = 1),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-2", captureTime = 3),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-3", captureTime = 2),
                        )
                    }
                    album(id = "album-2") {
                        albumPhotoListings(
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-1", addedTime = 3),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-2", addedTime = 1),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-3", addedTime = 4),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-4", addedTime = 2),
                        )
                    }
                }
            }
        }
        val albumPhotoListings = albumRepository.getAlbumPhotoListingsFlow(
            userId = userId,
            volumeId = photoVolumeId,
            albumId = AlbumId(photoShareId, "album-2"),
            fromIndex = 0,
            count = 500,
            sortingBy = PhotoListing.Album.SortBy.ADDED,
            sortingDirection = Direction.DESCENDING,
        ).first().getOrThrow()

        assertEquals(
            listOf("album-2-photo-3", "album-2-photo-1", "album-2-photo-4", "album-2-photo-2"),
            albumPhotoListings.map { albumPhotoListing -> albumPhotoListing.linkId.id }
        )
    }

    @Test
    fun `delete all album photo listings for specific album`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {
                    album(id = "album-1") {
                        albumPhotoListings(
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-1", captureTime = 1),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-2", captureTime = 3),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-3", captureTime = 2),
                        )
                    }
                    album(id = "album-2") {
                        albumPhotoListings(
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-1", addedTime = 3),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-2", addedTime = 1),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-3", addedTime = 4),
                            NullableAlbumPhotoListingEntity(linkId = "${this.link.id}-photo-4", addedTime = 2),
                        )
                    }
                }
            }
        }
        albumRepository.deleteAllAlbumPhotoListings(
            userId = userId,
            volumeId = photoVolumeId,
            albumId = AlbumId(photoShareId, "album-2")
        )
        val albumPhotoListings = driveRule.db.albumPhotoListingDao.getAlbumPhotoListings(
            userId = userId,
            volumeId = photoVolumeId.id,
            albumId = "album-2",
            sortingBy = PhotoListing.Album.SortBy.ADDED,
            direction = Direction.DESCENDING,
            limit = 500,
            offset = 0,
        )

        assertEquals(emptyList<AlbumPhotoListingEntity>(), albumPhotoListings)
    }

    @Test
    fun `insert only album photo listings which are not already in db`() = runTest {
        driveRule.db.user {
            photoVolume {
                photoShare {
                    album(id = "album-id") {
                        albumPhotoListings(
                            NullableAlbumPhotoListingEntity(linkId = "photo-1"),
                            NullableAlbumPhotoListingEntity(linkId = "photo-2"),
                            NullableAlbumPhotoListingEntity(linkId = "photo-3"),
                        )
                    }
                }
            }
        }
        albumRepository.insertOrIgnoreAlbumPhotoListing(
            volumeId = photoVolumeId,
            photoListings = listOf(
                PhotoListing.Album(
                    linkId = FileId(photoShareId, "photo-3"),
                    captureTime = TimestampS(0L),
                    nameHash = null,
                    contentHash = null,
                    albumId = AlbumId(photoShareId, "album-id"),
                    addedTime = TimestampS(0L),
                    isChildOfAlbum = false,
                ),
                PhotoListing.Album(
                    linkId = FileId(photoShareId, "photo-4"),
                    captureTime = TimestampS(0L),
                    nameHash = null,
                    contentHash = null,
                    albumId = AlbumId(photoShareId, "album-id"),
                    addedTime = TimestampS(0L),
                    isChildOfAlbum = false,
                ),
            )
        )
        val albumPhotoListings = driveRule.db.albumPhotoListingDao.getAlbumPhotoListings(
            userId = userId,
            volumeId = photoVolumeId.id,
            albumId = "album-id",
            sortingBy = PhotoListing.Album.SortBy.ADDED,
            direction = Direction.DESCENDING,
            limit = 500,
            offset = 0,
        )
        assertEquals(
            listOf("photo-1", "photo-2", "photo-3", "photo-4"),
            albumPhotoListings.map { entity -> entity.linkId }
        )
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

@Suppress("TestFunctionName")
internal fun NullableAlbumListingsDto(
    linkId: String,
    volumeId: String = photoVolumeId.id,
    shareId: String = photoShareId.id,
    locked: Boolean = false,
    coverLinkId: String? = null,
    lastActivityTime: Long = 0L,
    photoCount: Long = 0L,
) = AlbumListingsDto(
    linkId = linkId,
    volumeId = volumeId,
    shareId = shareId,
    locked = locked,
    coverLinkId = coverLinkId,
    lastActivityTime = lastActivityTime,
    photoCount = photoCount,
)

@Suppress("TestFunctionName")
internal fun  NullableAlbumPhotoListingDto(
    linkId: String,
    captureTime: Long = 0L,
    hash: String? = null,
    contentHash: String? = null,
    addedTime: Long = 0L,
    isChildOfAlbum: Boolean = false,
) =  AlbumPhotoListingDto(
    linkId = linkId,
    captureTime = captureTime,
    hash = hash,
    contentHash = contentHash,
    addedTime = addedTime,
    isChildOfAlbum = isChildOfAlbum,
)
