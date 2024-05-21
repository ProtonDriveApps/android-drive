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

import me.proton.android.drive.ui.screen.MoveToFolderScreenTestTag
import me.proton.core.drive.base.presentation.component.TopAppBarComponentTestTag
import me.proton.core.drive.files.presentation.component.FilesTestTag.listDetailsTitle
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.ui.compose.builders.OnNode
import me.proton.core.drive.i18n.R as I18N

object MoveToFolderRobot : NavigationBarRobot, Robot, LinksRobot, GrowlerRobot {
    private val moveToFolderScreen get() = node.withTag(MoveToFolderScreenTestTag.screen)
    private val appBar get() = node.withTag(TopAppBarComponentTestTag.appBar)
    private val rootDirectoryTitle = node.withText(I18N.string.title_my_files)
    private val addFolderButton get() = node.withContentDescription(I18N.string.folder_option_create_folder)
    private val cancelButton get() = node.withText(I18N.string.move_file_dismiss_action)
    private val moveButton get() = node.withTag(MoveToFolderScreenTestTag.moveButton)

    private fun itemWithName(name: String) = node.withTag(listDetailsTitle).withText(name)

    fun assertMoveButtonIsDisabled() = moveButton.assertDisabled()

    fun assertCreateFolderButtonDoesNotExist() = addFolderButton.assertDoesNotExist()

    /**
     * Wait the folder to be loaded before clicking back,
     * to ensure that parent id is known
     * @param folder the folder
     */
    fun clickBackFromFolder(folder: String) = navigationBackButton
        .hasSibling(node.hasDescendant(node.withText(folder)))
        .clickTo(MoveToFolderRobot)

    fun clickAddFolder(folder: String) = clickAddFolder(node.withText(folder))

    fun clickAddFolderToRoot() = clickAddFolder(rootDirectoryTitle)

    private fun clickAddFolder(onNode: OnNode) = addFolderButton
        .hasAncestor(node.hasDescendant(node.withTextSubstring(I18N.string.move_to).hasSibling(onNode)))
        .clickTo(CreateFolderRobot)

    fun clickCancel() = cancelButton.clickTo(FilesTabRobot)
    /**
     * Wait the folder to be loaded before clicking on move,
     * to ensure that folder id is known
     * @param folder the folder
     */
    fun clickMoveToFolder(folder: String) = clickMoveToFolder(node.withText(folder))
    /**
     * Wait the root folder to be loaded before clicking on move,
     * to ensure that folder id is known
     */
    fun clickMoveToRoot() = clickMoveToFolder(rootDirectoryTitle)

    private fun clickMoveToFolder(onNode: OnNode) = moveButton
        .hasAncestor(node.hasDescendant(appBar.hasDescendant(onNode)))
        .clickTo(FilesTabRobot)

    override fun robotDisplayed() {
        moveToFolderScreen.await { assertIsDisplayed() }
        addFolderButton.await { assertIsDisplayed() }
        filesContent.await { assertIsDisplayed() }
        cancelButton.await { assertIsDisplayed() }
        moveButton.await { assertIsDisplayed() }
    }
}
