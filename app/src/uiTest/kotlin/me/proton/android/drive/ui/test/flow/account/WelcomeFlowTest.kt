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

package me.proton.android.drive.ui.test.flow.account

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.WelcomeRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.auth.test.robot.AddAccountRobot
import me.proton.core.test.quark.data.Plan
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.command.jailUnban
import me.proton.core.test.quark.v2.command.userCreate
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class WelcomeFlowTest : BaseTest(showWelcomeScreen = true) {

    private val user = User(plan = Plan.MailPlus).also {
        quarkRule.quarkCommands.userCreate(it)
    }

    @Before
    fun preventHumanVerification() {
        quarkRule.quarkCommands.jailUnban()

        AddAccountRobot
            .clickSignIn()
            .login(user)

        WelcomeRobot
            .verify {
                robotDisplayed()
            }
    }

    @Test
    fun goThroughWelcomeScreenBySwipingAndClickingNext() {
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
        WelcomeRobot
            .clickSkip()
            .verify {
                robotDisplayed()
            }
    }
}
