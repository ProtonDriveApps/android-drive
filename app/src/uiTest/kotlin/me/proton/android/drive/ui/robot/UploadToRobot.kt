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

import me.proton.android.drive.ui.screen.UploadToScreenTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig.targetContext
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N

object UploadToRobot : LinksRobot, GrowlerRobot, Robot {
    private val uploadToScreen get() = node.withTag(UploadToScreenTestTag.screen)
    private val uploadButton get() = node.withText(I18N.string.upload_title).isEnabled()
    private val emptyText get() = node.withText(I18N.string.title_empty_my_files).isEnabled()
    private val createFolderButton get() = node.withContentDescription(I18N.string.folder_option_create_folder).isEnabled()

    fun clickUpload() = uploadButton.clickTo(this)
    fun clickCreateFolder() = createFolderButton.clickTo(CreateFolderRobot)

    fun assertEmptyFolder() = emptyText.await(60.seconds) { assertIsDisplayed() }

    fun assertFilesBeingUploaded(count: Int, folderName: String) = node.withText(
        targetContext.resources.getQuantityString(
            I18N.plurals.files_upload_being_uploaded_notification,
            count
        ).format(count, folderName)
    ).await { assertIsDisplayed() }

    override fun robotDisplayed() {
        uploadToScreen.await { assertIsDisplayed() }
    }
}
