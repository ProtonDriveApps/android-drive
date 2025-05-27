/*
 * Copyright (c) 2023-2024 Proton AG.
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
import me.proton.android.drive.ui.robot.CreateAlbumTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.PickerAlbumRobot
import me.proton.android.drive.ui.robot.ShareUserRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_ALBUMS
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class CreatingAlbumFlowTest : BaseTest() {

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun createAnAlbum() {
        val randomAlbumName = getRandomString()

        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickPlusButton()
            .typeName(randomAlbumName)
            .clickOnDone(AlbumRobot)
            .verify { 
                robotDisplayed()
                assertAlbumNameIsDisplayed(randomAlbumName)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun createAnAlbumWithExistingName() {
        val albumName = "album-for-photos"

        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickPlusButton()
            .typeName(albumName)
            .clickOnDone(AlbumRobot)
            .verify {
                robotDisplayed()
                assertAlbumNameIsDisplayed(albumName)
                assertItemsInAlbum(0)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun inEmptyAlbumAddPhotosThenCreate() {
        val albumName = "new-album"
        val photo1 = "activeTaggedFileInStream-2.jpg"
        val photo2 = "activeFileInStreamAndAlbum.jpg"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickPlusButton()
            .typeName(albumName)
            .clickOnAdd()
            .verify {
                assertTotalPhotosToAddToAlbum(0)
            }
            .clickOnPhoto(photo1)
            .verify {
                assertTotalPhotosToAddToAlbum(1)
            }
            .clickOnReset(PickerAlbumRobot)
            .verify {
                assertTotalPhotosToAddToAlbum(0)
            }
            .clickOnPhoto(photo2)
            .clickOnPhoto(photo1)
            .clickOnAddToAlbum(CreateAlbumTabRobot)
            .clickOnDone(AlbumRobot)
            .verify {
                assertAlbumNameIsDisplayed(albumName)
                assertItemsInAlbum(2)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun createSharedAlbum() {
        val sharingUser = protonRule.testDataRule.preparedUsers["sharingUser"]!!
        val firstPhoto = "activeTaggedFileInStream-1.jpg"
        val secondPhoto = "activeTaggedFileInStream-2.jpg"
        val sharedAlbumName = "My shared album"
        PhotosTabRobot
            .longClickOnPhoto(firstPhoto)
            .longClickOnPhoto(secondPhoto)
            .clickMultipleOptions()
            .clickShare()
            .clickOnNewSharedAlbum()
            .typeName(sharedAlbumName)
            .clickOnShare(ShareUserRobot)
            .typeEmail(sharingUser.email)
            .clickSend()
            .verify {
                dismissInvitationSent(1)
            }
        AlbumRobot
            .verify {
                assertAlbumNameIsDisplayed(sharedAlbumName)
            }
            .clickBack(PhotosTabRobot)
            .clickOnAlbumsTitleTab()
            .clickOnFilterSharedByMe()
            .verify {
                assertAlbumIsDisplayed(sharedAlbumName)
            }
    }
}
