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
import me.proton.android.drive.ui.robot.AlbumRobot
import me.proton.android.drive.ui.robot.AlbumsTabRobot
import me.proton.android.drive.ui.robot.ConfirmDeleteAlbumRobot
import me.proton.android.drive.ui.robot.FilesTabRobot.itemsRemovedFromAlbumGrowlerIsDisplayedAndDismissed
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_ALBUMS
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Before
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class AlbumFlowTest : BaseTest() {

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
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun renameAlbum() {
        val newAlbumName = "new-album-name"
        AlbumsTabRobot
            .clickOnAlbum("album-for-photos")
            .clickOnMoreButton()
            .clickRename()
            .typeName(newAlbumName)
            .clickRename(AlbumRobot)
            .verify {
                assertAlbumNameIsDisplayed(newAlbumName)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun deleteAlbumWithoutChildren() {
        val albumName = "album-for-photos-in-stream"
        AlbumsTabRobot
            .verify { assertAtLeastOneAlbumIsDisplayed() }
            .clickOnAlbum(albumName)
            .clickOnMoreButton()
            .clickDeleteAlbum()
            .clickOnDeleteAlbum(AlbumsTabRobot)
            .verify {
                assertAlbumIsNotDisplayed(albumName)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun deleteAlbumWithChildrenWithoutSaving() {
        val albumName = "album-for-photos"
        AlbumsTabRobot
            .clickOnAlbum(albumName)
            .clickOnMoreButton()
            .clickDeleteAlbum()
            .clickOnDeleteAlbum(ConfirmDeleteAlbumRobot)
            .verify {
                assertWithChildrenDialogIsDisplayed()
            }
            .clickOnDeleteWithoutSaving(AlbumsTabRobot)
            .verify {
                assertAlbumIsNotDisplayed(albumName)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun saveAndDeleteAlbumWithChildren() {
        val albumName = "album-for-photos"
        val photoNotInPhotoStream = "activeTaggedFileInAlbum-3.jpg"
        AlbumsTabRobot
            .clickOnAlbum(albumName)
            .clickOnMoreButton()
            .verify { assertDeleteAlbumOptionIsDisplayed() }
            .clickDeleteAlbum()
            .clickOnDeleteAlbum(ConfirmDeleteAlbumRobot)
            .verify {
                assertWithChildrenDialogIsDisplayed()
            }
            .clickOnSaveAndDelete(AlbumsTabRobot)
            .verify {
                assertAlbumIsNotDisplayed(albumName)
            }
            .clickOnPhotosTitleTab()
            .verify {
                assertPhotoDisplayed(photoNotInPhotoStream)
            }
    }
    
    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun setAsAlbumCover() {
        val albumName = "album-for-photos"
        val image = "activeTaggedFileInAlbum-3.jpg"
        AlbumsTabRobot
            .clickOnAlbum(albumName)
            .clickOnPhoto(image)
            .clickOnContextualButton()
            .clickSetAsAlbumCover()
            .verify {
                nodeWithTextDisplayed(I18N.string.albums_set_album_as_cover_success)
            }
            .clickBack(AlbumRobot)
            .verify {
                assertCoverAlbum(image)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun addPhotosIntoAlbum() {
        val albumName = "album-for-photos-uploaded-by-other-user"
        val photoInStream = "activeTaggedFileInStream-2.jpg"
        AlbumsTabRobot
            .clickOnAlbum(albumName)
            .verify {
                assertAlbumNameIsDisplayed(albumName)
                assertItemsInAlbum(3)
                assertVisibleMediaItemsInAlbum(3)
            }
            .clickOnAdd()
            .clickOnPhoto(photoInStream)
            .verify { assertTotalPhotosToAddToAlbum(1) }
            .clickOnAddToAlbum(AlbumRobot)
            .verify {
                assertAlbumNameIsDisplayed(albumName)
                assertVisibleMediaItemsInAlbum(4)
                assertItemsInAlbum(4)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun addRelatedPhotoIntoAlbum() {
        val albumName = "album-for-photos-uploaded-by-other-user"
        val photoInStream = "activeTaggedFileInStream-6.jpg"
        AlbumsTabRobot
            .clickOnAlbum(albumName)
            .verify {
                assertAlbumNameIsDisplayed(albumName)
                assertItemsInAlbum(3)
                assertVisibleMediaItemsInAlbum(3)
            }
            .clickOnAdd()
            .scrollToEnd()
            .clickOnPhoto(photoInStream)
            .verify { assertTotalPhotosToAddToAlbum(1) }
            .clickOnAddToAlbum(AlbumRobot)
            .verify {
                assertAlbumNameIsDisplayed(albumName)
                assertVisibleMediaItemsInAlbum(4)
                assertItemsInAlbum(4)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun removePhotosFromAlbum() {
        val albumName = "album-for-photos"
        val photo1 = "activeFileInAlbum.jpg"
        val photo2 = "activeTaggedFileInAlbum-3.jpg"
        val photo3 = "activeTaggedFileInAlbum-2.jpg"

        AlbumsTabRobot
            .verify { assertAtLeastOneAlbumIsDisplayed() }
            .clickOnAlbum(albumName)
            .verify {
                assertItemsInAlbum(6)
                assertVisibleMediaItemsInAlbum(6)
            }
            .longClickOnItem(photo1)
            .clickOptions()
            .clickRemoveFromAlbum()
            .verify {
                itemsRemovedFromAlbumGrowlerIsDisplayedAndDismissed(1)
            }
            .verify {
                assertItemsInAlbum(5)
                assertVisibleMediaItemsInAlbum(5)
            }
            .longClickOnItem(photo2, AlbumRobot)
            .clickOnPhoto(photo3, AlbumRobot)
            .clickMultipleOptions()
            .clickRemoveFromAlbum()
            .verify {
                itemsRemovedFromAlbumGrowlerIsDisplayedAndDismissed(2)
            }
            .verify {
                assertItemsInAlbum(3)
                assertVisibleMediaItemsInAlbum(3)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun sharePhotosThroughSharedAlbum() {
        val firstPhoto = "activeTaggedFileInStream-1.jpg"
        val secondPhoto = "activeTaggedFileInStream-2.jpg"
        val sharedAlbum = "activeAlbum-shared"
        PhotosTabRobot
            .clickOnPhotosTitleTab()
            .longClickOnPhoto(firstPhoto)
            .longClickOnPhoto(secondPhoto)
            .clickMultipleOptions()
            .clickShare()
            .clickOnSharedAlbum(sharedAlbum)
            .verify {
                assertAlbumNameIsDisplayed(sharedAlbum)
                assertItemsInAlbum(2)
            }
    }
}
