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

package me.proton.core.drive.folder.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.deviceShare
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.photoShare
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HasAnyCachedFolderChildrenTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var hasAnyCachedFolderChildren: HasAnyCachedFolderChildren

    @Test
    fun `local cache is empty`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                mainShare {  }
                photoShare {  }
                deviceShare {  }
            }
        }

        // When
        val anyFilesOrFolders = hasAnyCachedFolderChildren(userId = userId, filesOnly = false)
        val anyFiles = hasAnyCachedFolderChildren(userId = userId, filesOnly = true)

        // Then
        assertFalse(anyFilesOrFolders)
        assertFalse(anyFiles)
    }

    @Test
    fun `local cache has only single file in My files`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                mainShare {
                    file("file")
                }
            }
        }

        // When
        val anyFilesOrFolders = hasAnyCachedFolderChildren(userId = userId, filesOnly = false)
        val anyFiles = hasAnyCachedFolderChildren(userId = userId, filesOnly = true)

        // Then
        assertTrue(anyFilesOrFolders)
        assertTrue(anyFiles)
    }

    @Test
    fun `local cache has only single file in Photos`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                photoShare {
                    file("photo-file")
                }
            }
        }

        // When
        val anyFilesOrFolders = hasAnyCachedFolderChildren(userId = userId, filesOnly = false)
        val anyFiles = hasAnyCachedFolderChildren(userId = userId, filesOnly = true)

        // Then
        assertTrue(anyFilesOrFolders)
        assertTrue(anyFiles)
    }

    @Test
    fun `local cache has only single file in Computers`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                deviceShare {
                    file("device-file")
                }
            }
        }

        // When
        val anyFilesOrFolders = hasAnyCachedFolderChildren(userId = userId, filesOnly = false)
        val anyFiles = hasAnyCachedFolderChildren(userId = userId, filesOnly = true)

        // Then
        assertTrue(anyFilesOrFolders)
        assertTrue(anyFiles)
    }

    @Test
    fun `local cache has only folders without any files`() = runTest {
        driveRule.db.user {
            volume {
                mainShare {
                    folder("child-folder-id")
                }
                deviceShare(1) {
                    folder("child-device-1-folder-id")
                }
                deviceShare(2) {
                    folder("child-device-2-folder-id")
                }
            }
        }

        // When
        val anyFilesOrFolders = hasAnyCachedFolderChildren(userId = userId, filesOnly = false)
        val anyFiles = hasAnyCachedFolderChildren(userId = userId, filesOnly = true)

        // Then
        assertTrue(anyFilesOrFolders)
        assertFalse(anyFiles)
    }
}
