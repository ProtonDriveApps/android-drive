/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.link.domain.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.deviceShare
import me.proton.core.drive.db.test.deviceShareId
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.photoShare
import me.proton.core.drive.db.test.photoShareId
import me.proton.core.drive.db.test.user
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
class LinkRepositoryTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var linkRepository: LinkRepository

    @Test
    fun `single file in main share`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                mainShare {
                    file("file-id")
                }
            }
        }

        // When
        val anyFile = linkRepository.hasAnyFileLink(shareId = mainShareId)

        // Then
        assertTrue(anyFile)
    }

    @Test
    fun `single folder in main share`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                mainShare {
                    folder("folder-id")
                }
            }
        }

        // When
        val anyFile = linkRepository.hasAnyFileLink(shareId = mainShareId)

        // Then
        assertFalse(anyFile)
    }

    @Test
    fun `single file in photo share`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                photoShare {
                    file("photo-file-id")
                }
            }
        }

        // When
        val anyFile = linkRepository.hasAnyFileLink(shareId = photoShareId)

        // Then
        assertTrue(anyFile)
    }

    @Test
    fun `no file in photo share`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                photoShare { }
            }
        }

        // When
        val anyFile = linkRepository.hasAnyFileLink(shareId = photoShareId)

        // Then
        assertFalse(anyFile)
    }

    @Test
    fun `single file in device share`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                deviceShare {
                    file("device-file-id")
                }
            }
        }

        // When
        val anyFile = linkRepository.hasAnyFileLink(shareId = deviceShareId())

        // Then
        assertTrue(anyFile)
    }

    @Test
    fun `single folder in device share`() = runTest {
        // Given
        driveRule.db.user {
            volume {
                deviceShare {
                    folder("device-folder-id")
                }
            }
        }

        // When
        val anyFile = linkRepository.hasAnyFileLink(shareId = deviceShareId())

        // Then
        assertFalse(anyFile)
    }
}
