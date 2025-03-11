/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.cross.platform

import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.initializer.MainInitializer
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.rules.SlowTestRule
import me.proton.android.drive.ui.test.AbstractBaseTest
import me.proton.core.test.rule.ProtonRule
import me.proton.core.test.rule.extension.protonAndroidComposeRule
import org.junit.Rule
import org.junit.rules.RuleChain

open class ConfigurableTest : AbstractBaseTest() {

    private val permissionRule: GrantPermissionRule = runBlocking {
        GrantPermissionRule.grant(*photosTestPermissions.toTypedArray())
    }

    val configurationRule = ConfigurationRule()

    val protonRule: ProtonRule = protonAndroidComposeRule<MainActivity>(
        annotationTestData = driveTestDataRule.scenarioAnnotationTestData,
        fusionEnabled = true,
        additionalRules = linkedSetOf(
            IntentsRule(),
            SlowTestRule(),
            configurationRule
        ),
        beforeHilt = {
            configureFusion()
        },
        afterHilt = {
            MainInitializer.init(it.targetContext)
            setOverlaysDisplayStateAfterLogin()
        },
        logoutBefore = false
    )

    @get:Rule(order = 2)
    val ruleChain: RuleChain = RuleChain
        .outerRule(permissionRule)
        .around(protonRule)

    val emailToShareWith get() = configurationRule.getArgString("emailToShareWith")
    val fileName get() = configurationRule.getArgString("fileName")
    val folderName get() = configurationRule.getArgString("folderName")
    val itemName get() = configurationRule.getArgString("itemName")
    val location get() = configurationRule.getArgString("location")
    val newFileName get() = configurationRule.getArgString("newFileName")
    val oldName get() = configurationRule.getArgString("oldName")
    val ownerEmail get() = configurationRule.getArgString("ownerEmail")
    val permissions get() = configurationRule.getArgString("permissions")
    val photoNames get() = configurationRule.getArgStringList("photoNames")
    val target get() = configurationRule.getArgString("target")
}
