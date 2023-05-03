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
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.SidebarRobot
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.report.test.MinimalReportInternalTests
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportFlowTest : BaseTest(), MinimalReportInternalTests {

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(false)

    override val quark: Quark = BaseTest.quark
    override val users: User.Users = BaseTest.users

    @Before
    fun preventHumanVerification() {
        Companion.quark.jailUnban()
    }

    override fun verifyBefore() {
        FilesTabRobot.verify { homeScreenDisplayed() }
    }

    override fun startReport() {
        FilesTabRobot
            .openSidebarBySwipe()
            .verify { robotDisplayed() }
            .clickReportBug()
    }

    override fun verifyAfter() {
        FilesTabRobot.verify { homeScreenDisplayed() }
    }
}
