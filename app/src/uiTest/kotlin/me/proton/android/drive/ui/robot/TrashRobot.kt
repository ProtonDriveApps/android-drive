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

import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object TrashRobot : Robot, LinksRobot, NavigationBarRobot, GrowlerRobot {
    private val trashScreen get() = node.withText(I18N.string.common_trash)
    private val optionsButton get() = node.withContentDescription(I18N.string.trash_more_options)
    private val emptyTrashButton get() = node.withText(I18N.string.title_empty_trash_action)
    private val confirmEmptyTrashButton get() = node.withText(I18N.string.files_confirm_empty_trash_confirm_action)
    private val trashIsEmptyMessage get() = node.withText(I18N.string.description_empty_trash)
    private val trashIsEmptyTitle get() = node.withText(I18N.string.title_empty_trash)
    private val trashMoreOptions get() = node.withText(I18N.string.trash_more_options)

    override fun robotDisplayed() {
        trashScreen.await { assertIsDisplayed() }
    }
    fun openMoreOptions() = apply {
        optionsButton.click()
        emptyTrashButton.await { assertIsDisplayed() }
    }
    fun clickEmptyTrash() = apply {
        emptyTrashButton.click()
        confirmEmptyTrashButton.await { assertIsDisplayed() }
    }
    fun confirmEmptyTrash() = apply {
        confirmEmptyTrashButton.click()
    }
    fun confirmTrashIsEmpty() = apply {
        trashIsEmptyTitle.await { assertIsDisplayed() }
        trashIsEmptyMessage.await { assertIsDisplayed() }
        trashMoreOptions.await { assertIsNotDisplayed() }
    }
}
