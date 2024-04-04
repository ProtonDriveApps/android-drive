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

package me.proton.android.drive.photos.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddFolder
import me.proton.core.drive.db.test.NullableUserEntity
import me.proton.core.drive.db.test.VolumeContext
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photoShare
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.user.domain.entity.UserMessage
import me.proton.core.drive.user.domain.usecase.CancelUserMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject


@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ShowUpsellTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var cancelUserMessage: CancelUserMessage

    @Inject
    lateinit var showUpsell: ShowUpsell

    @Test
    fun `Given free user and backup disabled when show upsell should returns false`() = runTest {
        driveRule.db.user {
            volume {}
        }

        assertFalse(showUpsell(userId).first())
    }

    @Test
    fun `Given free user without photos and backup enabled when show upsell should returns false`() = runTest {
        val folderId = driveRule.db.user {
            volume {
                photoShare {}
            }
        }

        addFolder(BackupFolder(0, folderId)).getOrThrow()

        assertFalse(showUpsell(userId).first())
    }

    @Test
    fun `Given free user with 5 photos and backup disabled when show upsell should returns false`() = runTest {
        driveRule.db.user {
            volume {
                photoShareWithPhotos(5)
            }
        }

        assertFalse(showUpsell(userId).first())
    }

    @Test
    fun `Given free user with 5 photos and backup enabled when show upsell should returns true`() = runTest {
        val folderId = driveRule.db.user {
            volume {
                photoShareWithPhotos(5)
            }
        }

        addFolder(BackupFolder(0, folderId)).getOrThrow()

        assertTrue(showUpsell(userId).first())
    }

    @Test
    fun `Given message cancelled when show upsell should returns false`() = runTest {
        val folderId = driveRule.db.user {
            volume {
                photoShareWithPhotos(5)
            }
        }

        addFolder(BackupFolder(0, folderId)).getOrThrow()

        cancelUserMessage(userId, UserMessage.UPSELL_PHOTOS).getOrThrow()

        assertFalse(showUpsell(userId).first())
    }

    @Test
    fun `Given user with more storage with 5 photos and backup enabled when show upsell should returns false`() = runTest {
        val folderId = driveRule.db.user(NullableUserEntity(subscribed = 2)) {
            volume {
                photoShareWithPhotos(5)
            }
        }

        addFolder(BackupFolder(0, folderId)).getOrThrow()

        assertFalse(showUpsell(userId).first())
    }

    private suspend fun VolumeContext.photoShareWithPhotos(count : Int) = photoShare {
        (0 until count).forEach { index ->
            file("$index.jpg")
        }
    }

}
