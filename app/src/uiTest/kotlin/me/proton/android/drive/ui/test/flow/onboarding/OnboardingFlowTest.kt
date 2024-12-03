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

package me.proton.android.drive.ui.test.flow.onboarding

import android.os.Build
import android.os.Environment
import androidx.test.filters.SdkSuppress
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.extension.debug
import me.proton.android.drive.ui.extension.allowPhotosPermissions
import me.proton.android.drive.ui.robot.NotificationPermissionRobot
import me.proton.android.drive.ui.robot.OnboardingRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.settings.PhotosBackupRobot
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.android.drive.ui.test.ShowOnboardingBaseTest
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.test.android.robots.auth.AddAccountRobot
import me.proton.core.test.android.robots.auth.login.LoginRobot
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.core.test.rule.annotation.mapToUser
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
class OnboardingFlowTest : ShowOnboardingBaseTest() {

    @Inject lateinit var configurationProvider: ConfigurationProvider

    @Before
    fun before() {
        dcimCameraFolder.copyDirFromAssets("images/formats")
        configurationProvider.debug.photosUpsellPhotoCount = Int.MAX_VALUE
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun pressingNotNowShouldDismissOnboarding() {
        OnboardingRobot
            .verify {
                robotDisplayed()
            }
            .clickNotNow()
            .verify {
                robotDisplayed()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun denyPhotosPermissionsShouldDismissOnboarding() {
        OnboardingRobot
            .verify {
                robotDisplayed()
            }
            .clickMoreOptions()
            .denyPermissions(PhotosTabRobot)
            .verify {
                robotDisplayed()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun secondLoginShouldNotShowOnboarding() {
        OnboardingRobot
            .verify {
                robotDisplayed()
            }
            .clickNotNow()

        logoutThenLoginAgain()

        PhotosTabRobot
            .verify {
                robotDisplayed()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun goingBackFromPhotoBackupShowsMainOnboardingScreen() {
        OnboardingRobot
            .verify {
                robotDisplayed()
            }
            .clickMoreOptions()
            .allowPhotosPermissions(OnboardingRobot)
            .verify {
                assertPhotoBackupDisplayed()
            }
            .clickBack()
            .verify {
                assertMainDisplayed()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun turnOnCameraUpload() {
        OnboardingRobot
            .verify {
                robotDisplayed()
            }
            .clickEnablePhotoBackup(OnboardingRobot)
            .allowPhotosPermissions(PhotosTabRobot)
            .notificationPermissionFlow()
    }

    @Test
    @PrepareUser(loginBefore = true)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun turnOnCameraUploadForSelectedItems() {
        //TODO: In order for this to work we need to prepare firebase test device so that it have some content
        //      to show for partial permissions
        OnboardingRobot
            .verify {
                robotDisplayed()
            }
            .clickEnablePhotoBackup(OnboardingRobot)
            .allowSelectedPermissions(OnboardingRobot)
            .select()
            .clickAllow(PhotosTabRobot)
            .notificationPermissionFlow()
    }

    @Test
    @PrepareUser(loginBefore = true)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun moreOptionsTurnOnPhotoBackupForSelectedItems() {
        //TODO: In order for this to work we need to prepare firebase test device so that it have some content
        //      to show for partial permissions
        OnboardingRobot
            .verify {
                robotDisplayed()
            }
            .clickMoreOptions()
            .allowSelectedPermissions(OnboardingRobot)
            .select()
            .clickAllow(PhotosBackupRobot)
            .verify {
                robotDisplayed()
            }
            .clickBackupToggle(OnboardingRobot)
            .clickDone(PhotosTabRobot)
            .notificationPermissionFlow()
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun moreOptionsTurnOnPhotoBackup() {
        OnboardingRobot
            .verify {
                robotDisplayed()
            }
            .clickMoreOptions()
            .allowPhotosPermissions(PhotosBackupRobot)
            .verify {
                robotDisplayed()
            }
            .clickBackupToggle(OnboardingRobot)
            .clickDone(PhotosTabRobot)
            .notificationPermissionFlow()
    }

    private fun logoutThenLoginAgain() {
        loginTestHelper.logoutAll()
        // Without this sleep on emulator with API level 30 test stays on first login screen
        // and "Sign in" is not clicked
        Thread.sleep(1000)
        AddAccountRobot().signIn()
        LoginRobot().loginUser<LoginRobot>(protonRule.testDataRule.mainTestUser!!.mapToUser())
    }

    private fun PhotosTabRobot.notificationPermissionFlow() = this.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationPermissionRobot
                .verify {
                    assertRationaleDisplayed()
                }
                .clickEnableNotifications(PhotosTabRobot)
                .allowPermission(PhotosTabRobot)
                .verify {
                    assertPhotosLoadingOrContentDisplayed()
                }
        } else {
            // Nothing to do as notification permission is not needed on API < 33
            verify {
                assertPhotosLoadingOrContentDisplayed()
            }
        }
    }
}
