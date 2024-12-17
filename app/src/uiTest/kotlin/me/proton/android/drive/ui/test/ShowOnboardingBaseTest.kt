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

import android.Manifest
import android.os.Environment
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.initializer.MainInitializer
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.android.drive.ui.rules.SlowTestRule
import me.proton.core.test.rule.ProtonRule
import me.proton.core.test.rule.extension.protonAndroidComposeRule
import org.junit.Rule
import org.junit.rules.RuleChain
import java.io.File

abstract class ShowOnboardingBaseTest : AbstractBaseTest() {

    override val shouldShowOnboardingAfterLogin get() = true

    private val permissionRule: GrantPermissionRule = runBlocking {
        GrantPermissionRule.grant(*listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ).toTypedArray())
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
    val dcimCameraFolder = ExternalFilesRule {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Camera",
        )
    }
}