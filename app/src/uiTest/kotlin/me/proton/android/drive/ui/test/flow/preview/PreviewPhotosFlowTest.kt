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
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.annotation.SmokeTest
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.test.fusion.FusionConfig
import me.proton.test.fusion.ui.common.enums.SwipeDirection
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class PreviewPhotosFlowTest : BaseTest() {

    @Before
    fun setUp() {
        PhotosTabRobot.waitUntilLoaded()
        FusionConfig.Compose.waitTimeout.set(30.seconds)
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    @SmokeTest
    fun previewPhotoTest() {

        val firstImage = ImageName.Main
        val secondImage = ImageName.Now
        val thirdImage = ImageName.Yesterday

        PhotosTabRobot
            .clickOnPhoto(firstImage)
            .verify {
                assertPreviewIsDisplayed(firstImage.fileName)
            }
            .swipePage(SwipeDirection.Left)
            .verify {
                assertPreviewIsDisplayed(secondImage.fileName)
            }
            .swipePage(SwipeDirection.Left)
            .verify {
                assertPreviewIsDisplayed(thirdImage.fileName)
            }
            .clickBack(PhotosTabRobot)
            .clickOnPhoto(thirdImage)
            .verify {
                assertPreviewIsDisplayed(thirdImage.fileName)
            }
            .swipePage(SwipeDirection.Right)
            .verify {
                assertPreviewIsDisplayed(secondImage.fileName)
            }
            .swipePage(SwipeDirection.Right)
            .verify {
                assertPreviewIsDisplayed(firstImage.fileName)
            }
    }
}
