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
import me.proton.core.auth.test.MinimalSignUpExternalTests
import me.proton.core.test.quark.v2.command.jailUnban
import me.proton.test.fusion.FusionConfig
import org.junit.Before
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class SignUpFlowTest : BaseTest(
    showWelcomeScreen = true,
), MinimalSignUpExternalTests {

    override val isCongratsDisplayed = true

    @Before
    fun setTimeouts() {
        FusionConfig.Espresso.waitTimeout.set(90.seconds)
        FusionConfig.Compose.waitTimeout.set(90.seconds)
    }

    @Before
    fun preventHumanVerification() {
        quarkRule.quarkCommands.jailUnban()
    }

    override fun verifyAfter() {
        WelcomeRobot.verify { robotDisplayed() }
    }
}
