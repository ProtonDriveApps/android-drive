/*
 * Copyright (c) 2024 Proton AG.
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

import me.proton.android.drive.ui.screen.UserInvitationTestFlag
import me.proton.core.drive.drivelink.shared.presentation.component.UserInvitationContentTestFlag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object UserInvitationRobot : Robot, NavigationBarRobot {
    private val content get() = node.withTag(UserInvitationTestFlag.content)

    fun clickAccept(name: String) = node.withText(
        I18N.string.shared_user_invitations_accept_button
    ).hasAncestor(node
        .withTag(UserInvitationContentTestFlag.item)
        .hasDescendant(node.withText(name))
    ).clickTo(UserInvitationRobot)

    fun clickDecline(name: String) = node.withText(
        I18N.string.shared_user_invitations_decline_button
    ).hasAncestor(node
        .withTag(UserInvitationContentTestFlag.item)
        .hasDescendant(node.withText(name))
    ).clickTo(UserInvitationRobot)

    fun assertAcceptSucceed() = node.withText(
        I18N.string.shared_user_invitations_accept_success
    ).await { assertIsDisplayed() }

    fun assertDeclineSucceed() = node.withText(
        I18N.string.shared_user_invitations_decline_success
    ).await { assertIsDisplayed() }

    fun assertEmpty() {
        node.withText(I18N.string.shared_user_invitations_title_empty).await { assertIsDisplayed() }
        node.withText(I18N.string.shared_user_invitations_description_empty).await { assertIsDisplayed() }
    }

    override fun robotDisplayed() {
        content.await { assertIsDisplayed() }
    }
}
