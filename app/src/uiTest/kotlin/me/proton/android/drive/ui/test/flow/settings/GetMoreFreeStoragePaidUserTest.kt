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

package me.proton.android.drive.ui.test.flow.settings

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.core.test.quark.data.Plan
import me.proton.core.test.quark.data.User
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class GetMoreFreeStoragePaidUserTest : BaseTest() {
    private val paidUser = User(
        name = "proton_drive_${getRandomString(15)}",
        plan = Plan.Unlimited,
    )

    @get:Rule(order = 1)
    val userLoginRule: UserLoginRule = UserLoginRule(paidUser, quarkCommands = quarkRule.quarkCommands)

    @Test
    fun paidUserShouldNotHaveGetMoreFreeStorageOption() {
        FilesTabRobot
            .openSidebarBySwipe()
            .scrollToStorageIndicator()
            .verify {
                robotDisplayed()
                assertGetMoreFreeStorageIsNotDisplayed()
            }
    }
}
