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
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class DeeplinkFlowTest : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    fun launchFiles() {
        LauncherRobot.launch("files", FilesTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun homeFiles() {
        val id = protonRule.testDataRule.mainTestUser!!.id
        LauncherRobot.deeplinkTo(
            Screen.Files(UserId(id)),
            FilesTabRobot
        )
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun launchPhotos() {
        LauncherRobot.launch("photos", PhotosTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun homePhotos() {
        val id = protonRule.testDataRule.mainTestUser!!.id
        LauncherRobot.deeplinkTo(
            Screen.PhotosAndAlbums(UserId(id)),
            PhotosTabRobot
        )
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun launchComputers() {
        LauncherRobot.launch("computers", ComputersTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun homeComputers() {
        val id = protonRule.testDataRule.mainTestUser!!.id
        LauncherRobot.deeplinkTo(
            Screen.Computers(UserId(id)),
            ComputersTabRobot
        )
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun launchShared() {
        LauncherRobot.launch("shared_tabs", SharedTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun homeShared() {
        val id = protonRule.testDataRule.mainTestUser!!.id
        LauncherRobot.deeplinkTo(
            Screen.SharedTabs(UserId(id)),
            SharedTabRobot
        )
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun storageFull() {
        val id = protonRule.testDataRule.mainTestUser!!.id
        LauncherRobot.deeplinkTo(
            Screen.Dialogs.StorageFull(UserId(id)),
            StorageFullRobot
        )
            .verify { robotDisplayed() }
            .clickDismiss(PhotosTabRobot)
            .verify { robotDisplayed() }
    }
}
