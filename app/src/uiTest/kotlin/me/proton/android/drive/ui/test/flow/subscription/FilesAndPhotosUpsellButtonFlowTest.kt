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

package me.proton.android.drive.ui.test.flow.subscription

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.plan.test.robot.SubscriptionRobot
import me.proton.core.test.quark.data.Plan
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.core.test.rule.annotation.payments.TestSubscriptionData
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class FilesAndPhotosUpsellButtonFlowTest : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true, subscriptionData = TestSubscriptionData(Plan.Free))
    @Scenario(forTag = "main", value = 2)
    fun freeUserHasUpsellButton() {
        val folder = "folder1"
        PhotosTabRobot
            .clickOnPhotosTitleTab()
            .clickOpenSubscription()
            .currentPlanIsDisplayed()
        SubscriptionRobot
            .close()
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .verify {
                nodeWithContentDescriptionIsNotDisplayed(
                    I18N.string.content_description_subscription_action
                )
            }
            .clickFilesTab()
            .clickOnFolder(folder)
            .verify {
                nodeWithContentDescriptionIsNotDisplayed(
                    I18N.string.content_description_subscription_action
                )
            }
            .clickBack(FilesTabRobot)
            .clickOpenSubscription()
            .currentPlanIsDisplayed()
    }

    @Test
    @PrepareUser(loginBefore = true, subscriptionData = TestSubscriptionData(Plan.DrivePlus))
    @Scenario(forTag = "main", value = 2)
    fun paidUserDoesNotHaveUpsellButton() {
        PhotosTabRobot
            .clickOnPhotosTitleTab()
            .verify {
                nodeWithContentDescriptionIsNotDisplayed(
                    I18N.string.content_description_subscription_action
                )
            }
            .clickFilesTab()
            .verify {
                nodeWithContentDescriptionIsNotDisplayed(
                    I18N.string.content_description_subscription_action
                )
            }
    }
}
