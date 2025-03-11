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

package me.proton.android.drive.ui.test.flow.album

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.AlbumTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.ExternalStorageBaseTest
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_ALBUMS
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class AlbumFlowTest : ExternalStorageBaseTest() {

    @Before
    fun setUp() {
        PhotosTabRobot
            .clickOnAlbumTab()
            .verify {
                robotDisplayed()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun albumSharedByMeFilter() {
        AlbumTabRobot
            .clickOnFilterSharedByMe()
            .verify {
                assertAlbumIsDisplayed("activeAlbum-shared")
                assertAlbumIsNotDisplayed("album-for-photos-in-stream")
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun albumSharedWithMeFilter() {
        AlbumTabRobot
            .clickOnFilterSharedWithMe()
            .verify {
                assertIsEmpty()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun albumWithoutPhotos() {
        val albumName = "activeAlbum-shared"
        val albumPhotoCount = 0
        AlbumTabRobot
            .clickOnAlbum(albumName)
            .verify {
                robotDisplayed()
                assertAlbumNameIsDisplayed(albumName)
                assertItemsInAlbum(albumPhotoCount)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun albumWithPhotos() {
        val albumName = "album-for-photos"
        val albumPhotoCount = 1
        AlbumTabRobot
            .clickOnAlbum(albumName)
            .verify {
                robotDisplayed()
                assertAlbumNameIsDisplayed(albumName)
                assertItemsInAlbum(albumPhotoCount)
            }
    }
}
