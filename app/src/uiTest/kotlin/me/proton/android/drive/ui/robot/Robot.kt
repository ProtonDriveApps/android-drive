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

package me.proton.android.drive.ui.robot

import android.app.Instrumentation
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.ui.test.BaseTest.Companion.composeTestRule
import me.proton.android.drive.ui.test.BaseTest.Companion.screenshotLocation
import me.proton.android.drive.ui.test.BaseTest.Companion.testName
import me.proton.core.drive.folder.create.presentation.R
import me.proton.core.test.android.instrumented.ProtonTest.Companion.testTag
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.EMPTY_STRING
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface Robot {
    val instrumentation: Instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val waitForScreenDuration: Duration get() = 10.seconds
    val waitForItemToAppearInList: Duration get() = 30.seconds
    val watchInterval: Duration get() = 100.milliseconds
    val shouldUseUnmergedTree: Boolean get() = true

    fun robotDisplayed()

    fun node(
        vararg semanticsMatchers: SemanticsMatcher,
        useUnmergedTree: Boolean = shouldUseUnmergedTree,
    ): SemanticsNodeInteraction =
        composeTestRule.onNode(getFinalSemanticMatcher(semanticsMatchers), useUnmergedTree)

    fun nodes(
        vararg semanticsMatchers: SemanticsMatcher,
        useUnmergedTree: Boolean = shouldUseUnmergedTree,
    ): SemanticsNodeInteractionCollection =
        composeTestRule.onAllNodes(getFinalSemanticMatcher(semanticsMatchers), useUnmergedTree)

    private fun getFinalSemanticMatcher(
        semanticsMatchers: Array<out SemanticsMatcher>
    ): SemanticsMatcher {
        var finalSemanticsMatcher = semanticsMatchers.first()
        semanticsMatchers.drop(1).forEach {
            finalSemanticsMatcher = finalSemanticsMatcher.and(it)
        }
        return finalSemanticsMatcher
    }

    /**
     * Common semantics matchers
     */
    fun hasTextResource(
        @StringRes resourceId: Int,
        substring: Boolean = false,
        ignoreCase: Boolean = false,
        formatString: String = EMPTY_STRING
    ): SemanticsMatcher = hasText(
        StringUtils.stringFromResource(resourceId, substring, ignoreCase).format(formatString)
    )

    private fun successGrowler(itemName: String) = node(
        hasText(
            StringUtils.stringFromResource
                (R.string.folder_create_successful, itemName))
    )

    /**
     * Common user actions
     */
    fun <T : Robot> SemanticsNodeInteraction.tryToClickAndGoTo(goesTo: T): T =
        waitFor(goesTo) { performClick() }

    fun <T : Robot> SemanticsNodeInteraction.tryToTypeText(text: String, goesTo: T): T =
        waitFor(goesTo) { performTextInput(text) }

    fun <T : Robot> SemanticsNodeInteraction.clearText(goesTo: T): T =
        waitFor(goesTo) { performTextClearance() }

    fun <T : Robot> SemanticsNodeInteraction.tryPerformTouchInputAndGoTo(
        goesTo: T,
        touchInput: TouchInjectionScope.() -> Unit
    ): T = waitFor(goesTo) { performTouchInput { touchInput() } }

    fun <T : Robot> dismissSuccessGrowler(itemName: String, goesTo: T) =
        successGrowler(itemName).tryToClickAndGoTo(goesTo)


    /**
     * Other extensions and Helpers
     */
    fun <T : Robot> waitFor(
        goesTo: T,
        timeout: Duration = waitForScreenDuration,
        interval: Duration = watchInterval,
        block: () -> Any,
    ): T {
        var error: Throwable = TimeoutException("Condition not met in ${timeout}ms")
        try {
            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(timeout.inWholeMilliseconds) {
                try {
                    block()
                    true
                } catch (e: AssertionError) {
                    // Thrown on Compose failed actions and assertions
                    error = handleTestError(e, 1)
                    runBlocking { delay(interval) }
                    false
                } catch (e: IllegalStateException) {
                    // Thrown when Compose view is not ready
                    error = handleTestError(e, 0)
                    runBlocking { delay(interval) }
                    false
                }
            }
        } catch (e: ComposeTimeoutException) {
            CoreLogger.e(testTag, e)
            composeTestRule
                .onAllNodes(SemanticsMatcher("isRoot") { it.isRoot })
                .onFirst()
                .screenshot("${screenshotLocation}/${testName.methodName}.png")
            throw error
        }
        return goesTo
    }

    private fun handleTestError(throwable: Throwable, messageLine: Int): Throwable {
        val lines = throwable.message?.lines()
        if (!lines.isNullOrEmpty() && messageLine < lines.size) {
            CoreLogger.i(testTag, "Condition not yet met: ${lines[messageLine]}")
            return throwable
        }
        val message = "Could not extract message line at position $messageLine. Printing full message"
        CoreLogger.e(testTag, throwable, message)
        return throwable
    }

    private fun SemanticsNodeInteraction.screenshot(file: String) {
        try {
            val bitmap = captureToImage().asAndroidBitmap()
            FileOutputStream(file).use { destination ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, destination)
            }
        } catch (throwable: Throwable) {
            when (throwable) {
                is FileNotFoundException -> "File not found"
                is OutOfMemoryError -> "Out of memory"
                else -> "Unknown error"
            }.let {
                CoreLogger.e(testTag, throwable, "Could not take screenshot: $it")
            }
        }
    }

    fun nodeWithTextDisplayed(@StringRes stringRes: String) {
        node(hasText(stringRes)).assertIsDisplayed()
    }

    fun nodeWithTextResourceDisplayed(@StringRes stringRes: Int, formatAttr: Any = EMPTY_STRING) {
        node(hasTextResource(stringRes, formatString = formatAttr.toString())).assertIsDisplayed()
    }
}
