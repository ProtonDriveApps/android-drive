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

import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig.targetContext
import me.proton.core.drive.i18n.R as I18N

object ManageAccessRobot : NavigationBarRobot, Robot {
    private val title get() = node.withText(I18N.string.title_manage_access)
    private val shareButton get() = node.withContentDescription(I18N.string.common_share)
    private val allowToAnyonePublicSwitch
        get() = node.isCheckable()
            .hasDescendant(node.withText(I18N.string.manage_access_link_description_public))
    private val allowToAnyonePasswordProtectedSwitch
        get() = node.isCheckable()
            .hasDescendant(node.withText(I18N.string.manage_access_link_description_password_protected))
    private val linkSettingsButton get() = node.withText(I18N.string.manage_access_link_settings_action)
    private val copyLinkButton get() = node.withText(I18N.string.common_copy_link_action)
    fun clickSettings() = linkSettingsButton.clickTo(ShareRobot)

    fun clickAllowToAnyone() = allowToAnyonePublicSwitch.clickTo(this)

    fun <T : Robot> clickAllowToAnyone(goesTo: T) = allowToAnyonePublicSwitch.clickTo(goesTo)
    fun clickShare() = ShareUserRobot.apply {
        shareButton.click()
    }

    fun clickInvitation(text: String) =
        node.withText(text).clickTo(ShareInvitationOptionRobot)

    fun assertLinkIsShareWithAnyonePublic() {
        allowToAnyonePublicSwitch.await { assertIsAsserted() }
        copyLinkButton.await { assertIsDisplayed() }
        linkSettingsButton.await { assertIsDisplayed() }
    }

    fun assertLinkIsShareWithAnyonePasswordProtected() {
        allowToAnyonePasswordProtectedSwitch.await { assertIsAsserted() }
        copyLinkButton.await { assertIsDisplayed() }
        linkSettingsButton.await { assertIsDisplayed() }
    }

    fun assertLinkIsNotShareWithAnyonePublic() {
        allowToAnyonePublicSwitch.await { assertIsNotAsserted() }
        copyLinkButton.await { assertDoesNotExist() }
        linkSettingsButton.await { assertDoesNotExist() }
    }

    fun assertSharedWith(email: String) {
        node.withText(email).await { assertIsDisplayed() }
    }

    fun assertSharedWithViewer(email: String) {
        assertSharedWithEditor(
            email = email,
            role = targetContext.getString(I18N.string.share_via_invitations_permission_viewer)
        )
    }

    fun assertSharedWithEditor(email: String) {
        assertSharedWithEditor(
            email = email,
            role = targetContext.getString(I18N.string.share_via_invitations_permission_editor)
        )
    }

    private fun assertSharedWithEditor(email: String, role: String) {
        val pendingInvitation =
            targetContext.getString(I18N.string.share_via_invitations_invitation_sent)
        node.withText(email)
            .hasSibling(node.withText(pendingInvitation.format(role)))
            .await { assertIsDisplayed() }
    }

    override fun robotDisplayed() {
        title.await { assertIsDisplayed() }
    }
}
