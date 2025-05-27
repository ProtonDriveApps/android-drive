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

import me.proton.android.drive.ui.dialog.FileFolderOptionsDialogTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N
import me.proton.android.drive.ui.dialog.MultipleFileFolderOptionsDialogTestTag
import me.proton.core.test.android.instrumented.utils.StringUtils


@Suppress("TooManyFunctions")
object FileFolderOptionsRobot : Robot {
    private val fileFolderOptionsScreen get() = node.withTag(FileFolderOptionsDialogTestTag.fileOrFolderOptions)
    private val moveButton get() = node.withText(I18N.string.files_move_file_action)
    private val moveToTrashButton get() = node.withText(I18N.string.files_send_to_trash_action)
    private val restoreFromTrashButton get() = node.withText(I18N.string.files_restore_from_trash_action)
    private val saveSharedPhotoButton get() = node.withText(I18N.string.files_save_shared_photo_action)
    private val makeAvailableOfflineButton get() = node.withText(I18N.string.common_make_available_offline_action)
    private val openInBrowserButton get() = node.withText(I18N.string.common_open_in_browser_action)
    private val removeAvailableOfflineButton get() = node
        .withText(I18N.string.common_remove_from_offline_available_action)
    private val manageLinkButton get() = node.withText(I18N.string.common_manage_link_action)
    private val setAsAlbumCoverButton get() = node.withText(I18N.string.common_set_as_album_cover_action)
    private val shareButton get() = node.withText(I18N.string.common_share)
    private val addToFavoriteButton get() = node.withText(I18N.string.files_add_to_favorite_action)
    private val removeFromFavoriteButton get() = node.withText(I18N.string.files_remove_from_favorite_action)
    private val manageAccessButton get() = node.withText(I18N.string.common_manage_access_action)
    private val removeMeButton get() = node.withText(I18N.string.files_remove_me_action)
    private val renameButton get() = node.withText(I18N.string.files_rename_file_action)
    private val fileDetailsButton get() = node.withText(I18N.string.files_display_file_info_action)
    private val folderDetailsButton get() = node.withText(I18N.string.files_display_folder_info_action)
    private val stopSharingButton get() = node.withText(I18N.string.common_stop_sharing_action)
    private val deletePermanentlyButton get() = node.withText(I18N.string.common_delete_permanently_action)
    private val deleteConfirm get() = node.withText(I18N.string.title_files_confirm_deletion)
    private val removeFromAlbum get() = node.withText(I18N.string.common_remove_from_album_action)
    private val addToAlbumsButton get() = node.withText(I18N.string.common_add_to_albums_action)

    fun clickMove() = MoveToFolderRobot.apply {
        moveButton.scrollTo().click()
    }
    fun clickRename() = RenameRobot.apply {
        renameButton.scrollTo().click()
    }
    fun clickManageLink() = ShareRobot.apply {
        manageLinkButton.scrollTo().click()
    }
    fun clickSetAsAlbumCover() = ShareRobot.apply {
        setAsAlbumCoverButton.scrollTo().click()
    }
    fun clickRemoveFromAlbum() = AlbumRobot.apply {
        removeFromAlbum.scrollTo().click()
    }
    fun <T : Robot> clickAddToFavorite(goesTo: T) =
        addToFavoriteButton.scrollTo().clickTo(goesTo)

    fun <T : Robot> clickRemoveFromFavorite(goesTo: T) =
        removeFromFavoriteButton.scrollTo().clickTo(goesTo)

    fun clickShare() = ShareUserRobot.apply {
        shareButton.scrollTo().click()
    }
    fun clickManageAccess() = ManageAccessRobot.apply {
        manageAccessButton.scrollTo().click()
    }
    fun <T: Robot >clickRemoveMe(goesTo: T) = removeMeButton.scrollTo().clickTo(goesTo)
    fun clickStopSharing() = ConfirmStopSharingRobot.apply {
        stopSharingButton.scrollTo().click()
    }
    fun clickFileDetails() = DetailsRobot.apply {
        fileDetailsButton.scrollTo().click()
    }
    fun clickFolderDetails() = DetailsRobot.apply {
        folderDetailsButton.scrollTo().click()
    }
    fun clickMoveToTrash() = FilesTabRobot.apply {
        moveToTrashButton.scrollTo().click()
    }
    fun clickRestoreTrash() = FilesTabRobot.apply {
        restoreFromTrashButton.scrollTo().click()
    }
    fun clickSaveSharedPhoto() = AlbumRobot.apply {
        saveSharedPhotoButton.scrollTo().click()
    }
    fun clickMakeAvailableOffline() = FilesTabRobot.apply {
        makeAvailableOfflineButton.scrollTo().click()
    }
    fun clickDeletePermanently() = apply {
        deletePermanentlyButton.scrollTo().click()
    }
    fun confirmDelete() = TrashRobot.apply {
        deleteConfirm.await { assertIsDisplayed() }
        deletePermanentlyButton.click()
    }
    fun <T : Robot> clickRemoveAvailableOffline(goesTo: T): T = goesTo.apply {
        removeAvailableOfflineButton.scrollTo().click()
    }

    fun <T : Robot> clickMakeAvailableOffline(goesTo: T) = goesTo.apply {
        makeAvailableOfflineButton.scrollTo().click()
    }

    fun clickOpenInBrowserButton() = openInBrowserButton.scrollTo().clickTo(FilesTabRobot)

    fun clickAddToAlbums() = AddToAlbumsOptionsRobot.apply {
        addToAlbumsButton.scrollTo().click()
    }

    fun numberOfItemsSelectedIsSeen(quantity: Int) {
        node.withTag(MultipleFileFolderOptionsDialogTestTag.fileOrFolderOptions).hasDescendant(
            node.withText(
                StringUtils.pluralStringFromResource(
                    I18N.plurals.common_selected,
                    quantity,
                    quantity
                )
            )).assertIsDisplayed()
    }

    override fun robotDisplayed() {
        fileFolderOptionsScreen.await { assertIsDisplayed() }
    }
}
