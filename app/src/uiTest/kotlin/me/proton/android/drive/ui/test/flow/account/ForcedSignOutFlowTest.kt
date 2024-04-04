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

package me.proton.android.drive.ui.test.flow.account

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.auth.test.robot.AddAccountRobot
import me.proton.core.test.quark.v2.command.expireSession
import me.proton.test.fusion.Fusion
import me.proton.test.fusion.ui.compose.ComposeWaiter.waitFor
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class ForcedSignOutFlowTest : AuthenticatedBaseTest() {

    @Test
    fun forcedSignOut() {
        PhotosTabRobot.verify {
            robotDisplayed()
        }

        quarkRule.quarkCommands.expireSession(
            username = testUser.name,
            expireRefreshToken = true
        )

        Fusion.node.isEnabled().waitFor(120.seconds) {
            AddAccountRobot.uiElementsDisplayed()
        }
    }
}
