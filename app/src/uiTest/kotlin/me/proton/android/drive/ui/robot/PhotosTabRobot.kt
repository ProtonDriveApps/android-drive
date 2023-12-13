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

import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.extension.withItemType
import me.proton.android.drive.ui.extension.withLayoutType
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.test.fusion.Fusion.allNodes
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig
import kotlin.time.Duration
import me.proton.core.drive.i18n.R as I18N

object PhotosTabRobot : HomeRobot, LinksRobot, NavigationBarRobot {
    private val enableBackupButton
        get() = allNodes.withText(I18N.string.photos_permissions_action).onFirst()

    private val backupCompleted get() = node.withText(I18N.string.photos_backup_state_completed)
    private val noBackupsYet get() = node.withText(I18N.string.photos_empty_title)

    private fun photoWithName(name: String) = linkWithName(name)
        .withItemType(ItemType.File)
        .withLayoutType(LayoutType.Grid)

    fun enableBackup() = enableBackupButton.clickTo(PhotosTabRobot)

    fun longClickOnPhoto(fileName: String) =
        photoWithName(fileName).longClickTo(this)

    fun clickOnPhoto(imageName: ImageName) = clickOnPhoto(imageName.fileName)

    fun clickOnPhoto(name: String) =
        photoWithName(name).clickTo(PreviewRobot)

    fun deselectPhoto(imageName: ImageName) =
        photoWithName(imageName.fileName).clickTo(PhotosTabRobot)

    fun assertPhotoDisplayed(imageName: ImageName) {
        photoWithName(imageName.fileName).await { assertIsDisplayed() }
    }

    fun assertNoBackupsDisplayed() {
        noBackupsYet.await { assertIsDisplayed() }
    }

    fun assertPhotoCountEquals(count: Int) {
        allNodes
            .withItemType(ItemType.File)
            .withLayoutType(LayoutType.Grid)
            .assertCountEquals(count)
    }

    fun assertPhotoDisplayed(fileName: String) {
        photoWithName(fileName).await { assertIsDisplayed() }
    }

    fun assertBackupCompleteDisplayed(
        waitFor: Duration = FusionConfig.Compose.waitTimeout.get()
    ) {
        backupCompleted.await(timeout = waitFor) { assertIsDisplayed() }
    }

    override fun robotDisplayed() {
        homeScreenDisplayed()
        photosTab.assertIsSelected()
    }
}
