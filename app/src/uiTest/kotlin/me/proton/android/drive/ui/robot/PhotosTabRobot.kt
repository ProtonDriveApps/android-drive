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

import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.extension.doesNotExist
import me.proton.android.drive.ui.extension.withItemType
import me.proton.android.drive.ui.extension.withLayoutType
import me.proton.android.drive.ui.extension.withPluralTextResource
import me.proton.android.drive.ui.extension.withTextResource
import me.proton.android.drive.ui.robot.settings.PhotosBackupRobot
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.plan.test.robot.SubscriptionRobot
import me.proton.test.fusion.Fusion.allNodes
import me.proton.test.fusion.Fusion.node
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N

object PhotosTabRobot : HomeRobot, LinksRobot, NavigationBarRobot {
    private val enableBackupButton
        get() = allNodes.withText(I18N.string.photos_permissions_action).onFirst()

    private val backupCompleted get() = node.withText(I18N.string.photos_backup_state_completed)
    private val backupUploading get() = node.withText(I18N.string.photos_backup_state_uploading)
    private val backupFailed get() = node.withText(I18N.string.photos_error_backup_failed)
    private val noBackupsYet get() = node.withText(I18N.string.photos_empty_title)
    private val missingFolder get() = node.withText(I18N.string.photos_error_backup_missing_folder)
    private val noConnectivityBanner get() = node.withText(I18N.string.photos_error_waiting_wifi_connectivity)
    private val getMoreStorageButton get() = node.withText(I18N.string.storage_quotas_get_storage_action)
    private val storageFullTitle get() =
        node.withTextResource(I18N.string.storage_quotas_full_title, I18N.string.app_name)
    private fun storageFullTitle(percentage: Int) =
        node.withTextResource(I18N.string.storage_quotas_not_full_title, "${percentage}%")
    private val itCanTakeAWhileLabel get() = node.withText(I18N.string.photos_empty_loading_label_progress)
    private val closeQuotaBannerButton get() = node.withContentDescription(I18N.string.common_close_action)
    private val photosBackupDisabled get() = node.withText(I18N.string.error_creating_photo_share_not_allowed)
    private fun itemsLeft(items: Int) =
        node.withPluralTextResource(I18N.plurals.notification_content_text_backup_in_progress, items)

    private fun photoWithName(name: String) = linkWithName(name)
        .withItemType(ItemType.File)
        .withLayoutType(LayoutType.Grid)

    fun enableBackup() = enableBackupButton.clickTo(PhotosTabRobot)
    fun clickOnMore() = missingFolder.clickTo(PhotosBackupRobot)

    fun clickGetMoreStorage() = SubscriptionRobot.apply { getMoreStorageButton.click() }

    fun dismissQuotaBanner() = apply { closeQuotaBannerButton.click() }

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

    fun assertMissingFolderDisplayed() {
        missingFolder.await { assertIsDisplayed() }
    }

    fun assertPhotoCountEquals(count: Int) {
        allNodes
            .withItemType(ItemType.File)
            .withLayoutType(LayoutType.Grid)
            .assertCountEquals(count)
    }

    fun assertPhotoDisplayed(fileName: String) {
        photoWithName(fileName).await(90.seconds) { assertIsDisplayed() }
    }

    fun assertEnableBackupDisplayed() {
        enableBackupButton.await { assertIsDisplayed() }
    }

    fun assertNoConnectivityBannerDisplayed() {
        noConnectivityBanner.await {
            assertIsDisplayed()
        }
    }

    fun assertNoConnectivityBannerNotDisplayed() {
        noConnectivityBanner.await {
            doesNotExist()
        }
    }

    fun assertItCanTakeAwhileDisplayed() {
        itCanTakeAWhileLabel.await { assertIsDisplayed() }
        backupUploading.await { assertIsDisplayed() }
    }

    fun assertBackupCompleteDisplayed(
        waitFor: Duration = 90.seconds,
    ) {
        backupCompleted.await(timeout = waitFor) { assertIsDisplayed() }
    }

    fun assertStorageFull() {
        storageFullTitle.await { assertIsDisplayed() }
        getMoreStorageButton.await { assertIsDisplayed() }
        closeQuotaBannerButton.await { doesNotExist() }
    }

    fun assertBackupFailed() {
        backupFailed.await { assertIsDisplayed() }
    }

    fun assertStorageFull(percentage: Int) {
        storageFullTitle(percentage).await { assertIsDisplayed() }
        closeQuotaBannerButton.await { assertIsDisplayed() }
    }

    fun assertLeftToBackup(items: Int) {
        itemsLeft(items).await { assertIsDisplayed() }
    }

    fun assertPhotosBackupDisabled() {
        photosBackupDisabled.await { assertIsDisplayed() }
    }

    override fun robotDisplayed() {
        homeScreenDisplayed()
        photosTab.assertIsSelected()
    }
}
