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

object ShareRobot : NavigationBarRobot, Robot {
    private val contentShareScreen get() = node.withTag(SharedDriveLinkTestTag.content)
    private val passwordToggle get() = node.withTag(PrivacySettingsTestTag.passwordSwitch)
    private val passwordTextField get() = node.withTag(PrivacySettingsTestTag.passwordTextField)
    private val expirationDateToggle get() = node.withTag(PrivacySettingsTestTag.expirationDateSwitch)
    private val expirationDateTextField get() = node.withTag(PrivacySettingsTestTag.expirationDateTextField)
    private val saveButton get() = node.withText(I18N.string.common_save_action)
    private val stopSharingButton get() = node.withText(I18N.string.common_stop_sharing_action)
    private val copyPasswordAction get() = node.withText(I18N.string.shared_link_action_copy_password)
    private val accessibilityDescription get() = node.withTag(SharedDriveLinkTestTag.accessibilityDescription)
    private val messageNotificationPasswordCopiedToClipboard
        get() = node
            .withText(
                targetContext.getString(
                    I18N.string.common_in_app_notification_copied_to_clipboard,
                    targetContext.getString(I18N.string.common_password),
                )
            )

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

    fun clickCopyPassword() = apply {
        contentShareScreen.scrollTo(copyPasswordAction)
        copyPasswordAction.click()
    }

    fun clickExpirationDateTextField() = expirationDateTextField
        .clickTo(PickerRobot)

    fun clickStopSharing() = ConfirmStopSharingRobot.apply {
        stopSharingButton.scrollTo().click()
    }

    fun passwordToggleIsOn() = passwordToggle.await { assertIsAsserted() }
    fun passwordToggleIsOff() = passwordToggle.await { assertIsNotAsserted() }
    fun expirationDateToggleIsOn() = expirationDateToggle.await { assertIsAsserted() }
    fun expirationDateToggleIsOff() = expirationDateToggle.await { assertIsNotAsserted() }
    fun publicAccessibilityDescriptionWasShown(
        isFile: Boolean = true,
    ) = accessibilityDescription.await {
        assertContainsText(
            targetContext.getString(
                I18N.string.shared_link_accessibility_description_public,
                targetContext.getString(
                    if (isFile) I18N.string.shared_link_file
                    else I18N.string.shared_link_folder
                ),
            )
        )
    }

    fun passwordProtectedAccessibilityDescriptionWasShown(
        isFile: Boolean = true,
    ) = accessibilityDescription.await {
        assertContainsText(
            targetContext.getString(
                I18N.string.shared_link_accessibility_description_password_protected,
                targetContext.getString(
                    if (isFile) I18N.string.shared_link_file
                    else I18N.string.shared_link_folder
                ),
            )
        )
    }

    fun passwordCopiedToClipboardWasShown() = messageNotificationPasswordCopiedToClipboard
        .await { assertIsDisplayed() }

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

    fun verifyShareLinkFolder(name: String) = verifyShareLink(
        name = name,
        type = targetContext.resources.getString(I18N.string.shared_link_folder)
    )
    fun verifyShareLinkFile(name: String) = verifyShareLink(
        name = name,
        type = targetContext.resources.getString(I18N.string.shared_link_file)
    )

    private fun verifyShareLink(name: String, type: String) {
        node.withText(
            targetContext.resources.getString(I18N.string.shared_link_accessibility_description_public)
                .format(type)
        ).hasSibling(node.withText(name)).await {
            assertIsDisplayed()
        }
    }

    override fun robotDisplayed() {
        contentShareScreen.await { assertIsDisplayed() }
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

        fun clickOk() = ShareRobot.apply { okButton.click() }

        fun selectDate(date: Date) = apply {
            picker.setDate(date, normalizeMonthOfYear = false)
        }

        override fun robotDisplayed() {
            okButton.await { checkIsDisplayed() }
        }
    }
}
