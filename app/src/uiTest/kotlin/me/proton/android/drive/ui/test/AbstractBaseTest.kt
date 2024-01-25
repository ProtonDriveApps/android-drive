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
import android.os.Build
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.initializer.MainInitializer
import me.proton.android.drive.ui.robot.Robot
import me.proton.android.drive.ui.rules.QuarkRule
import me.proton.android.drive.ui.rules.SlowTestRule
import me.proton.android.drive.utils.screenshot
import me.proton.core.auth.presentation.testing.ProtonTestEntryPoint
import me.proton.test.fusion.FusionConfig
import me.proton.test.fusion.FusionConfig.targetContext
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

abstract class AbstractBaseTest(
    showWelcomeScreen: Boolean,
) {
    private val permissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        else -> emptyList()
    }

    private val hiltRule = HiltAndroidRule(this)

    val quarkRule = QuarkRule()

    private val configurationRule = before {
        // Initialize components *before* injecting via Hilt.
        MainInitializer.init(targetContext)
        // Inject via Hilt/Dagger.
        hiltRule.inject()
        uiTestHelper.showWelcomeScreenAfterLogin(showWelcomeScreen)
        configureFusion()
    }

    @get:Rule(order = 0)
    val ruleChain: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(testName)
        .around(GrantPermissionRule.grant(*permissions.toTypedArray()))
        .around(configurationRule)
        .around(quarkRule)
        .around(SlowTestRule())

    fun <T : Robot> T.verify(block: T.() -> Any): T =
        apply { block() }

    private fun configureFusion() {
        FusionConfig.Compose.useUnmergedTree.set(true)
        FusionConfig.Compose.onFailure = { screenshot() }
        FusionConfig.Compose.waitTimeout.set(60.seconds)
        FusionConfig.Espresso.onFailure = { screenshot() }
        FusionConfig.Espresso.waitTimeout.set(60.seconds)
        screenshotCounter.set(0)
    }

    companion object {
        private val protonTestEntryPoint by lazy {
            EntryPointAccessors.fromApplication(targetContext, ProtonTestEntryPoint::class.java)
        }

        private val uiTestEntryPoint by lazy {
            EntryPointAccessors.fromApplication(targetContext, UiTestEntryPoint::class.java)
        }

        val loginTestHelper by lazy { protonTestEntryPoint.loginTestHelper }
        val uiTestHelper by lazy { uiTestEntryPoint.uiTestHelper }
        val testName = TestName()
        val screenshotLocation get() = "/sdcard/Pictures/Screenshots/${testName.methodName}/"
        val screenshotCounter = AtomicInteger(0)
    }
}


private fun <T> T.before(block: suspend T.() -> Any): ExternalResource =
    object : ExternalResource() {
        override fun before() {
            runBlocking {
                block()
            }
        }
    }