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

package me.proton.android.drive.ui.robot.settings

import me.proton.android.drive.ui.robot.Robot
import me.proton.android.drive.ui.screen.GetMoreFreeStorage
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig.Compose.targetContext
import me.proton.core.drive.i18n.R as I18N

object GetMoreFreeStorageRobot : Robot {
    private val content = node.withTag(GetMoreFreeStorage.screen)

    fun assertTitleDisplayed(maxFreeSpace: Bytes) {
        val title = targetContext.getString(
            I18N.string.get_more_free_storage_title,
            maxFreeSpace.asHumanReadableString(targetContext, numberOfDecimals = 0),
        )
        node.withText(title).await { assertIsDisplayed() }
    }

    fun assertSubtitleDisplayed() {
        node.withText(I18N.string.get_more_free_storage_description).await {
            assertIsDisplayed()
        }
    }

    fun assertActionsDisplayed() {
        listOf(
            I18N.string.get_more_free_storage_action_upload_title,
            I18N.string.get_more_free_storage_action_link_title,
            I18N.string.get_more_free_storage_action_recovery_title,
        ).forEach { actionTitleResId ->
            assertAction(actionTitleResId)
        }
    }

    private fun assertAction(titleResId: Int) {
        node.withText(titleResId).await { assertIsDisplayed() }
    }

    override fun robotDisplayed() {
        content.await { assertIsDisplayed() }
    }
}
