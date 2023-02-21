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

package me.proton.android.drive.ui.dialog

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import me.proton.android.drive.ui.viewmodel.ShareState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SendFileDialogTest {
    private val uiDevice = UiDevice.getInstance(getInstrumentation())

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pressBackShouldNotDismissProgressDialog() {
        // Given - dialog is shown
        setupSendFileProgressDialog()
        // When - back is pressed
        uiDevice.pressBack()
        // Then - dialog is not dismissed
        composeTestRule
            .onNodeWithTag(PROGRESS_DIALOG_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun pressOutsideOfDialogShouldNotDismissProgressDialog() {
        // Given - dialog is shown
        setupSendFileProgressDialog()
        // When - outside of a dialog is pressed
        uiDevice.click(0, statusBarHeight() + 1)
        // Then - dialog is not dismissed
        composeTestRule
            .onNodeWithTag(PROGRESS_DIALOG_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun pressOnCancelButtonShouldDismissProgressDialog() {
        // Given - dialog is shown
        setupSendFileProgressDialog()
        // When - dialog cancel button is pressed
        composeTestRule
            .onNodeWithTag(CancelButtonTestTag)
            .performClick()
        // Then - dialog is dismissed
        composeTestRule
            .onNodeWithTag(PROGRESS_DIALOG_TEST_TAG)
            .assertDoesNotExist()
    }

    private fun setupSendFileProgressDialog() {
        composeTestRule.setupDialog { navController ->
            ProgressDialog(
                state = ShareState.Decrypting,
                title = "Send file progress dialog test",
                modifier = Modifier.testTag(PROGRESS_DIALOG_TEST_TAG),
                onDismiss = { Unit.also { navController.navigateUp() } },
            )
        }
        showDialog()
    }

    private fun showDialog() {
        composeTestRule
            .onNodeWithText(SHOW_DIALOG_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun statusBarHeight(): Int {
        val resources = getInstrumentation().context.resources
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) {
            resources.getDimensionPixelSize(resId)
        } else {
            100
        }
    }

    companion object {
        private const val PROGRESS_DIALOG_TEST_TAG = "progress dialog"
    }
}
