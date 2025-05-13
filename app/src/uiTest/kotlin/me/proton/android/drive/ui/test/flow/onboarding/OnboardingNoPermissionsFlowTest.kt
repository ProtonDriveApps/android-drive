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

import androidx.test.espresso.intent.rule.IntentsRule
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.initializer.MainInitializer
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.extension.mainUserId
import me.proton.android.drive.ui.robot.OnboardingRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.OverlayRule
import me.proton.android.drive.ui.rules.SlowTestRule
import me.proton.android.drive.ui.test.AbstractBaseTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.robots.auth.AddAccountRobot
import me.proton.core.test.android.robots.auth.login.LoginRobot
import me.proton.core.test.rule.ProtonRule
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.core.test.rule.annotation.mapToUser
import me.proton.core.test.rule.extension.protonAndroidComposeRule
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OnboardingNoPermissionsFlowTest : AbstractBaseTest() {

    override val shouldShowOnboardingAfterLogin get() = true

    @get:Rule(order = 2)
    val protonRule: ProtonRule = protonAndroidComposeRule<MainActivity>(
        annotationTestData = driveTestDataRule.scenarioAnnotationTestData,
        fusionEnabled = true,
        additionalRules = linkedSetOf(
            IntentsRule(),
            SlowTestRule(),
            OverlayRule(this),
        ),
        beforeHilt = {
            configureFusion()
        },
        afterHilt = {
            MainInitializer.init(it.targetContext)
        },
        logoutBefore = true
    )

    override val mainUserId: UserId get() = protonRule.mainUserId

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

    private fun logoutThenLoginAgain() {
        loginTestHelper.logoutAll()
        // Without this sleep on emulator with API level 30 test stays on first login screen
        // and "Sign in" is not clicked
        Thread.sleep(1000)
        AddAccountRobot().signIn()
        LoginRobot().loginUser<LoginRobot>(protonRule.testDataRule.mainTestUser!!.mapToUser())
    }

}
