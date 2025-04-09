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

package me.proton.android.drive.ui.test.flow.preview

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.annotation.SmokeTest
import me.proton.android.drive.ui.robot.AlbumRobot
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_ALBUMS
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.test.fusion.FusionConfig
import me.proton.test.fusion.ui.common.enums.SwipeDirection
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class PreviewAlbumFlowTest : BaseTest() {

    @Before
    fun setUp() {
        PhotosTabRobot.waitUntilLoaded()
        FusionConfig.Compose.waitTimeout.set(30.seconds)
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun previewAlbumTest() {

        val firstImage = "activeTaggedFileInAlbum-3.jpg"
        val secondImage = "activeTaggedFileInAlbum-2.jpg"
        val thirdImage = "activeTaggedFileInAlbum-1.jpg"

        PhotosTabRobot
            .clickOnAlbumsTab()
            .clickOnAlbum("album-for-photos")
            .clickOnPhoto(firstImage)
            .verify {
                assertPreviewIsDisplayed(firstImage)
            }
            .swipePage(SwipeDirection.Left)
            .verify {
                assertPreviewIsDisplayed(secondImage)
            }
            .swipePage(SwipeDirection.Left)
            .verify {
                assertPreviewIsDisplayed(thirdImage)
            }
            .clickBack(AlbumRobot)
            .clickOnPhoto(thirdImage)
            .verify {
                assertPreviewIsDisplayed(thirdImage)
            }
            .swipePage(SwipeDirection.Right)
            .verify {
                assertPreviewIsDisplayed(secondImage)
            }
            .swipePage(SwipeDirection.Right)
            .verify {
                assertPreviewIsDisplayed(firstImage)
            }
    }
}
