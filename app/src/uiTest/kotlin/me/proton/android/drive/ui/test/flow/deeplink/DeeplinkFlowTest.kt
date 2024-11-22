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

package me.proton.android.drive.ui.test.flow.deeplink

import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.robot.ComputersTabRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.LauncherRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.SharedTabRobot
import me.proton.android.drive.ui.robot.StorageFullRobot
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.rules.UserPlan
import me.proton.android.drive.ui.test.EmptyBaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.core.test.quark.data.Plan
import me.proton.core.test.quark.data.User
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class DeeplinkFlowTest : EmptyBaseTest() {

    private val testUser = User(name = "proton_drive_${getRandomString(15)}")

    @get:Rule(order = 1)
    val userLoginRule: UserLoginRule =
        UserLoginRule(testUser, quarkCommands = quarkRule.quarkCommands)

    @Test
    fun photosTabIsDefaultForB2CUsers() {
        ActivityScenario.launch(MainActivity::class.java)
        PhotosTabRobot.verify { robotDisplayed() }
    }

    @Test
    @UserPlan(Plan.DriveProfessional)
    fun myFilesTabIsDefaultForB2BUsers() {
        ActivityScenario.launch(MainActivity::class.java)
        FilesTabRobot.verify { robotDisplayed() }
    }

    @Test
    fun launchFiles() {
        LauncherRobot.launch("files", FilesTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    fun homeFiles() {
        LauncherRobot.deeplinkTo(Screen.Files(userLoginRule.userId!!), FilesTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    fun launchPhotos() {
        LauncherRobot.launch("photos", PhotosTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    fun homePhotos() {
        LauncherRobot.deeplinkTo(Screen.Photos(userLoginRule.userId!!), PhotosTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    fun launchComputers() {
        LauncherRobot.launch("computers", ComputersTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    fun homeComputers() {
        LauncherRobot.deeplinkTo(Screen.Computers(userLoginRule.userId!!), ComputersTabRobot)
            .verify { robotDisplayed() }
    }


    @Test
    fun launchShared() {
        LauncherRobot.launch("shared_tabs", SharedTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    fun homeShared() {
        LauncherRobot.deeplinkTo(Screen.SharedTabs(userLoginRule.userId!!), SharedTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    fun storageFull() {
        LauncherRobot.deeplinkTo(
            Screen.Dialogs.StorageFull(userLoginRule.userId!!),
            StorageFullRobot
        )
            .verify { robotDisplayed() }
            .clickDismiss(PhotosTabRobot)
            .verify { robotDisplayed() }
    }
}
