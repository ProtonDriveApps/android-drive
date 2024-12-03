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

package me.proton.android.drive.ui.test

import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.initializer.MainInitializer
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.android.drive.ui.rules.SlowTestRule
import me.proton.core.test.rule.ProtonRule
import me.proton.core.test.rule.extension.protonAndroidComposeRule
import org.junit.Rule
import org.junit.rules.RuleChain

@HiltAndroidTest
abstract class ExternalStorageBaseTest : AbstractBaseTest() {

    private val permissionRule: GrantPermissionRule = runBlocking {
        GrantPermissionRule.grant(*externalStoragePermissions.toTypedArray())
    }

    val protonRule: ProtonRule = protonAndroidComposeRule<MainActivity>(
        annotationTestData = driveTestDataRule.scenarioAnnotationTestData,
        fusionEnabled = true,
        additionalRules = linkedSetOf(
            IntentsRule(),
            SlowTestRule()
        ),
        beforeHilt = {
            configureFusion()
        },
        afterHilt = {
            MainInitializer.init(it.targetContext)
            setOnboardingDisplayStateAfterLogin()
            setWhatsNewDisplayStateAfterLogin()
        },
        logoutBefore = true
    )

    @get:Rule(order = 2)
    val ruleChain: RuleChain = RuleChain
        .outerRule(permissionRule)
        .around(protonRule)

    @get:Rule(order = 3)
    val externalFilesRule = ExternalFilesRule()
}
