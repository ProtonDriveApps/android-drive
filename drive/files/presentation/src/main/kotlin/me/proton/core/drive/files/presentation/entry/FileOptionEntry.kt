/*
 * Copyright (c) 2021-2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.files.presentation.entry

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.hasShareLink
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

sealed interface FileOptionEntry<in T : DriveLink> {

    val onClick: (T) -> Unit
    val notificationDotVisible: Boolean get() = false

    interface SimpleEntry<T : DriveLink> : FileOptionEntry<T> {

        @get:DrawableRes
        val icon: Int

        @get:DrawableRes
        val trailingIcon: Int? get() = null

        val trailingIconTintColor: Color? @Composable get() = null

        @Composable
        fun getLabel(): String
    }

    interface StateBasedEntry<T : DriveLink> : FileOptionEntry<T> {

        @get:DrawableRes
        val trailingIcon: Int? get() = null

        val trailingIconTintColor: Color? @Composable get() = null

        @Composable
        fun getLabel(driveLink: DriveLink): String

        @DrawableRes
        fun getIcon(driveLink: DriveLink): Int
    }
}

class ToggleOfflineEntry(
    override val onClick: (DriveLink) -> Unit,
) : FileOptionEntry.StateBasedEntry<DriveLink> {
    @Composable
    override fun getLabel(driveLink: DriveLink): String = stringResource(
        if (driveLink.isMarkedAsOffline) {
            I18N.string.common_remove_from_offline_available_action
        } else {
            I18N.string.common_make_available_offline_action
        }
    )

    override fun getIcon(driveLink: DriveLink): Int =
        if (driveLink.isMarkedAsOffline) {
            CorePresentation.drawable.ic_proton_arrow_down_circle_filled
        } else {
            CorePresentation.drawable.ic_proton_arrow_down_circle
        }
}

class ToggleTrashEntry(
    override val onClick: (DriveLink) -> Unit,
) : FileOptionEntry.StateBasedEntry<DriveLink> {
    @Composable
    override fun getLabel(driveLink: DriveLink): String = stringResource(
        if (driveLink.isTrashed) {
            I18N.string.files_restore_from_trash_action
        } else {
            I18N.string.files_send_to_trash_action
        }
    )

    override fun getIcon(driveLink: DriveLink): Int = if (driveLink.isTrashed) {
        CorePresentation.drawable.ic_proton_clock_rotate_left
    } else {
        CorePresentation.drawable.ic_proton_trash
    }
}

class DeletePermanentlyEntry(
    override val onClick: (DriveLink) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink> {

    override val icon: Int = CorePresentation.drawable.ic_proton_trash

    @Composable
    override fun getLabel(): String = stringResource(I18N.string.files_delete_from_trash_action)
}

class DeleteAlbumEntry(
    override val onClick: (DriveLink.Album) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink.Album> {

    override val icon: Int = CorePresentation.drawable.ic_proton_trash

    @Composable
    override fun getLabel(): String = stringResource(I18N.string.albums_delete_album_action)
}

class RemoveMeEntry(
    override val onClick: (DriveLink) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink> {

    override val icon: Int = CorePresentation.drawable.ic_proton_trash_cross

    @Composable
    override fun getLabel(): String = stringResource(I18N.string.files_remove_me_action)
}

class FileInfoEntry(
    override val onClick: (DriveLink) -> Unit,
) : FileOptionEntry.StateBasedEntry<DriveLink> {

    override fun getIcon(driveLink: DriveLink): Int = CorePresentation.drawable.ic_proton_info_circle

    @Composable
    override fun getLabel(driveLink: DriveLink): String = if (driveLink is Folder) {
        stringResource(id = I18N.string.files_display_folder_info_action)
    } else {
        stringResource(id = I18N.string.files_display_file_info_action)
    }
}

class MoveFileEntry(
    override val onClick: (DriveLink) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink> {

    override val icon: Int
        get() = CorePresentation.drawable.ic_proton_arrows_cross

    @Composable
    override fun getLabel(): String = stringResource(I18N.string.files_move_file_action)
}

class RenameFileEntry(
    override val onClick: (DriveLink) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink> {

    override val icon: Int = CorePresentation.drawable.ic_proton_pen_square

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.files_rename_file_action)
}

class SendFileEntry(
    override val onClick: (DriveLink.File) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink.File> {

    override val icon: Int = CorePresentation.drawable.ic_proton_paper_plane

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.files_send_file_action)
}

class StopSharingEntry(
    override val onClick: (DriveLink) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink> {
    override val icon: Int = CorePresentation.drawable.ic_proton_link_slash

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.common_stop_sharing_action)
}

class ShareViaLinkEntry(
    override val onClick: (DriveLink) -> Unit,
) : FileOptionEntry.StateBasedEntry<DriveLink> {
    @Composable
    override fun getLabel(driveLink: DriveLink): String = stringResource(
        if (driveLink.hasShareLink) {
            I18N.string.common_manage_link_action
        } else {
            I18N.string.common_get_link_action
        }
    )

    override fun getIcon(driveLink: DriveLink): Int = if (driveLink.hasShareLink) {
        CorePresentation.drawable.ic_proton_link_pen
    } else {
        CorePresentation.drawable.ic_proton_link
    }
}

class ManageAccessEntry(
    override val onClick: (DriveLink) -> Unit,
): FileOptionEntry.SimpleEntry<DriveLink> {
    override val icon: Int = CorePresentation.drawable.ic_proton_users

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.common_manage_access_action)
}

class ShareViaInvitationsEntry(
    override val onClick: (DriveLink) -> Unit,
): FileOptionEntry.SimpleEntry<DriveLink> {
    override val icon: Int = CorePresentation.drawable.ic_proton_user_plus

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.common_share)
}

class CopySharedLinkEntry(
    override val onClick: (DriveLink) -> Unit,
): FileOptionEntry.SimpleEntry<DriveLink> {
    override val icon: Int = CorePresentation.drawable.ic_proton_link

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.common_copy_link_action)
}

class DownloadFileEntry(
    override val onClick: (DriveLink) -> Unit,
): FileOptionEntry.SimpleEntry<DriveLink> {
    override val icon: Int = CorePresentation.drawable.ic_proton_arrow_down_line

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.common_download)
}

class OpenInBrowserProtonDocsEntry(
    override val onClick: (DriveLink.File) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink.File> {
    override val icon: Int = CorePresentation.drawable.ic_proton_globe

    override val trailingIcon: Int = CorePresentation.drawable.ic_proton_arrow_out_square

    override val trailingIconTintColor: Color @Composable get() = ProtonTheme.colors.iconWeak

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.common_open_in_browser_action)
}

class SetAsAlbumCoverEntry(
    override val onClick: (DriveLink.File) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink.File> {
    override val icon: Int = CorePresentation.drawable.ic_proton_window_image

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.common_set_as_album_cover_action)
}

class RemoveFromAlbumFileEntry(
    override val onClick: (DriveLink.File) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink.File> {
    override val icon: Int = CorePresentation.drawable.ic_proton_minus_circle

    @Composable
    override fun getLabel(): String = stringResource(id = I18N.string.common_remove_from_album_action)
}
