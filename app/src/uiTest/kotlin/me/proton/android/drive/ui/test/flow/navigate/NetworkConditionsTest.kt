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

package me.proton.android.drive.ui.test.flow.navigate

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.MoveToFolderRobot.clickRetry
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.android.drive.utils.network.NetworkInterceptor
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class NetworkConditionsTest: AuthenticatedBaseTest() {

    @Scenario(2)
    @Test
    fun retryNavigateToFolderWithNoConnection() {
        val folder1 = "folder1"
        val file2 = "file2"

        FilesTabRobot
            .verify {
                itemIsDisplayed(folder1)
            }

        NetworkInterceptor.simulateUnknownHost()

        FilesTabRobot
            .clickOnFolder(folder1)
            .verify {
                retryGrowlerIsDisplayed()
            }

        NetworkInterceptor.reset()

        FilesTabRobot
            .clickRetry()
            .verify {
                itemIsDisplayed(file2)
            }
    }
}
