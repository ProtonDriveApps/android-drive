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
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import me.proton.android.drive.test.BuildConfig
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.robot.Robot
import me.proton.android.drive.ui.rules.LogoutAllRule
import me.proton.core.auth.presentation.testing.ProtonTestEntryPoint
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.deserializeList
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestName

typealias AndroidComposeRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

open class BaseTest {
    @Rule
    @JvmField
    val ruleChain: RuleChain = RuleChain.outerRule(testName)

    @get:Rule
    val composeTestRule: AndroidComposeRule = createAndroidComposeRule()

    @get:Rule(order = 0)
    val logoutAllRule = LogoutAllRule()

    init {
        setGlobalComposeRule(composeTestRule)
    }

    inline fun <T : Robot> T.   verify(crossinline block: T.() -> Any): T =
        apply { waitFor(this) { block() } }

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

        private var globalComposeRule: AndroidComposeRule? = null

        val loginTestHelper by lazy { protonTestEntryPoint.loginTestHelper }
        val uiTestHelper by lazy { uiTestEntryPoint.uiTestHelper }
        val testName = TestName()
        val screenshotLocation: String get() = "/sdcard/Pictures"
        val composeTestRule: AndroidComposeRule
            get() = globalComposeRule
                ?: throw AssertionError(
                    "Compose test rule was not set. Make sure to call setGlobalComposeRule(rule) first"
                )

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

        fun setGlobalComposeRule(rule: AndroidComposeRule) {
            globalComposeRule = rule
        }
    }
}
