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

package me.proton.android.drive.ui.test.flow.account

import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.test.ExternalStorageBaseTest
import me.proton.core.accountrecovery.dagger.CoreAccountRecoveryFeaturesModule
import me.proton.core.usersettings.test.MinimalUserSettingsTest

@HiltAndroidTest
@UninstallModules(CoreAccountRecoveryFeaturesModule::class)
open class AccountSettingsFlowTest : ExternalStorageBaseTest(), MinimalUserSettingsTest {

    /**
     * Check TestCoreFeaturesModule for proper mocks set for provideIsAccountRecoveryEnabled,
     * provideIsAccountRecoveryResetEnabled and provideIsNotificationsEnabled.
     * Currently all of them are set to true.
     */

    private fun startAccountSettings() = PhotosTabRobot
        .openSidebarBySwipe()
        .verify { robotDisplayed() }
        .clickSettings()
        .clickAccount()

    override fun startPasswordManagement() {
        startAccountSettings().clickPasswordManagement()
    }

    override fun startRecoveryEmail() {
        startAccountSettings().clickRecoveryEmail()
    }
}
