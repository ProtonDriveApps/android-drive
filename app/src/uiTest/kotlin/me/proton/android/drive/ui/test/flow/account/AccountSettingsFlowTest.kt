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
import io.mockk.every
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.accountrecovery.dagger.CoreAccountRecoveryFeaturesModule
import me.proton.core.accountrecovery.domain.IsAccountRecoveryEnabled
import me.proton.core.accountrecovery.domain.IsAccountRecoveryResetEnabled
import me.proton.core.test.quark.Quark
import me.proton.core.usersettings.test.MinimalUserSettingsTest
import org.junit.Before
import org.junit.Rule
import org.junit.rules.ExternalResource
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(CoreAccountRecoveryFeaturesModule::class)
class AccountSettingsFlowTest : BaseTest(), MinimalUserSettingsTest {

    override val quark: Quark = quarkRule.quark

    @get:Rule(order = 1)
    val initFeaturesRule = object : ExternalResource() {
        override fun before() {
            every { isAccountRecoveryEnabled(any()) } returns true
            every { isAccountRecoveryResetEnabled(any()) } returns true
        }
    }

    @Inject
    internal lateinit var isAccountRecoveryEnabled: IsAccountRecoveryEnabled

    @Inject
    internal lateinit var isAccountRecoveryResetEnabled: IsAccountRecoveryResetEnabled

    @Before
    fun preventHumanVerification() {
        quark.jailUnban()
    }

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
