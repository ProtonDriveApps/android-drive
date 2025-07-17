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

package me.proton.android.drive.ui.test.flow.photos.list

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.AlbumRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.ExternalStorageBaseTest
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class PhotosAddToAlbumsFlowTest : ExternalStorageBaseTest() {

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun addSinglePhotoToExistingAlbum() {
        val photo = "activeTaggedFileInStream-1.jpg"
        val albumName = "album-for-photos-in-stream"
        PhotosTabRobot
            .longClickOnPhoto(photo)
            .clickOptions()
            .clickAddToAlbums()
            .clickOnAlbum(albumName)
            .verify {
                assertAlbumNameIsDisplayed(albumName)
                assertItemsInAlbum(2)
                assertVisibleMediaItemsInAlbum(2)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun addMultiplePhotosToNewAlbum() {
        val photo1 = "activeTaggedFileInStream-1.jpg"
        val photo2 = "activeTaggedFileInStream-2.jpg"
        val video = "activeVideoFileInStream-1.jpg"
        val newAlbumName = "new-album-name"
        PhotosTabRobot
            .longClickOnPhoto(photo1)
            .longClickOnPhoto(photo2)
            .longClickOnPhoto(video)
            .clickOptions()
            .clickAddToAlbums()
            .clickOnAddToNewAlbum()
            .typeName(newAlbumName)
            .clickOnRemoveFirstPhoto()
            .clickOnDone(AlbumRobot)
            .verify {
                assertAlbumNameIsDisplayed(newAlbumName)
                assertItemsInAlbum(2)
                assertVisibleMediaItemsInAlbum(2)
            }
    }
}
