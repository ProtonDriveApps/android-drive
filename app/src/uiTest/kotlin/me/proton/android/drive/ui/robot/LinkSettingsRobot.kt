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

package me.proton.android.drive.ui.robot

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import me.proton.android.drive.ui.extension.setDate
import me.proton.core.drive.drivelink.shared.presentation.component.PrivacySettingsTestTag
import me.proton.core.drive.drivelink.shared.presentation.component.SharedDriveLinkTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.Fusion.view
import me.proton.test.fusion.FusionConfig.targetContext
import java.text.DateFormat
import java.util.Date
import me.proton.core.drive.i18n.R as I18N

object LinkSettingsRobot : NavigationBarRobot, Robot {
    private val title get() = node.withText(I18N.string.shared_link_settings)
    private val passwordToggle get() = node.withTag(PrivacySettingsTestTag.passwordSwitch)
    private val passwordTextField get() = node.withTag(PrivacySettingsTestTag.passwordTextField)
    private val expirationDateToggle get() = node.withTag(PrivacySettingsTestTag.expirationDateSwitch)
    private val expirationDateTextField get() = node.withTag(PrivacySettingsTestTag.expirationDateTextField)
    private val saveButton get() = node.withText(I18N.string.common_save_action)

    private fun messageNotificationPasswordLengthError(maxLength: Int) = node
        .withText(
            targetContext.getString(
                I18N.string.shared_link_error_message_invalid_password_length,
                maxLength
            )
        )

    private val messageNotificationSharingSettingsUpdated
        get() = node
            .withText(I18N.string.shared_link_message_update_share_url)

    fun clickPasswordToggle() = passwordToggle.clickTo(this)
    fun clickSave() = saveButton.isEnabled().clickTo(this)
    fun clickUpdateSuccessfulGrowler() = apply { messageNotificationSharingSettingsUpdated.click() }
    fun typePassword(password: String) = apply {
        passwordTextField.clearText()
        passwordTextField.typeText(password)
    }

    fun clearPassword() = apply {
        passwordTextField.clearText()
    }

    fun clickExpirationDateTextField() = expirationDateTextField
        .clickTo(PickerRobot)

    fun passwordToggleIsOn() = passwordToggle.await { assertIsAsserted() }
    fun passwordToggleIsOff() = passwordToggle.await { assertIsNotAsserted() }
    fun expirationDateToggleIsOn() = expirationDateToggle.await { assertIsAsserted() }
    fun expirationDateToggleIsOff() = expirationDateToggle.await { assertIsNotAsserted() }

    fun passwordLengthErrorWasShown(maxLength: Int) =
        messageNotificationPasswordLengthError(maxLength)
            .await { assertIsDisplayed() }

    fun expirationDateIsShown(date: Date) = expirationDateTextField
        .await {
            assertContainsText(
                DateFormat
                    .getDateInstance(DateFormat.MEDIUM)
                    .format(date)
            )
        }

    override fun robotDisplayed() {
        title.await { assertIsDisplayed() }
    }

    object DiscardChanges : Robot {
        private val dialogTitle get() = node.withText(I18N.string.shared_link_dialog_title_discard_changes)
        private val discardButton get() = node.withText(I18N.string.common_discard_action)

        fun clickDiscard() = discardButton.clickTo(FilesTabRobot)

        fun <T : Robot> clickDiscard(goesTo: T): T = discardButton.clickTo(goesTo)

        override fun robotDisplayed() {
            dialogTitle.await { assertIsDisplayed() }
        }
    }

    object PickerRobot : Robot {
        private val okButton get() = view.withId(android.R.id.button1)
        private val picker get() = view.instanceOf(android.widget.DatePicker::class.java)

        fun clickOk() = LinkSettingsRobot.apply { okButton.click() }

        fun selectDate(date: Date) = apply {
            picker.setDate(date, normalizeMonthOfYear = false)
        }

        override fun robotDisplayed() {
            okButton.await { checkIsDisplayed() }
        }
    }
}
