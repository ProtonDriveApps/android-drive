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
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.ExternalStorageBaseTest
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_ALBUMS
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class AlbumFavoriteFlowTest : ExternalStorageBaseTest() {

    private val albumNameInStream = "album-for-photos-in-stream"
    private val albumNameAlbumOnly = "album-for-photos"
    private val albumNameUploadedByOtherUser = "album-for-photos-uploaded-by-other-user"

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun addToFavoriteAndRemoveFromFavoritePhotoInStream() {
        val fileName = "activeFileInStreamAndAlbum.jpg"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickOnAlbum(albumNameInStream)
            .longClickOnPhoto(fileName)
            .clickOptions()
            .clickAddToFavorite(PhotosTabRobot)
            .close(PhotosTabRobot)
            .verify {
                itemIsDisplayed(fileName, isFavorite = true)
            }
            .longClickOnPhoto(fileName)
            .clickOptions()
            .clickRemoveFromFavorite(PhotosTabRobot)
            .close(PhotosTabRobot)
            .verify {
                itemIsDisplayed(fileName, isFavorite = false)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun addToFavoriteAndRemoveFromFavoritePhotoInPreviewInStream() {
        val fileName = "activeFileInStreamAndAlbum.jpg"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickOnAlbum(albumNameInStream)
            .clickOnPhoto(fileName)
            .clickOnContextualButton()
            .clickAddToFavorite(PhotosTabRobot)
            .clickBack(PhotosTabRobot)
            .verify {
                itemIsDisplayed(fileName, isFavorite = true)
            }
            .clickOnPhoto(fileName)
            .clickOnContextualButton()
            .clickRemoveFromFavorite(PhotosTabRobot)
            .close(PhotosTabRobot)
            .verify {
                itemIsDisplayed(fileName, isFavorite = false)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun addToFavoritePhotoInAlbumOnly() {
        val fileName = "activeTaggedFileInAlbum-3.jpg"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickOnAlbum(albumNameAlbumOnly)
            .longClickOnPhoto(fileName)
            .clickOptions()
            .clickAddToFavorite(PhotosTabRobot)
            .close(PhotosTabRobot)
            .verify {
                itemIsDisplayed(fileName, isFavorite = true)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun addToFavoritePhotoUploadedByOtherUser() {
        val fileName = "activeFileInAlbumByOtherUser.jpg"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickOnAlbum(albumNameUploadedByOtherUser)
            .longClickOnPhoto(fileName)
            .clickOptions()
            .clickAddToFavorite(PhotosTabRobot)
            .close(PhotosTabRobot)
            .verify {
                itemIsDisplayed(fileName, isFavorite = true)
            }
    }
}
