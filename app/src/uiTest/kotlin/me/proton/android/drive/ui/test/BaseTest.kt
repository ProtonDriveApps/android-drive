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

import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import me.proton.android.drive.test.BuildConfig
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.robot.Robot
import me.proton.android.drive.ui.rules.LogoutAllRule
import me.proton.android.drive.ui.toolkits.screenshot
import me.proton.core.auth.presentation.testing.ProtonTestEntryPoint
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.deserializeList
import me.proton.test.fusion.FusionConfig
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import java.util.concurrent.atomic.AtomicInteger

open class BaseTest {
    @Rule
    @JvmField
    val ruleChain: RuleChain = RuleChain.outerRule(testName)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 0)
    val logoutAllRule = LogoutAllRule()

    init {
        FusionConfig.Compose.testRule.set(composeTestRule)
        FusionConfig.Compose.useUnmergedTree.set(true)
        FusionConfig.Compose.onFailure = { screenshot() }
        screenshotCounter.set(0)
    }

    inline fun <T : Robot> T.verify(crossinline block: T.() -> Any): T = apply { block() }

    companion object {
        private val protonTestEntryPoint by lazy {
            EntryPointAccessors.fromApplication(
                ApplicationProvider.getApplicationContext<Application>(),
                ProtonTestEntryPoint::class.java,
            )
        }

        private val uiTestEntryPoint by lazy {
            EntryPointAccessors.fromApplication(
                ApplicationProvider.getApplicationContext<Application>(),
                UiTestEntryPoint::class.java,
            )
        }

        val loginTestHelper by lazy { protonTestEntryPoint.loginTestHelper }
        val uiTestHelper by lazy { uiTestEntryPoint.uiTestHelper }
        val testName = TestName()
        val screenshotLocation get() = "/sdcard/Pictures/Screenshots/${testName.methodName}/"
        val screenshotCounter = AtomicInteger(0)

        // TODO: before publishing to github, this information should be moved from assets into gitlab vars
        val users = User.Users(InstrumentationRegistry.getInstrumentation().context
            .assets
            .open("users.json")
            .bufferedReader()
            .use { it.readText() }
            .deserializeList())

        val quark = Quark(
            host = BuildConfig.HOST,
            proxyToken = BuildConfig.PROXY_TOKEN,
            InstrumentationRegistry.getInstrumentation().context
                .assets
                .open("internal_api.json")
                .bufferedReader()
                .use { it.readText() }
                .deserialize())
    }
}
