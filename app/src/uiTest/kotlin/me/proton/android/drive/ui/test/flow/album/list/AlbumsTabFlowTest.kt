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

package me.proton.android.drive.ui.test.flow.album.list

import androidx.test.espresso.Espresso
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.AlbumRobot
import me.proton.android.drive.ui.robot.AlbumRobot.assertAlbumNameIsDisplayed
import me.proton.android.drive.ui.robot.AlbumRobot.assertNameEmptyError
import me.proton.android.drive.ui.robot.AlbumsTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.ExternalStorageBaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class AlbumsTabFlowTest : ExternalStorageBaseTest() {

    @Before
    fun setUp() {
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .verify {
                robotDisplayed()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun albumSharedByMeFilter() {
        AlbumsTabRobot
            .clickOnFilterSharedByMe()
            .verify { assertAtLeastOneAlbumIsDisplayed() }
            .swipeUpAlbumsContent(AlbumsTabRobot)
            .verify {
                assertAlbumIsDisplayed("activeAlbum-shared")
                assertAlbumIsNotDisplayed("album-for-photos-in-stream")
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun albumSharedWithMeFilter() {
        AlbumsTabRobot
            .swipeFiltersToEnd()
            .clickOnFilterSharedWithMe()
            .verify {
                assertAlbumIsDisplayed("album-for-photos-uploaded-by-other-user")
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun albumWithoutPhotos() {
        val albumName = "activeAlbum-shared"
        val albumPhotoCount = 0
        AlbumsTabRobot
            .verify { assertAtLeastOneAlbumIsDisplayed() }
            .swipeUpAlbumsContent(AlbumsTabRobot)
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
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun albumAllFilter() {
        val newAlbumName = "new-album"
        val existingAlbumName = "album-for-photos-uploaded-by-other-user"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickPlusButton()
            .typeName(newAlbumName)
            .clickOnDone(AlbumRobot)
            .verify {assertEmptyAlbum()}
            .clickBack(AlbumsTabRobot)
        AlbumsTabRobot
            .clickOnFilterAll()
            .verify {
                assertAlbumNameIsDisplayed(newAlbumName)
                assertAlbumNameIsDisplayed(existingAlbumName)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun albumMyAlbumsFilter() {
        val newAlbumName = "new-album"
        val existingAlbumName = "album-for-photos-uploaded-by-other-user"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickPlusButton()
            .typeName(newAlbumName)
            .clickOnDone(AlbumRobot)
            .verify {assertEmptyAlbum()}
            .clickBack(AlbumRobot)
        AlbumsTabRobot
            .clickOnFilterAlbums()
            .verify {
                assertAlbumNameIsDisplayed(newAlbumName)
                assertAlbumIsNotDisplayed(existingAlbumName)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun albumWithPhotos() {
        val albumName = "album-for-photos"
        val albumPhotoCount = 6
        AlbumsTabRobot
            .verify { assertAtLeastOneAlbumIsDisplayed() }
            .clickOnAlbum(albumName)
            .verify {
                robotDisplayed()
                assertAlbumNameIsDisplayed(albumName)
                assertItemsInAlbum(albumPhotoCount)
                assertVisibleMediaItemsInAlbum(albumPhotoCount)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun albumAllFilterEmptyState() {
        val album = "album-for-photos-uploaded-by-other-user"
        AlbumsTabRobot
            .swipeFiltersToEnd()
            .clickOnFilterSharedWithMe()
            .verify { assertAtLeastOneAlbumIsDisplayed() }
            .clickOnAlbum(album)
            .clickOnMoreButton()
            .clickLeaveAlbum()
            .verify { assertDescriptionIsDisplayed(album) }
            .clickOnLeaveWithoutSaving(AlbumsTabRobot)
            .swipeFiltersToStart()
            .clickOnFilterAll()
            .verify {
                assertIsEmptyList()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun albumMyAlbumsEmptyState() {
        AlbumsTabRobot
            .clickOnFilterAlbums()
            .verify {
                assertIsEmptyMyAlbums()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 10, sharedWithUserTag = "main")
    fun albumSharedByMeEmptyState() {
        AlbumsTabRobot
            .clickOnFilterSharedByMe()
            .verify {
                assertIsEmptySharedByMe()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun albumSharedWithMeEmptyState() {
        AlbumsTabRobot
            .swipeFiltersToEnd()
            .clickOnFilterSharedWithMe()
            .verify {
                assertIsEmptySharedWithMe()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    fun albumListEmptyState() {
        AlbumsTabRobot
            .verify {
                assertIsEmptyList()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    fun albumEmptyState() {
        val album = "emptyAlbum"
        AlbumsTabRobot
            .clickPlusButton()
            .typeName(album)
            .clickOnDone(AlbumRobot)
            Espresso.closeSoftKeyboard()
            AlbumRobot.verify {
                assertEmptyAlbum()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun renameAlbumWithExistingName() {
        val albumName = "album-for-photos-in-stream"
        val newAlbumName = "album-for-photos"

        AlbumsTabRobot
            .clickOnAlbum(albumName)
            .clickOnMoreButton()
            .clickRename()
            .typeName(newAlbumName)
            .clickRename()
            Thread.sleep(2000)
            AlbumRobot.verify {
                assertAlbumNameIsDisplayed(newAlbumName)
            }
            .clickBack(AlbumsTabRobot)
            .verify {
            robotDisplayed()
            assertTwoAlbumsWithSameNameExist(newAlbumName)
        }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun renameAlbumWithEmptyName() {
        val albumName = "album-for-photos-in-stream"
        val newAlbumName = " "

        AlbumsTabRobot
            .clickOnAlbum(albumName)
            .clickOnMoreButton()
            .clickRename()
            .typeName(newAlbumName)
            .clickRename()
            .verify {
                assertNameEmptyError()
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun renameAlbumWithEmailAddress() {
        val albumName = "album-for-photos-in-stream"
        val newAlbumName = "albums@drive.proton.me"

        AlbumsTabRobot
            .clickOnAlbum(albumName)
            .clickOnMoreButton()
            .clickRename()
            .typeName(newAlbumName)
            .clickRename()
        AlbumRobot.verify {
             assertAlbumNameIsDisplayed(newAlbumName)
        }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun renameAlbumWithNameAtMaxLimit() {
        val albumName = "album-for-photos-in-stream"
        val newAlbumName = getRandomString(255)

        AlbumsTabRobot
            .clickOnAlbum(albumName)
            .clickOnMoreButton()
            .clickRename()
            .typeName(newAlbumName)
            .clickRename()
        AlbumRobot.verify {
            assertAlbumNameIsDisplayed(newAlbumName)
        }
    }
}
