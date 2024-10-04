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

import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.toPercentString
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig.targetContext
import java.util.Locale
import me.proton.core.drive.i18n.R as I18N

object FilesTabRobot : NavigationBarRobot, HomeRobot, LinksRobot, GrowlerRobot {
    private val addFilesButton get() = node.withText(I18N.string.action_empty_files_add_files)
    private val plusButton get() = node.withContentDescription(I18N.string.content_description_files_upload_upload_file)
    private val cancelUploadButton get() = node
        .withContentDescription(I18N.string.files_upload_content_description_cancel_upload)

    fun clickPlusButton() = plusButton.clickTo(ParentFolderOptionsRobot)
    fun clickAddFilesButton() = addFilesButton.clickTo(ParentFolderOptionsRobot)
    fun clickCancelUpload() = cancelUploadButton.clickTo(this)

    fun dismissFilesBeingUploaded(count: Int, folderName: String) = node.withText(
        targetContext.resources.getQuantityString(
            I18N.plurals.files_upload_being_uploaded_notification,
            count
        ).format(count, folderName)
    ).click()

    fun assertFilesBeingUploaded(count: Int, folderName: String) = node.withText(
        targetContext.resources.getQuantityString(
            I18N.plurals.files_upload_being_uploaded_notification,
            count
        ).format(count, folderName)
    ).await { assertIsDisplayed() }

    fun assertFilesSelected(count: Int) = node.withText(
        targetContext.resources.getQuantityString(
            I18N.plurals.common_selected,
            count
        ).format(count)
    ).await { assertIsDisplayed() }

    fun assertStageWaiting() =
        node.withText(I18N.string.files_upload_stage_waiting).await { assertIsDisplayed() }

    fun assertStageEncrypting() =
        node.withText(I18N.string.files_upload_stage_encrypting).await { assertIsDisplayed() }

    fun assertStageUploading() =
        node.withText(I18N.string.files_upload_stage_uploading).await { assertIsDisplayed() }

    fun assertStageUploadedProgress(progress: Int) = node.withText(
        targetContext.resources
            .getString(I18N.string.files_upload_stage_uploading_with_progress)
            .format(Percentage(progress).toPercentString(Locale.getDefault()))
    ).await { assertIsDisplayed() }

    fun assertUploadingFailed(fileName: String, reason: String) = node.withText(
        targetContext.resources.getString(
            I18N.string.files_upload_failure_with_description,
        ).format(fileName, reason)
    ).await { assertIsDisplayed() }

    override fun robotDisplayed() {
        homeScreenDisplayed()
        filesTab.assertIsSelected()
    }
}
