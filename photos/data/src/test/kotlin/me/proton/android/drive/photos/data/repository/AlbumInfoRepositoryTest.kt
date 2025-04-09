/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.data.repository

import androidx.datastore.preferences.core.edit
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.photos.data.extension.toAddToAlbumEntity
import me.proton.android.drive.photos.data.extension.toPhotoListing
import me.proton.android.drive.photos.domain.repository.AlbumInfoRepository
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.core.drive.db.test.NullableLinkEntity
import me.proton.core.drive.db.test.album
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photoRootId
import me.proton.core.drive.db.test.photoShare
import me.proton.core.drive.db.test.photoShareId
import me.proton.core.drive.db.test.photoVolume
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.test.DriveRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class AlbumInfoRepositoryTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Inject lateinit var albumInfoRepository: AlbumInfoRepository
    @Inject lateinit var getUserDataStore: GetUserDataStore
    private val storageLocationProvider = mockk<StorageLocationProvider>()

    @Before
    fun before() = runTest {
        val permanentFolder = tempFolder.newFolder()
        coEvery { storageLocationProvider.getPermanentFolder(any(), any()) } returns permanentFolder
    }

    @Test
    fun empty() = runTest {
        // When
        val albumName = albumInfoRepository.getName(userId)

        // Then
        assertNull(albumName)
    }

    @Test
    fun emptyAddToAlbumCount() = runTest {
        // When
        val count = albumInfoRepository.getPhotoListingsCount(userId, null).first()

        // Then
        assertEquals(0, count)
    }

    @Test
    fun nonEmptyAddToAlbumCount() = runTest {
        // Given
        driveRule.db.user {
            photoVolume {
                photoShare {
                    file("photo-id-1")
                    file("photo-id-2")
                }
            }
        }
        val volumePhotoListings = listOf(
            NullableVolumePhotoListing(
                photoId = "photo-id-1",
                captureTime = TimestampS(1),
            ),
            NullableVolumePhotoListing(
                photoId = "photo-id-2",
                captureTime = TimestampS(2),
            ),
        )
        albumInfoRepository.addPhotoListings(photoListings = volumePhotoListings.toTypedArray())

        // When
        val count = albumInfoRepository.getPhotoListingsCount(userId, null).first()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `if new album name has been set getName should return it`() = runTest {
        // Given
        val myAlbum = "My album"
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.newAlbumName] = myAlbum
        }

        // When
        val albumName = albumInfoRepository.getName(userId)

        // Then
        assertEquals(
            myAlbum,
            albumName,
        )
    }

    @Test
    fun `if new album name has been updated getName should return it`() = runTest {
        // Given
        val myAlbum = "My album"
        val newAlbumName = "New album name"
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.newAlbumName] = myAlbum
        }

        // When
        albumInfoRepository.updateName(userId, newAlbumName)
        val albumName = albumInfoRepository.getName(userId)

        // Then
        assertEquals(
            newAlbumName,
            albumName,
        )
    }

    @Test
    fun `add volume photo listings into new album`() = runTest {
        // Given
        driveRule.db.user {
            photoVolume {
                photoShare {
                    file("photo-id-1")
                    file("photo-id-2")
                }
            }
        }
        val volumePhotoListings = listOf(
            NullableVolumePhotoListing(
                photoId = "photo-id-1",
                captureTime = TimestampS(1),
            ),
            NullableVolumePhotoListing(
                photoId = "photo-id-2",
                captureTime = TimestampS(2),
            ),
        )

        // When
        albumInfoRepository.addPhotoListings(photoListings = volumePhotoListings.toTypedArray())

        val photoListings = driveRule
            .db
            .addToAlbumDao
            .getPhotoListings(userId, 500, 0)
            .map { addToAlbumEntity -> addToAlbumEntity.toPhotoListing() }

        // Then
        assertEquals(
            listOf("photo-id-2", "photo-id-1"),
            photoListings.map { photoListing -> photoListing.linkId.id },
        )
    }

    @Test
    fun `add album photo listings into new album`() = runTest {
        // Given
        driveRule.db.user {
            photoVolume {
                photoShare {
                    file("photo-id-1")
                    file("photo-id-2")
                    album("album-id") {}
                }
            }
        }
        val albumPhotoListings = listOf(
            NullableAlbumPhotoListing(
                albumId = "album-id",
                photoId = "photo-id-1",
                captureTime = TimestampS(100),
            ),
            NullableAlbumPhotoListing(
                albumId = "album-id",
                photoId = "photo-id-2",
                captureTime = TimestampS(10),
            ),
        )

        // When
        albumInfoRepository.addPhotoListings(photoListings = albumPhotoListings.toTypedArray())

        val photoListings = driveRule
            .db
            .addToAlbumDao
            .getPhotoListings(userId, 500, 0)
            .map { addToAlbumEntity -> addToAlbumEntity.toPhotoListing() }

        // Then
        assertEquals(
            listOf("photo-id-1", "photo-id-2"),
            photoListings.map { photoListing -> photoListing.linkId.id },
        )
    }

    @Test
    fun `remove volume photo listings from new album`() = runTest {
        // Given
        driveRule.db.user {
            photoVolume {
                photoShare {
                    file("photo-id-1")
                    file("photo-id-2")
                }
            }
        }
        val volumePhotoListings = listOf(
            NullableVolumePhotoListing(
                photoId = "photo-id-1",
                captureTime = TimestampS(1),
            ),
            NullableVolumePhotoListing(
                photoId = "photo-id-2",
                captureTime = TimestampS(2),
            ),
        )
        driveRule.db.addToAlbumDao.insertOrIgnore(
            *volumePhotoListings
                .map { photoListing -> photoListing.toAddToAlbumEntity(null) }
                .toTypedArray()
        )

        // When
        albumInfoRepository.removePhotoListings(photoListings = volumePhotoListings.toTypedArray())
        val photoListings = driveRule
            .db
            .addToAlbumDao
            .getPhotoListings(userId, 500, 0)
            .map { addToAlbumEntity -> addToAlbumEntity.toPhotoListing() }

        // Then
        assertEquals(
            emptyList<PhotoListing.Volume>(),
            photoListings
        )
    }

    @Test
    fun `remove album photo listings from new album`() = runTest {
        // Given
        driveRule.db.user {
            photoVolume {
                photoShare {
                    file("photo-id-1")
                    file("photo-id-2")
                    album("album-id") {}
                }
            }
        }
        val albumPhotoListings = listOf(
            NullableAlbumPhotoListing(
                albumId = "album-id",
                photoId = "photo-id-1",
                captureTime = TimestampS(100),
            ),
            NullableAlbumPhotoListing(
                albumId = "album-id",
                photoId = "photo-id-2",
                captureTime = TimestampS(10),
            ),
        )
        driveRule.db.addToAlbumDao.insertOrIgnore(
            *albumPhotoListings
                .map { photoListing -> photoListing.toAddToAlbumEntity(null) }
                .toTypedArray()
        )

        // When
        albumInfoRepository.removePhotoListings(photoListings = albumPhotoListings.toTypedArray())

        val photoListings = driveRule
            .db
            .addToAlbumDao
            .getPhotoListings(userId,500, 0)
            .map { addToAlbumEntity -> addToAlbumEntity.toPhotoListing() }

        // Then
        assertEquals(
            emptyList<PhotoListing.Album>(),
            photoListings,
        )
    }

    @Test
    fun `remove all album photo listings from new album`() = runTest {
        // Given
        driveRule.db.user {
            photoVolume {
                photoShare {
                    file("photo-id-1")
                    file("photo-id-2")
                    album("album-id") {}
                }
            }
        }
        val albumPhotoListings = listOf(
            NullableAlbumPhotoListing(
                albumId = "album-id",
                photoId = "photo-id-1",
                captureTime = TimestampS(100),
            ),
            NullableAlbumPhotoListing(
                albumId = "album-id",
                photoId = "photo-id-2",
                captureTime = TimestampS(10),
            ),
        )
        driveRule.db.addToAlbumDao.insertOrIgnore(
            *albumPhotoListings
                .map { photoListing -> photoListing.toAddToAlbumEntity(null) }
                .toTypedArray()
        )

        // When
        albumInfoRepository.removeAllPhotoListings(userId)

        val photoListingsCount = driveRule
            .db
            .addToAlbumDao
            .getPhotoListingsCount(userId)
            .first()

        // Then
        assertEquals(
            0,
            photoListingsCount,
        )
    }

    @Test
    fun `get photo listings returns all photo listings`() = runTest {
        // Given
        driveRule.db.user {
            photoVolume {
                photoShare {}
            }
        }
        val (links, linkProperties) = (1..10000).map { index ->
            val linkId = "photo-id-$index"
            NullableLinkEntity(
                userId,
                photoShareId.id,
                "photo-id-$index",
                photoRootId.id,
                2L,
                "",
            ) to LinkFilePropertiesEntity(
                userId = userId,
                shareId = photoShareId.id,
                linkId = linkId,
                activeRevisionId = "revision-$linkId",
                hasThumbnail = false,
                contentKeyPacket = "",
                contentKeyPacketSignature = null,
                activeRevisionSignatureAddress = null,
            )
        }.unzip()
        driveRule.db.linkDao.insertOrIgnore(*links.toTypedArray())
        driveRule.db.linkDao.insertOrIgnore(*linkProperties.toTypedArray())
        val addToAlbumEntities = (1..10000).map { index ->
            NullableVolumePhotoListing(
                photoId = "photo-id-$index",
                captureTime = TimestampS(index.toLong()),
            ).toAddToAlbumEntity(null)
        }
        driveRule.db.addToAlbumDao.insertOrIgnore(*addToAlbumEntities.toTypedArray())

        // When
        val photoListings = albumInfoRepository.getPhotoListings(userId)

        // Then
        assertEquals(
            10000,
            photoListings.size,
        )
    }

    @Test
    fun clear() = runTest {
        // Given
        val myAlbum = "My album"
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.newAlbumName] = myAlbum
        }
        driveRule.db.user {
            photoVolume {
                photoShare {
                    file("photo-id-1")
                    file("photo-id-2")
                }
            }
        }
        val volumePhotoListings = listOf(
            NullableVolumePhotoListing(
                photoId = "photo-id-1",
                captureTime = TimestampS(1),
            ),
            NullableVolumePhotoListing(
                photoId = "photo-id-2",
                captureTime = TimestampS(2),
            ),
        )
        driveRule.db.addToAlbumDao.insertOrIgnore(
            *volumePhotoListings
                .map { photoListing -> photoListing.toAddToAlbumEntity(null) }
                .toTypedArray()
        )

        // When
        albumInfoRepository.clear(userId)
        val albumName = albumInfoRepository.getName(userId)
        val photoListings = albumInfoRepository.getPhotoListings(userId)

        // Then
        assertNull(albumName)
        assertEquals(
            emptyList<PhotoListing>(),
            photoListings,
        )
    }
}

private fun NullableVolumePhotoListing(
    photoId: String,
    captureTime: TimestampS = TimestampS(),
    nameHash: String? = null,
    contentHash: String? = null,
): PhotoListing.Volume = PhotoListing.Volume(
    linkId = FileId(photoShareId, photoId),
    captureTime = captureTime,
    nameHash = nameHash,
    contentHash = contentHash,
)

private fun NullableAlbumPhotoListing(
    albumId: String,
    photoId: String,
    captureTime: TimestampS = TimestampS(),
    addedTime: TimestampS = TimestampS(),
    isChildOfAlbum: Boolean = false,
    nameHash: String? = null,
    contentHash: String? = null,
): PhotoListing.Album = PhotoListing.Album(
    linkId = FileId(photoShareId, photoId),
    captureTime = captureTime,
    nameHash = nameHash,
    contentHash = contentHash,
    albumId = AlbumId(photoShareId, albumId),
    addedTime = addedTime,
    isChildOfAlbum = isChildOfAlbum,
)
