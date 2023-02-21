/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.ui.test.flow

import androidx.test.ext.junit.runners.AndroidJUnit4
import me.proton.android.drive.ui.robot.WelcomeRobot
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.test.android.robots.auth.AddAccountRobot
import me.proton.core.test.android.robots.auth.login.LoginRobot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeFlowTest : BaseTest() {

    private val user = users.getUser { it.isPaid }

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(true)

    @Test
    fun goThroughWelcomeScreenBySwipingAndClickingNext() {
        AddAccountRobot()
            .signIn()
            .loginUser<LoginRobot>(user)

        WelcomeRobot
            .verify {
                robotDisplayed()
            }

        WelcomeRobot
            .clickNext()
            .clickNext()
            .clickNext()
    // swipe back to initial onboarding window
            .swipeRight()
            .swipeRight()
            .swipeRight()
    // swipe to Get Started window
            .swipeLeft()
            .swipeLeft()
            .swipeLeft()
            .clickGetStarted()
            .verify {
                robotDisplayed()
            }
    }

    @Test
    fun skipWelcomeScreen() {
        AddAccountRobot()
            .signIn()
            .loginUser<LoginRobot>(user)

        WelcomeRobot
            .verify {
                robotDisplayed()
            }

        WelcomeRobot
            .clickSkip()
            .verify {
                robotDisplayed()
            }
    }
}
