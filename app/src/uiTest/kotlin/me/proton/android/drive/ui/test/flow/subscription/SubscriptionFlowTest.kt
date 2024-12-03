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

package me.proton.android.drive.ui.test.flow.subscription

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.proton.android.drive.initializer.MainInitializer
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.SlowTestRule
import me.proton.android.drive.ui.test.AbstractBaseTest
import me.proton.android.drive.ui.test.AbstractBaseTest.Companion.configureFusion
import me.proton.core.plan.test.MinimalSubscriptionTests
import me.proton.core.plan.test.robot.SubscriptionRobot
import me.proton.core.test.rule.ProtonRule
import me.proton.core.test.rule.extension.protonAndroidComposeRule
import org.junit.Rule

@HiltAndroidTest
open class SubscriptionFlowTest : MinimalSubscriptionTests() {

    @get:Rule(order = 2)
    val protonRule: ProtonRule = protonAndroidComposeRule<MainActivity>(
        fusionEnabled = true,
        additionalRules = linkedSetOf(SlowTestRule()),
        beforeHilt = { configureFusion() },
        afterHilt = {
            MainInitializer.init(it.targetContext)
            CoroutineScope(Dispatchers.Main).launch {
                AbstractBaseTest.uiTestHelper.doNotShowOnboardingAfterLogin()
            }
        },
        logoutBefore = true
    )

    override fun startSubscription(): SubscriptionRobot {
        PhotosTabRobot
            .robotDisplayed()
        PhotosTabRobot
            .clickSidebarButton()
            .clickSubscription()
        return SubscriptionRobot
    }
}
