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

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.extension.debug
import me.proton.android.drive.ui.robot.ForceUpdateRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class UpdateFlowTest : BaseTest() {

    @Inject
    lateinit var configurationProvider: ConfigurationProvider

    @Test
    @PrepareUser(loginBefore = true)
    fun deprecatedVersion() {
        configurationProvider.debug.appVersionHeader = "android-drive@1.0.0"

        ForceUpdateRobot
            .verify { robotDisplayed() }
    }
}
