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

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig.targetContext
import me.proton.core.drive.i18n.R as I18N

object ManageAccessRobot : NavigationBarRobot, Robot {
    private val title get() = node.withText(I18N.string.title_manage_access)
    private val shareButton get() = node.withContentDescription(I18N.string.common_share)
    private val shareWithAnyoneSwitch
        get() = node.isCheckable()
            .hasDescendant(node.withText(I18N.string.manage_access_share_with_anyone))
    private val allowToAnyonePublicText get() = node.withText(I18N.string.manage_access_link_description_public)
    private val allowToAnyonePasswordProtectedText
        get() = node.withText(I18N.string.manage_access_link_description_password_protected)
    private val shareViewerPermission get() = node.withText(I18N.string.manage_access_link_viewer_permission)
    private val shareEditorPermission get() = node.withText(I18N.string.manage_access_link_editor_permission)
    private val linkSettingsButton get() = node.withText(I18N.string.manage_access_link_settings_action)
    private val copyLinkButton get() = node.withText(I18N.string.common_copy_link_action)
    private val messageNotificationPasswordCopiedToClipboard
        get() = node
            .withText(
                targetContext.getString(
                    I18N.string.common_in_app_notification_copied_to_clipboard,
                    targetContext.getString(I18N.string.common_password),
                )
            )
    fun clickSettings() = linkSettingsButton.clickTo(LinkSettingsRobot)

    fun clickAllowToAnyone() = shareWithAnyoneSwitch.clickTo(this)

    fun clickViewerPermissions() = shareViewerPermission.clickTo(ShareLinkPermissionsOptionRobot)

    fun <T : Robot> clickAllowToAnyone(goesTo: T) = shareWithAnyoneSwitch.clickTo(goesTo)
    fun clickShare() = ShareUserRobot.apply {
        shareButton.click()
    }

    fun clickInvitation(text: String) =
        node.withText(text).clickTo(ShareInvitationOptionRobot)

    fun clickMember(text: String) =
        node.withText(text).clickTo(ShareMemberOptionRobot)

    fun assertUserIsNotEditable(text: String) {
        node.hasDescendant(node.withText(text))
            .isClickable().await { assertDisabled() }
    }

    fun <T : Robot> clickStopSharing(goesTo: T) =
        node.withText(I18N.string.share_via_invitations_stop_sharing_title).clickTo(goesTo)

    fun clickCopyPassword() = apply {
        copyLinkButton.click()
    }

    fun passwordCopiedToClipboardWasShown() = messageNotificationPasswordCopiedToClipboard
        .await { assertIsDisplayed() }

    fun assertLinkIsShareWithAnyonePublic(permissions: Permissions = Permissions.viewer) {
        assertLinkIsShareWithAnyone(permissions)
        allowToAnyonePublicText.await { assertIsDisplayed() }
    }

    fun assertLinkIsShareWithAnyonePasswordProtected(permissions: Permissions = Permissions.viewer) {
        assertLinkIsShareWithAnyone(permissions)
        allowToAnyonePasswordProtectedText.await { assertIsDisplayed() }
    }

    private fun assertLinkIsShareWithAnyone(permissions: Permissions) {
        if (permissions == Permissions.viewer) {
            shareViewerPermission.await { assertIsDisplayed() }
        } else {
            shareEditorPermission.await { assertIsDisplayed() }
        }
        shareWithAnyoneSwitch.await { assertIsAsserted() }
        copyLinkButton.await { assertIsDisplayed() }
        linkSettingsButton.await { assertIsDisplayed() }
    }

    fun assertLinkIsNotShareWithAnyonePublic() {
        shareWithAnyoneSwitch.await { assertIsOff() }
        copyLinkButton.await { assertDoesNotExist() }
        linkSettingsButton.await { assertDoesNotExist() }
    }

    fun assertSharedWith(email: String) {
        node.withText(email).await { assertIsDisplayed() }
    }

    fun assertSharedWithViewer(email: String) {
        assertSharedWithEditor(
            email = email,
            role = targetContext.getString(I18N.string.common_permission_viewer)
        )
    }

    fun assertSharedWithEditor(email: String) {
        assertSharedWithEditor(
            email = email,
            role = targetContext.getString(I18N.string.common_permission_editor)
        )
    }

    private fun assertSharedWithEditor(email: String, role: String) {
        node.withText(email)
            .hasSibling(node.withText(role))
            .await { assertIsDisplayed() }
    }

    fun assertNotSharedWith(email: String) {
        node.withText(email).await { assertDoesNotExist() }
    }

    fun assertInvitedWithViewer(email: String) {
        assertInvitedWithRole(
            email = email,
            role = targetContext.getString(I18N.string.common_permission_viewer)
        )
    }

    fun assertInvitedWithEditor(email: String) {
        assertInvitedWithRole(
            email = email,
            role = targetContext.getString(I18N.string.common_permission_editor)
        )
    }

    private fun assertInvitedWithRole(email: String, role: String) {
        node.withText(email)
            .hasSibling(node.withText(role))
            .await { assertIsDisplayed() }
    }

    fun assertExternalInvitedWithViewer(email: String) {
        assertExternalInvitedWithRole(
            email = email,
            role = targetContext.getString(I18N.string.common_permission_viewer)
        )
    }

    fun assertExternalInvitedWithEditor(email: String) {
        assertExternalInvitedWithRole(
            email = email,
            role = targetContext.getString(I18N.string.common_permission_editor)
        )
    }

    private fun assertExternalInvitedWithRole(email: String, role: String) {
        val pendingInvitation =
            targetContext.getString(I18N.string.share_via_invitations_external_invitation_pending)
        node.withText(email)
            .hasSibling(node.withText(pendingInvitation.format(role)))
            .await { assertIsDisplayed() }
    }

    fun assertMemberWithViewer(email: String) {
        node.withText(email)
            .hasSibling(node.withText(I18N.string.common_permission_viewer))
            .await { assertIsDisplayed() }
    }

    fun assertMemberWithEditor(email: String) {
        node.withText(email)
            .hasSibling(node.withText(I18N.string.common_permission_editor))
            .await { assertIsDisplayed() }
    }

    fun assertViewerPermissionsIsNotClickable() = shareViewerPermission.await { assertIsNotClickable() }

    override fun robotDisplayed() {
        title.await { assertIsDisplayed() }
    }
}
