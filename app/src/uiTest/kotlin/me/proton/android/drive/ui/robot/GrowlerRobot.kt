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

import me.proton.android.drive.ui.robot.ConfirmStopSharingRobot.clickTo
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

interface GrowlerRobot {
    private fun moveToTrashSuccessGrowler(quantity: Int) = node.withText(
        StringUtils.pluralStringFromResource(
            I18N.plurals.trash_operation_successful_format,
            quantity,
        )
    )
    private fun renameSuccessGrowler(newName: String) = node.withText(
        StringUtils.stringFromResource(
            I18N.string.link_rename_successful,
            newName
        )
    )

    private fun deletePermanentlySuccessGrowler(quantity: Int) = node.withText(
        StringUtils.pluralStringFromResource(
            I18N.plurals.trash_delete_operation_successful_format,
            quantity,
        )
    )

    private val retryButton get() = node.withText(I18N.string.common_retry)
    private val noInternetLabel get() = node.withText(I18N.string.common_error_no_internet)

    fun <T : Robot> dismissFolderCreateSuccessGrowler(itemName: String, goesTo: T) =
        node
            .withText(
                StringUtils.stringFromResource(
                    I18N.string.folder_create_successful,
                    itemName,
                )
            )
            .clickTo(goesTo)

    fun <T : Robot> dismissMoveToTrashSuccessGrowler(quantity: Int, goesTo: T) =
        moveToTrashSuccessGrowler(quantity).clickTo(goesTo)

    fun <T : Robot> dismissRenameSuccessGrowler(newName: String, goesTo: T) =
        renameSuccessGrowler(newName).clickTo(goesTo)


    fun <T : Robot> dismissDeleteSuccessGrowler(quantity: Int, goesTo: T) =
        deletePermanentlySuccessGrowler(quantity).clickTo(goesTo)

    fun <T : Robot> T.clickRetry(): T = apply {
        retryButton.click()
    }

    fun retryGrowlerIsDisplayed() {
        retryButton.await { assertIsDisplayed() }
        noInternetLabel.assertIsDisplayed()
    }
}
