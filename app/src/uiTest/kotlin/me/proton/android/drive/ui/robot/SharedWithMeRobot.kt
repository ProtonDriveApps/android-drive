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

import me.proton.android.drive.ui.screen.SharedWithMeTestTag
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object SharedWithMeRobot : LinksRobot {
    override val filesContent get() = node.withTag(SharedWithMeTestTag.content)

    fun clickUserInvitation(count: Int) = node.withText(
        StringUtils.pluralStringFromResource(
            I18N.plurals.shared_by_me_invitation_banner_description,
            count,
            count
        )
    ).clickTo(UserInvitationRobot)

    fun assertEmpty() {
        node.withText(I18N.string.shared_with_me_empty_title).await { assertIsDisplayed() }
        node.withText(I18N.string.shared_with_me_empty_description).await { assertIsDisplayed() }
    }

    override fun robotDisplayed() {
        filesContent.await { assertIsDisplayed() }
    }
}
