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

package me.proton.android.drive.ui.test.flow.album.share

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.AlbumRobot
import me.proton.android.drive.ui.robot.AlbumsTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class SharedWithMeAlbumFlowTest : BaseTest() {

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun leaveWithoutSavingAlbum() {
        val album = "album-for-photos-uploaded-by-other-user"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .swipeFiltersToEnd()
            .clickOnFilterSharedWithMe()
            .verify { assertAtLeastOneAlbumIsDisplayed() }
            .clickOnAlbum(album)
            .verify { assertItemsInAlbum(3) }
            .clickOnMoreButton()
            .clickLeaveAlbum()
            .verify { assertDescriptionIsDisplayed(album) }
            .clickOnLeaveWithoutSaving(AlbumsTabRobot)
            .verify { assertAlbumIsNotDisplayed(album) }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun saveAndLeaveAlbum() {
        val album = "album-for-photos-uploaded-by-other-user"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .swipeFiltersToEnd()
            .clickOnFilterSharedWithMe()
            .verify { assertAtLeastOneAlbumIsDisplayed() }
            .clickOnAlbum(album)
            .verify { assertItemsInAlbum(3) }
            .clickOnMoreButton()
            .clickLeaveAlbum()
            .verify { assertDescriptionIsDisplayed(album) }
            .clickOnSaveAndLeave(AlbumRobot)
            .dismissPhotoSavedToDrive(3)
        AlbumsTabRobot
            .verify { assertAlbumIsNotDisplayed(album) }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun addPhotosToAlbum() {
        val album = "album-for-photos-uploaded-by-other-user"
        val photo = "activeTaggedFileInStream-1.jpg"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .swipeFiltersToEnd()
            .clickOnFilterSharedWithMe()
            .clickOnAlbum(album)
            .verify {
                assertItemsInAlbum(3)
                assertVisibleMediaItemsInAlbum(3)
            }
            .clickOnAdd()
            .clickOnPhoto(photo)
            .clickOnAddToAlbum(AlbumRobot)
            .pullToRefresh(AlbumRobot)
            .verify {
                assertItemsInAlbum(4)
                assertVisibleMediaItemsInAlbum(4)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun addRelatedPhotosToAlbumAndAddToFavorite() {
        val album = "album-for-photos-uploaded-by-other-user"
        val photo = "activeTaggedFileInStream-6.jpg"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .swipeFiltersToEnd()
            .clickOnFilterSharedWithMe()
            .clickOnAlbum(album)
            .verify {
                assertItemsInAlbum(3)
                assertVisibleMediaItemsInAlbum(3)
            }
            .clickOnAdd()
            .scrollToEnd()
            .clickOnPhoto(photo)
            .clickOnAddToAlbum(AlbumRobot)
            .dismissAddToAlbumStartMessage()
            .dismissAddToAlbumSuccess(1)
            .pullToRefresh(AlbumRobot)
            .verify {
                assertItemsInAlbum(4)
                assertVisibleMediaItemsInAlbum(4)
            }
            .longClickOnPhoto(photo)
            .clickOptions()
            .clickAddToFavorite(AlbumRobot)
            .verify {
                assertAddToFavoriteFromForeignVolume()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun saveSharedPhotoToStream() {
        val album = "album-for-photos-uploaded-by-other-user"
        val photo = "activeFileInAlbumByOtherUser.jpg"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .swipeFiltersToEnd()
            .clickOnFilterSharedWithMe()
            .clickOnAlbum(album)
            .longClickOnPhoto(photo)
            .clickOptions()
            .clickSaveSharedPhoto()
            .dismissPhotoSavedToDrive(1)
            .clickBack(AlbumsTabRobot)
            .clickOnPhotosTitleTab()
            .pullToRefresh(PhotosTabRobot)
            .scrollToPhoto(photo)
            .verify {
                itemIsDisplayed(photo)
            }

    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun saveAllSharedPhotoToStream() {
        val album = "album-for-photos-uploaded-by-other-user"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .swipeFiltersToEnd()
            .clickOnFilterSharedWithMe()
            .clickOnAlbum(album)
            .clickOnSaveAll()
            .dismissPhotoSavedToDrive(3)
    }
}
