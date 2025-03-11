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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.photos.domain.entity.NewAlbumInfo
import me.proton.android.drive.photos.domain.repository.AlbumInfoRepository
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.core.drive.db.test.userId
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
        val newAlbumInfo = albumInfoRepository.getInfo(userId)

        // Then
        assertEquals(
            NewAlbumInfo(
                name = null,
                items = emptyFlow(),
            ),
            newAlbumInfo,
        )
    }

    @Test
    fun `if new album name has been set NewAlbumInfo should contain it`() = runTest {
        // Given
        val myAlbum = "My album"
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.newAlbumName] = myAlbum
        }

        // When
        val newAlbumInfo = albumInfoRepository.getInfo(userId)

        // Then
        assertEquals(
            NewAlbumInfo(
                name = myAlbum,
                items = emptyFlow(),
            ),
            newAlbumInfo,
        )
    }

    @Test
    fun `if new album name has been updated NewAlbumInfo should contain it`() = runTest {
        // Given
        val myAlbum = "My album"
        val newAlbumName = "New album name"
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.newAlbumName] = myAlbum
        }

        // When
        albumInfoRepository.updateName(userId, newAlbumName)
        val newAlbumInfo = albumInfoRepository.getInfo(userId)

        // Then
        assertEquals(
            NewAlbumInfo(
                name = newAlbumName,
                items = emptyFlow(),
            ),
            newAlbumInfo,
        )
    }

    @Test
    fun clear() = runTest {
        // Given
        val myAlbum = "My album"
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.newAlbumName] = myAlbum
        }

        // When
        albumInfoRepository.clear(userId)
        val newAlbumInfo = albumInfoRepository.getInfo(userId)

        // Then
        assertEquals(
            NewAlbumInfo(
                name = null,
                items = emptyFlow(),
            ),
            newAlbumInfo,
        )
    }
}
