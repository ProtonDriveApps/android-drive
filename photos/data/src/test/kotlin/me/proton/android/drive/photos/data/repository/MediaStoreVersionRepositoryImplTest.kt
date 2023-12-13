/*
 * Copyright (c) 2023 Proton AG.
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

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.photos.domain.repository.MediaStoreVersionRepository
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaStoreVersionRepositoryImplTest {
    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var repository: MediaStoreVersionRepository

    @Before
    fun setup() = runTest {
        database.db.user { }
        repository = MediaStoreVersionRepositoryImpl(
            ApplicationProvider.getApplicationContext(),
            database.db,
        )
    }

    @Test
    fun empty() = runTest {
        assertNull(repository.getLastVersion(userId))
    }

    @Test
    fun set() = runTest {
        repository.setLastVersion(userId, "1")
        assertEquals("1", repository.getLastVersion(userId))
    }
}
