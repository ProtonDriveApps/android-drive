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

import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.report.test.MinimalReportInternalTests
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User
import me.proton.core.util.kotlin.deserializeList
import org.junit.Before

@HiltAndroidTest
class ReportFlowTest : BaseTest(), MinimalReportInternalTests {

    override val quark: Quark = quarkRule.quark
    override val users: User.Users = User.Users(
        InstrumentationRegistry.getInstrumentation().context
            .assets
            .open("users.json")
            .bufferedReader()
            .use { it.readText() }
            .deserializeList()
    )

    @Before
    fun preventHumanVerification() {
        quark.jailUnban()
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
