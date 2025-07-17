/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.test.flow.photos.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import me.proton.android.drive.photos.data.di.PhotosConfigurationModule
import me.proton.android.drive.photos.domain.provider.PhotosDefaultConfigurationProvider
import me.proton.android.drive.provider.PhotosConnectedDefaultConfigurationProvider
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.PhotosUpsellRobot
import me.proton.android.drive.ui.test.PhotosBaseTest
import me.proton.core.plan.test.robot.SubscriptionRobot
import me.proton.core.test.quark.data.Plan
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.core.test.rule.annotation.payments.TestSubscriptionData
import org.junit.Before
import org.junit.Test
import javax.inject.Singleton

@HiltAndroidTest
@UninstallModules(PhotosConfigurationModule::class)
class UpsellPhotosUsersTest : PhotosBaseTest() {

    @Before
    fun setUp() {
        pictureCameraFolder.copyDirFromAssets("images/basic")
        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .enableBackup()
            .dismissBackupSetupGrowler("Camera", "Camera")
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoCountEquals(5)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun upsellPopUpIsShownForFreeUsers() {
        PhotosUpsellRobot
            .verify {
                robotDisplayed()
            }
            .clickGetStorage()
            .verify {
                SubscriptionRobot.currentPlanIsDisplayed()
            }
    }

    @Test
    @PrepareUser(subscriptionData = TestSubscriptionData(plan = Plan.PassPlus), loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun upsellPopUpIsShownForPassPlusUsers() {
        PhotosUpsellRobot
            .verify {
                robotDisplayed()
            }
            .clickGetStorage()
            .verify {
                SubscriptionRobot.currentPlanIsDisplayed()
            }
    }

    @Test
    @PrepareUser(subscriptionData = TestSubscriptionData(plan = Plan.MailPlus), loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun upsellPopUpIsNotShownForMailPlusUsers() {
        PhotosUpsellRobot
            .verify {
                robotDoesNotExist()
            }
    }

    @Test
    @PrepareUser(subscriptionData = TestSubscriptionData(plan = Plan.Unlimited), loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun upsellPopUpIsNotShownForDriveUsers() {

        val user = protonRule.testDataRule.mainTestUser
        PhotosUpsellRobot
            .verify {
                robotDoesNotExist()
            }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    @Suppress("Unused")
    interface TestPhotosConfigurationModule {
        @Binds
        @Singleton
        fun bindPhotosDefaultConfigurationProvider(
            impl: PhotosConnectedDefaultConfigurationProvider,
        ): PhotosDefaultConfigurationProvider
    }
}
