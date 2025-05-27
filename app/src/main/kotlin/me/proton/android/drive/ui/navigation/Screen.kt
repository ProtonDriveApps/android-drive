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

package me.proton.android.drive.ui.navigation

import android.net.Uri
import android.os.Parcelable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.proton.android.drive.ui.viewmodel.AddToAlbumsOptionsViewModel
import me.proton.android.drive.ui.viewmodel.AlbumOptionsViewModel
import me.proton.android.drive.ui.viewmodel.AlbumViewModel
import me.proton.android.drive.ui.viewmodel.ComputerOptionsViewModel
import me.proton.android.drive.ui.viewmodel.ConfirmDeleteAlbumDialogViewModel
import me.proton.android.drive.ui.viewmodel.ConfirmLeaveAlbumDialogViewModel
import me.proton.android.drive.ui.viewmodel.ConfirmStopAllSharingDialogViewModel
import me.proton.android.drive.ui.viewmodel.CreateNewAlbumViewModel
import me.proton.android.drive.ui.viewmodel.FileOrFolderOptionsViewModel
import me.proton.android.drive.ui.viewmodel.MoveToFolderViewModel
import me.proton.android.drive.ui.viewmodel.MultipleFileOrFolderOptionsViewModel
import me.proton.android.drive.ui.viewmodel.ParentFolderOptionsViewModel
import me.proton.android.drive.ui.viewmodel.PhotosPickerAndSelectionViewModel
import me.proton.android.drive.ui.viewmodel.PickerPhotosViewModel
import me.proton.android.drive.ui.viewmodel.ShareInvitationOptionsViewModel
import me.proton.android.drive.ui.viewmodel.ShareMemberOptionsViewModel
import me.proton.android.drive.ui.viewmodel.SubscriptionPromoViewModel
import me.proton.android.drive.ui.viewmodel.ShareMultiplePhotosOptionsViewModel
import me.proton.android.drive.ui.viewmodel.UploadToViewModel
import me.proton.android.drive.ui.viewmodel.UserInvitationViewModel
import me.proton.android.drive.ui.viewmodel.WhatsNewViewModel
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.drivelink.device.presentation.viewmodel.RenameDeviceViewModel
import me.proton.core.drive.drivelink.rename.presentation.viewmodel.RenameViewModel
import me.proton.core.drive.drivelink.shared.presentation.viewmodel.LinkSettingsViewModel
import me.proton.core.drive.drivelink.shared.presentation.viewmodel.SharedDriveLinkViewModel
import me.proton.core.drive.folder.create.presentation.CreateFolderViewModel
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.notification.presentation.viewmodel.NotificationPermissionRationaleViewModel
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.drive.android.settings.domain.entity.WhatsNewKey

sealed class Screen(val route: String) {
    open fun deepLink(baseUrl: String): String? = "$baseUrl/$route"

    data object Launcher : Screen("launcher?redirection={redirection}") {
        const val REDIRECTION = "redirection"
    }

    data object SigningOut : Screen("signingOut/{userId}") {
        operator fun invoke(userId: UserId) = "signingOut/${userId.id}"

        const val USER_ID = Screen.USER_ID
    }

    data object Home : Screen("home/{userId}?tab={tab}") {
        operator fun invoke(userId: UserId, tab: String? = null) = buildString {
            append("home/${userId.id}")
            if (tab != null) {
                append("?tab=$tab")
            }
        }

        const val USER_ID = Screen.USER_ID
        const val TAB = "tab"
        const val TAB_FILES = "files"
        const val TAB_PHOTOS = "photos"
        const val TAB_COMPUTERS = "computers"
        const val TAB_SHARED_TABS = "shared_tabs"
    }

    data object Sorting :
        Screen("sorting/{userId}/files?by={by}&direction={direction}&shareId={shareId}&folderId={folderId}") {
        operator fun invoke(
            userId: UserId,
            folderId: FolderId?,
            by: By,
            direction: Direction,
        ) =
            buildString {
                append("sorting/${userId.id}/files")
                append("?by=$by")
                append("&direction=$direction")
                if (folderId != null) {
                    append("&shareId=${folderId.shareId.id}")
                    append("&folderId=${folderId.id}")
                }
            }

        const val BY = "by"
        const val DIRECTION = "direction"
    }

    data object FileOrFolderOptions : Screen(
        "options/link/{userId}/shares/{shareId}/linkId={linkId}?albumShareId={albumShareId}&albumId={albumId}&selectionId={selectionId}"
    ) {
        operator fun invoke(
            userId: UserId,
            linkId: LinkId,
            albumId: AlbumId? = null,
            selectionId: SelectionId? = null,
        ) = buildString {
            append("options/link/${userId.id}/shares/${linkId.shareId.id}/linkId=${linkId.id}")
            if (albumId != null) {
                append("?albumShareId=${albumId.shareId.id}&albumId=${albumId.id}")
            }
            if (selectionId != null) {
                if (albumId != null) {
                    append("&")
                } else {
                    append("?")
                }
                append("selectionId=${selectionId.id}")
            }
        }

        const val SHARE_ID = FileOrFolderOptionsViewModel.KEY_SHARE_ID
        const val LINK_ID = FileOrFolderOptionsViewModel.KEY_LINK_ID
        const val ALBUM_ID = FileOrFolderOptionsViewModel.KEY_ALBUM_ID
        const val ALBUM_SHARE_ID = FileOrFolderOptionsViewModel.KEY_ALBUM_SHARE_ID
        const val SELECTION_ID = FileOrFolderOptionsViewModel.KEY_SELECTION_ID
    }

    data object MultipleFileOrFolderOptions : Screen(
        "options/multiple/{userId}/selectionId={selectionId}?albumShareId={albumShareId}&albumId={albumId}"
    ) {
        operator fun invoke(
            userId: UserId,
            selectionId: SelectionId,
            albumId: AlbumId? = null,
        ) = buildString {
            append("options/multiple/${userId.id}/selectionId=${selectionId.id}")
            if (albumId != null) {
                append("?albumShareId=${albumId.shareId.id}&albumId=${albumId.id}")
            }
        }

        const val SELECTION_ID = MultipleFileOrFolderOptionsViewModel.KEY_SELECTION_ID
        const val ALBUM_SHARE_ID = MultipleFileOrFolderOptionsViewModel.KEY_ALBUM_SHARE_ID
        const val ALBUM_ID = MultipleFileOrFolderOptionsViewModel.KEY_ALBUM_ID
    }

    data object ParentFolderOptions : Screen(
        "options/parentFolder/{userId}/shares/{shareId}/folderId={folderId}"
    ) {
        operator fun invoke(
            userId: UserId,
            folderId: FolderId,
        ) = "options/parentFolder/${userId.id}/shares/${folderId.shareId.id}/folderId=${folderId.id}"

        const val FOLDER_ID = ParentFolderOptionsViewModel.KEY_FOLDER_ID
        const val SHARE_ID = ParentFolderOptionsViewModel.KEY_SHARE_ID
    }

    data object ComputerOptions : Screen(
        "options/computer/{userId}/devices/{deviceId}"
    ) {

        operator fun invoke(
            userId: UserId,
            deviceId: DeviceId,
        ) = "options/computer/${userId.id}/devices/${deviceId.id}"

        const val DEVICE_ID = ComputerOptionsViewModel.KEY_DEVICE_ID
    }

    data object ProtonDocsInsertImageOptions : Screen(
        "options/protonDocsInsertImage/{userId}/"
    ) {
        operator fun invoke(
            userId: UserId,
        ) = "options/protonDocsInsertImage/${userId.id}/"
    }

    data object AlbumOptions : Screen(
        "options/album/{userId}/shares/{shareId}/albumId={albumId}"
    ) {
        operator fun invoke(
            userId: UserId,
            albumId: AlbumId,
        ) = "options/album/${userId.id}/shares/${albumId.shareId.id}/albumId=${albumId.id}"

        const val SHARE_ID = AlbumOptionsViewModel.KEY_SHARE_ID
        const val ALBUM_ID = AlbumOptionsViewModel.KEY_ALBUM_ID
    }

    data object ShareMultiplePhotosOptions : Screen(
        "options/multiple/sharePhotos/{userId}/selectionId={selectionId}"
    ) {
        operator fun invoke(
            userId: UserId,
            selectionId: SelectionId,
        ) = "options/multiple/sharePhotos/${userId.id}/selectionId=${selectionId.id}"

        const val USER_ID = Screen.USER_ID
        const val SELECTION_ID = ShareMultiplePhotosOptionsViewModel.SELECTION_ID
    }

    data object AddToAlbumsOptions : Screen(
        "options/multiple/addToAlbums/{userId}/selectionId={selectionId}"
    ) {
        operator fun invoke(
            userId: UserId,
            selectionId: SelectionId,
        ) = "options/multiple/addToAlbums/${userId.id}/selectionId=${selectionId.id}"

        const val USER_ID = Screen.USER_ID
        const val SELECTION_ID = AddToAlbumsOptionsViewModel.SELECTION_ID
    }

    data object Info : Screen("info/{userId}/shares/{shareId}/files?linkId={linkId}") {
        operator fun invoke(
            userId: UserId,
            linkId: LinkId,
        ) = "info/${userId.id}/shares/${linkId.shareId.id}/files?linkId=${linkId.id}"

        const val USER_ID = Screen.USER_ID
        const val LINK_ID = "linkId"
        const val SHARE_ID = "shareId"
    }

    data object Files : Screen(filesBrowsableRoute("home")), HomeTab {

        override fun invoke(userId: UserId) = invoke(userId, null)

        operator fun invoke(userId: UserId, folderId: FolderId?, folderName: String? = null) =
            filesBrowsableBuildRoute("home", userId, folderId, folderName)

        object Dialogs {

            data object ConfirmDeletion : Screen("delete/{userId}/shares/{shareId}/files/{fileId}/confirm") {
                operator fun invoke(userId: UserId, linkId: LinkId) =
                    "delete/${userId.id}/shares/${linkId.shareId.id}/files/${linkId.id}/confirm"

                const val FILE_ID = "fileId"
                const val SHARE_ID = "shareId"
            }

            data object ConfirmEmptyTrash : Screen("delete/{userId}/trash?volumeId={volumeId}") {
                operator fun invoke(
                    userId: UserId,
                    volumeId: VolumeId,
                ) = "delete/${userId.id}/trash?volumeId=${volumeId.id}"
            }

            data object Rename : Screen("rename/{userId}/shares/{shareId}/files?fileId={fileId}&folderId={folderId}&albumId={albumId}") {
                operator fun invoke(
                    userId: UserId,
                    linkId: LinkId,
                ) = when (linkId) {
                    is FileId -> "rename/${userId.id}/shares/${linkId.shareId.id}/files?fileId=${linkId.id}"
                    is FolderId -> "rename/${userId.id}/shares/${linkId.shareId.id}/files?folderId=${linkId.id}"
                    is AlbumId -> "rename/${userId.id}/shares/${linkId.shareId.id}/files?albumId=${linkId.id}"
                }

                const val FILE_ID = RenameViewModel.KEY_FILE_ID
                const val FOLDER_ID = RenameViewModel.KEY_FOLDER_ID
                const val SHARE_ID = RenameViewModel.KEY_SHARE_ID
            }

            data object CreateFolder :
                Screen("folder/{userId}/shares/{shareId}/files/{parentId}") {
                operator fun invoke(
                    userId: UserId,
                    parentId: FolderId,
                ) = "folder/${userId.id}/shares/${parentId.shareId.id}/files/${parentId.id}"

                const val SHARE_ID = CreateFolderViewModel.KEY_SHARE_ID
                const val PARENT_ID = CreateFolderViewModel.KEY_PARENT_ID
            }

            data object ConfirmStopLinkSharing : Screen("share_url/{userId}/shares/{shareId}/links/{linkId}/confirm_delete?confirmPopUpRoute={confirmPopUpRoute}&confirmPopUpRouteInclusive={confirmPopUpRouteInclusive}") {
                operator fun invoke(userId: UserId, linkId: LinkId) =
                    "share_url/${userId.id}/shares/${linkId.shareId.id}/links/${linkId.id}/confirm_delete?confirmPopUpRouteInclusive=true"

                operator fun invoke(
                    userId: UserId,
                    linkId: LinkId,
                    confirmPopUpRoute: String,
                    confirmPopUpRouteInclusive: Boolean = true,
                ) =
                    "share_url/${userId.id}/shares/${linkId.shareId.id}/links/${linkId.id}/confirm_delete?confirmPopUpRoute=${confirmPopUpRoute}&confirmPopUpRouteInclusive=${confirmPopUpRouteInclusive}"

                const val LINK_ID = "linkId"
                const val SHARE_ID = ConfirmStopAllSharingDialogViewModel.SHARE_ID
                const val CONFIRM_POP_UP_ROUTE = "confirmPopUpRoute"
                const val CONFIRM_POP_UP_ROUTE_INCLUSIVE = "confirmPopUpRouteInclusive"
            }

            data object ConfirmStopAllSharing : Screen("sharing/{userId}/shares/{shareId}/confirm_delete_all?confirmPopUpRoute={confirmPopUpRoute}&confirmPopUpRouteInclusive={confirmPopUpRouteInclusive}") {
                operator fun invoke(shareId: ShareId) =
                    "sharing/${shareId.userId.id}/shares/${shareId.id}/confirm_delete_all?confirmPopUpRouteInclusive=true"

                operator fun invoke(
                    shareId: ShareId,
                    confirmPopUpRoute: String,
                    confirmPopUpRouteInclusive: Boolean = true,
                ) =
                    "sharing/${shareId.userId.id}/shares/${shareId.id}/confirm_delete_all?confirmPopUpRoute=${confirmPopUpRoute}&confirmPopUpRouteInclusive=$confirmPopUpRouteInclusive"


                const val SHARE_ID = "shareId"
                const val CONFIRM_POP_UP_ROUTE = "confirmPopUpRoute"
                const val CONFIRM_POP_UP_ROUTE_INCLUSIVE = "confirmPopUpRouteInclusive"
            }
        }

        const val USER_ID = Screen.USER_ID
        const val FOLDER_ID = "folderId"
        const val VOLUME_ID = "volumeId"
        const val SHARE_ID = "shareId"
        const val FOLDER_NAME = "folderName"
    }

    data object Move :
        Screen("move/{userId}/files?shareId={shareId}&linkId={linkId}&selectionId={selectionId}&parentShareId={parentShareId}&parentId={parentId}") {
        operator fun invoke(
            userId: UserId,
            selectionId: SelectionId,
            parentId: FolderId?,
        ) =
            "move/${userId.id}/files?selectionId=${selectionId.id}${
                parentId?.let {
                    "&parentShareId=${parentId.shareId.id}&parentId=${parentId.id}"
                } ?: ""
            }"

        operator fun invoke(
            userId: UserId,
            linkId: LinkId,
            parentId: FolderId?,
        ) =
            "move/${userId.id}/files?shareId=${linkId.shareId.id}&linkId=${linkId.id}${
                parentId?.let {
                    "&parentShareId=${parentId.shareId.id}&parentId=${parentId.id}"
                } ?: ""
            }"


        const val LINK_ID = "linkId"
        const val SHARE_ID = "shareId"
        const val SELECTION_ID = MoveToFolderViewModel.SELECTION_ID
        const val PARENT_ID = "parentId"
        const val PARENT_SHARE_ID = "parentShareId"
    }

    data object SharedTabs : Screen(filesBrowsableRoute("shared")), HomeTab {
        override fun invoke(userId: UserId) =
            filesBrowsableBuildRoute("shared", userId, null, null)

        operator fun invoke(userId: UserId, folderId: FolderId?, folderName: String?) =
            filesBrowsableBuildRoute("shared", userId, folderId, folderName)

        const val USER_ID = Screen.USER_ID
    }
    data object Photos : Screen("home/{userId}/photos/{shareId}"), HomeTab {

        override fun invoke(userId: UserId) = invoke(userId, null)

        operator fun invoke(userId: UserId, shareId: ShareId?) = "home/${userId.id}/photos/${shareId?.id}"

        data object Upsell : Screen("home/{userId}/photos/upsell") {
            operator fun invoke(userId: UserId) = "home/${userId.id}/photos/upsell"
        }

        data object ImportantUpdates : Screen("home/{userId}/photos/importantUpdates") {
            operator fun invoke(userId: UserId) = "home/${userId.id}/photos/importantUpdates"
        }

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = "shareId"
    }
    data object PhotosAndAlbums : Screen("home/{userId}/photos"), HomeTab {

        override fun invoke(userId: UserId) = "home/${userId.id}/photos"

        data object CreateNewAlbum : Screen("home/{userId}/photos/new_album?sharedAlbum={sharedAlbum}") {
            operator fun invoke(
                userId: UserId,
                sharedAlbum: Boolean = false,
            ) = "home/${userId.id}/photos/new_album?sharedAlbum=${sharedAlbum}"
        }

        const val USER_ID = Screen.USER_ID
        const val SHARED_ALBUM = CreateNewAlbumViewModel.SHARED_ALBUM
    }

    data object Album : Screen("photos/{userId}/shares/{shareId}/albums/{albumId}") {

        operator fun invoke(albumId: AlbumId) = "photos/${albumId.userId.id}/shares/${albumId.shareId.id}/albums/${albumId.id}"

        object Dialogs {
            data object ConfirmDeleteAlbum : Screen("delete/photos/{userId}/shares/{shareId}/albums/{albumId}/confirm") {
                operator fun invoke(albumId: AlbumId) =
                    "delete/photos/${albumId.userId.id}/shares/${albumId.shareId.id}/albums/${albumId.id}/confirm"

                const val SHARE_ID = ConfirmDeleteAlbumDialogViewModel.SHARE_ID
                const val ALBUM_ID = ConfirmDeleteAlbumDialogViewModel.ALBUM_ID
            }

            data object ConfirmLeaveAlbum : Screen("leave/photos/{userId}/shares/{shareId}/albums/{albumId}/confirm") {
                operator fun invoke(albumId: AlbumId) =
                    "leave/photos/${albumId.userId.id}/shares/${albumId.shareId.id}/albums/${albumId.id}/confirm"

                const val SHARE_ID = ConfirmLeaveAlbumDialogViewModel.SHARE_ID
                const val ALBUM_ID = ConfirmLeaveAlbumDialogViewModel.ALBUM_ID
            }
        }

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = AlbumViewModel.SHARE_ID
        const val ALBUM_ID = AlbumViewModel.ALBUM_ID
    }

    object Picker {
        data object Photos : Screen("picker/{userId}/photos/destination?inPickerMode={inPickerMode}&destinationShareId={destinationShareId}&destinationAlbumId={destinationAlbumId}") {
            operator fun invoke(userId: UserId) = "picker/${userId.id}/photos/destination?inPickerMode=true"

            operator fun invoke(destinationAlbumId: AlbumId) =
                "picker/${destinationAlbumId.userId.id}/photos/destination?inPickerMode=true&destinationShareId=${destinationAlbumId.shareId.id}&destinationAlbumId=${destinationAlbumId.id}"
        }

        data object Album : Screen("picker/{userId}/shares/{shareId}/albums/{albumId}/destination?inPickerMode={inPickerMode}&destinationShareId={destinationShareId}&destinationAlbumId={destinationAlbumId}") {
            operator fun invoke(albumId: AlbumId) = "picker/${albumId.userId.id}/shares/${albumId.shareId.id}/albums/${albumId.id}/destination?inPickerMode=true"

            operator fun invoke(albumId: AlbumId, destinationAlbumId: AlbumId) =
                "picker/${albumId.userId.id}/shares/${albumId.shareId.id}/albums/${albumId.id}/destination?inPickerMode=true&destinationShareId=${destinationAlbumId.shareId.id}&destinationAlbumId=${destinationAlbumId.id}"
        }

        const val USER_ID = Screen.USER_ID
        const val IN_PICKER_MODE = PhotosPickerAndSelectionViewModel.IN_PICKER_MODE
        const val SHARE_ID = AlbumViewModel.SHARE_ID
        const val ALBUM_ID = AlbumViewModel.ALBUM_ID
        const val DESTINATION_SHARE_ID = PickerPhotosViewModel.DESTINATION_SHARE_ID
        const val DESTINATION_ALBUM_ID = PickerPhotosViewModel.DESTINATION_ALBUM_ID
    }

    data object BackupIssues : Screen("backup/issues/{userId}/shares/{shareId}/folder/{folderId}") {

        fun invoke(folderId: FolderId) = "backup/issues/${folderId.shareId.userId.id}/shares/${folderId.shareId.id}/folder/${folderId.id}"

        object Dialogs {
            data object ConfirmSkipIssues : Screen("backup/issues/{userId}/shares/{shareId}/folder/{folderId}/confirm_skip?confirmPopUpRoute={confirmPopUpRoute}&confirmPopUpRouteInclusive={confirmPopUpRouteInclusive}"){
                operator fun invoke(
                    folderId: FolderId,
                    confirmPopUpRoute: String,
                    confirmPopUpRouteInclusive: Boolean = true,
                ) = "backup/issues/${folderId.shareId.userId.id}/shares/${folderId.shareId.id}/folder/${folderId.id}/confirm_skip?confirmPopUpRoute=${confirmPopUpRoute}&confirmPopUpRouteInclusive=${confirmPopUpRouteInclusive}"

                const val CONFIRM_POP_UP_ROUTE = "confirmPopUpRoute"
                const val CONFIRM_POP_UP_ROUTE_INCLUSIVE = "confirmPopUpRouteInclusive"
            }
        }

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = "shareId"
        const val FOLDER_ID = "folderId"
    }

    data object PhotosPermissionRationale : Screen(
        "home/{userId}/photosPermissionRationale"
    ) {
        operator fun invoke(
            userId: UserId,
        ) = "home/${userId.id}/photosPermissionRationale"

        const val USER_ID = Screen.USER_ID
    }

    data object Computers : Screen(filesBrowsableRoute("computers") + "&syncedFolders={syncedFolders}"), HomeTab {
        override fun invoke(userId: UserId) =
            filesBrowsableBuildRoute("computers", userId, null, null)

        operator fun invoke(userId: UserId, folderId: FolderId?, folderName: String?, syncedFolders: Boolean) =
            filesBrowsableBuildRoute("computers", userId, folderId, folderName) +
                    folderId?.let{"&syncedFolders=${syncedFolders}"}.orEmpty()

        const val USER_ID = Screen.USER_ID
        const val SYNCED_FOLDERS = "syncedFolders"
    }

    data object Trash : Screen("trash/{userId}") {
        operator fun invoke(userId: UserId) = "trash/${userId.id}"

        const val USER_ID = Screen.USER_ID
    }

    data object OfflineFiles : Screen(filesBrowsableRoute("offline")) {
        operator fun invoke(userId: UserId, folderId: FolderId? = null, folderName: String? = null) =
            filesBrowsableBuildRoute("offline", userId, folderId, folderName)
    }

    data object PagerPreview : Screen("pager/{pagerType}/preview/{userId}/shares/{shareId}/files/{fileId}?photoTag={photoTag}&albumShareId={albumShareId}&albumId={albumId}") {
        operator fun invoke(
            pagerType: PagerType,
            userId: UserId,
            fileId: FileId,
        ) = "pager/${pagerType.type}/preview/${userId.id}/shares/${fileId.shareId.id}/files/${fileId.id}"
        operator fun invoke(
            pagerType: PagerType,
            userId: UserId,
            fileId: FileId,
            photoTag: PhotoTag,
        ) = "pager/${pagerType.type}/preview/${userId.id}/shares/${fileId.shareId.id}/files/${fileId.id}?photoTag=${photoTag.value}"
        operator fun invoke(
            pagerType: PagerType,
            userId: UserId,
            fileId: FileId,
            albumId: AlbumId,
        ) = "pager/${pagerType.type}/preview/${userId.id}/shares/${fileId.shareId.id}/files/${fileId.id}?albumShareId=${albumId.shareId.id}&albumId=${albumId.id}"

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = "shareId"
        const val FILE_ID = "fileId"
        const val ALBUM_SHARE_ID = "albumShareId"
        const val ALBUM_ID = "albumId"
        const val PHOTO_TAG = "photoTag"
        const val PAGER_TYPE = "pagerType"
    }

    data object Settings : Screen("settings/{userId}") {
        operator fun invoke(userId: UserId) = "settings/${userId.id}"

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = "shareId"
        const val FOLDER_ID = "folderId"

        data object AccountSettings : Screen("settings/{userId}/account") {
            operator fun invoke(userId: UserId) = "settings/${userId.id}/account"
        }

        data object AppAccess : Screen("settings/{userId}/appAccess") {

            operator fun invoke(userId: UserId) = "settings/${userId.id}/appAccess"

            object Dialogs {

                data object SystemAccess : Screen("settings/{userId}/appAccess/systemAccess") {
                    operator fun invoke(userId: UserId) = "settings/${userId.id}/appAccess/systemAccess"
                }
            }
        }

        data object AutoLockDurations : Screen("settings/{userId}/autoLockDurations") {
            operator fun invoke(userId: UserId) = "settings/${userId.id}/autoLockDurations"
        }

        data object PhotosBackup : Screen("settings/{userId}/photosBackup") {
            operator fun invoke(userId: UserId) = "settings/${userId.id}/photosBackup"

            object Dialogs {

                object ConfirmStopSyncFolder : Screen("settings/{userId}/backup/{shareId}/folder/{folderId}/confirm_skip?id={id}"){
                    operator fun invoke(
                        folderId: FolderId,
                        id: Int,
                    ) = "settings/${folderId.shareId.userId.id}/backup/${folderId.shareId.id}/folder/${folderId.id}/confirm_skip?id=${id}"
                }
            }
        }

        data object DefaultHomeTab : Screen("settings/{userId}/defaultHomeTab") {
            operator fun invoke(userId: UserId) = "settings/${userId.id}/defaultHomeTab"
        }
    }

    data object SendFile : Screen("send/{userId}/shares/{shareId}/files/{fileId}") {
        operator fun invoke(userId: UserId, fileId: FileId) =
            "send/${userId.id}/shares/${fileId.shareId.id}/files/${fileId.id}"

        const val SHARE_ID = "shareId"
        const val FILE_ID = "fileId"
    }

    data object ShareViaLink : Screen("shareViaLink/{userId}/shares/{shareId}/linkId/{linkId}") {
        operator fun invoke(
            userId: UserId,
            linkId: LinkId,
        ) = "shareViaLink/${userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}"

        object Dialogs {
            object DiscardChanges : Screen("shareViaLink/{userId}/shares/{shareId}/linkId/{linkId}/discard_changes"){
                operator fun invoke(
                    userId: UserId,
                    linkId: LinkId,
                ) = "shareViaLink/${userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}/discard_changes"
            }
        }

        const val USER_ID = Screen.USER_ID
        const val LINK_ID = SharedDriveLinkViewModel.LINK_ID
        const val SHARE_ID = SharedDriveLinkViewModel.SHARE_ID
    }

    data object ManageAccess : Screen("manageAccess/{userId}/shares/{shareId}/linkId/{linkId}") {
        operator fun invoke(
            userId: UserId,
            linkId: LinkId,
        ) = "manageAccess/${userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}"

        const val USER_ID = Screen.USER_ID
        const val LINK_ID = SharedDriveLinkViewModel.LINK_ID
        const val SHARE_ID = SharedDriveLinkViewModel.SHARE_ID
    }

    data object LinkSettings : Screen("manageAccess/{userId}/shares/{shareId}/linkId/{linkId}/linkSettings") {
        operator fun invoke(
            userId: UserId,
            linkId: LinkId,
        ) = "manageAccess/${userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}/linkSettings"

        object Dialogs {
            data object DiscardChanges : Screen("manageAccess/{userId}/shares/{shareId}/linkId/{linkId}/linkSettings/discard_changes"){
                operator fun invoke(
                    linkId: LinkId,
                ) = "manageAccess/${linkId.userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}/linkSettings/discard_changes"
            }
        }

        const val USER_ID = Screen.USER_ID
        const val LINK_ID = LinkSettingsViewModel.LINK_ID
        const val SHARE_ID = LinkSettingsViewModel.SHARE_ID
    }

    data object ShareViaInvitations : Screen("shareViaInvitations/{userId}/shares/{shareId}/linkId/{linkId}") {
        operator fun invoke(
            userId: UserId,
            linkId: LinkId,
        ) = "shareViaInvitations/${userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}"

        object Dialogs {
            data object DiscardChanges : Screen("shareViaInvitations/{userId}/shares/{shareId}/linkId/{linkId}/discard_changes"){
                operator fun invoke(
                    linkId: LinkId,
                ) = "shareViaInvitations/${linkId.userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}/discard_changes"
            }
        }

        data object InternalOptions :
            Screen("shareViaInvitations/{userId}/shares/{shareId}/linkId/{linkId}/invitation/{invitationId}/options") {
            operator fun invoke(
                linkId: LinkId,
                invitationId: String,
            ) =
                "shareViaInvitations/${linkId.userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}/invitation/${invitationId}/options"

            const val USER_ID = Screen.USER_ID
            const val SHARE_ID = ShareInvitationOptionsViewModel.KEY_SHARE_ID
            const val LINK_ID = ShareInvitationOptionsViewModel.KEY_LINK_ID
            const val INVITATION_ID = ShareInvitationOptionsViewModel.KEY_INVITATION_ID
        }

        data object ExternalOptions :
            Screen("shareViaInvitations/{userId}/shares/{shareId}/linkId/{linkId}/external-invitation/{invitationId}/options") {
            operator fun invoke(
                linkId: LinkId,
                invitationId: String,
            ) =
                "shareViaInvitations/${linkId.userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}/external-invitation/${invitationId}/options"

            const val USER_ID = Screen.USER_ID
            const val SHARE_ID = ShareInvitationOptionsViewModel.KEY_SHARE_ID
            const val LINK_ID = ShareInvitationOptionsViewModel.KEY_LINK_ID
            const val INVITATION_ID = ShareInvitationOptionsViewModel.KEY_INVITATION_ID
        }

        const val USER_ID = Screen.USER_ID
        const val LINK_ID = SharedDriveLinkViewModel.LINK_ID
        const val SHARE_ID = SharedDriveLinkViewModel.SHARE_ID
    }

    data object ShareMemberOptions : Screen("shareViaInvitations/{userId}/shares/{shareId}/linkId/{linkId}/member/{memberId}/options") {
        operator fun invoke(
            linkId: LinkId,
            memberId: String,
        ) = "shareViaInvitations/${linkId.userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}/member/${memberId}/options"

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = ShareMemberOptionsViewModel.KEY_SHARE_ID
        const val LINK_ID = ShareMemberOptionsViewModel.KEY_LINK_ID
        const val MEMBER_ID = ShareMemberOptionsViewModel.KEY_MEMBER_ID
    }

    data object UserInvitation : Screen("shareViaInvitations/{userId}/invitations?albumsOnly={albumsOnly}") {
        operator fun invoke(
            userId: UserId,
            albumsOnly: Boolean = false,
        ) = "shareViaInvitations/${userId.id}/invitations?albumsOnly=$albumsOnly"

        const val USER_ID = Screen.USER_ID
        const val ALBUMS_ONLY = UserInvitationViewModel.ALBUMS_ONLY
    }

    data object ShareLinkPermissions : Screen("shareViaLink/{userId}/shares/{shareId}/linkId/{linkId}/permissions") {
        operator fun invoke(
            linkId: LinkId,
        ) = "shareViaLink/${linkId.userId.id}/shares/${linkId.shareId.id}/linkId/${linkId.id}/permissions"

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = ShareMemberOptionsViewModel.KEY_SHARE_ID
        const val LINK_ID = ShareMemberOptionsViewModel.KEY_LINK_ID
    }

    data object Upload : Screen("upload/{userId}/files?uris={uris}&parentShareId={parentShareId}&parentId={parentId}") {
        operator fun invoke(
            userId: UserId,
            uris: List<UriWithFileName>,
            parentId: FolderId? = null,
        ) =
            "upload/${userId.id}/files?uris=${Uri.encode(Json.encodeToString(UploadParameters(uris)))}${
                parentId?.let {
                    "&parentShareId=${parentId.shareId.id}&parentId=${parentId.id}"
                } ?: ""
            }"

        const val USER_ID = Screen.USER_ID
        const val URIS = UploadToViewModel.URIS
        const val PARENT_ID = UploadToViewModel.PARENT_ID
        const val PARENT_SHARE_ID = UploadToViewModel.PARENT_SHARE_ID
    }

    object Dialogs {
        data object SignOut : Screen("home/{userId}/signout") {
            operator fun invoke(userId: UserId) = "home/${userId.id}/signout"

            const val USER_ID = Files.USER_ID
        }

        data object StorageFull : Screen("storage/{userId}/full") {
            operator fun invoke(userId: UserId) = "storage/${userId.id}/full"

            const val USER_ID = Screen.USER_ID
        }

        data object RenameComputer : Screen(
            "rename/{userId}/shares/{shareId}/files?fileId={fileId}&folderId={folderId}&deviceId={deviceId}"
        ) {

            operator fun invoke(
                userId: UserId,
                deviceId: DeviceId,
                folderId: FolderId,
            ) = "rename/${userId.id}/shares/${folderId.shareId.id}/files?folderId=${folderId.id}&deviceId=${deviceId.id}"

            const val FOLDER_ID = RenameViewModel.KEY_FOLDER_ID
            const val SHARE_ID = RenameViewModel.KEY_SHARE_ID
            const val DEVICE_ID = RenameDeviceViewModel.KEY_DEVICE_ID
        }
    }

    data object GetMoreFreeStorage : Screen("storage/{userId}/getMoreFree") {

        operator fun invoke(userId: UserId) = "storage/${userId.id}/getMoreFree"

        const val USER_ID = Screen.USER_ID
    }

    data object Log : Screen("log/{userId}/show") {

        operator fun invoke(userId: UserId) = "log/${userId.id}/show"

        data object Options : Screen(
            "log/{userId}/options"
        ) {

            operator fun invoke(
                userId: UserId,
            ) = "log/${userId.id}/options"
        }

        const val USER_ID = Screen.USER_ID
    }

    data object Onboarding : Screen("onboarding/{userId}/show") {

        operator fun invoke(userId: UserId) = "onboarding/${userId.id}/show"

        const val USER_ID = Screen.USER_ID
    }

    data object WhatsNew : Screen("whatsNew/{userId}/show/{key}") {

        operator fun invoke(userId: UserId, key: WhatsNewKey) = "whatsNew/${userId.id}/show/${key}"

        const val USER_ID = Screen.USER_ID
        const val KEY = WhatsNewViewModel.KEY
    }

    data object NotificationPermissionRationale : Screen(
        "rationale/{userId}/notificationPermission?context={rationaleContext}"
    ) {

        operator fun invoke(
            userId: UserId,
            rationaleContext: NotificationPermissionRationaleViewModel.RationaleContext,
        ) = "rationale/${userId.id}/notificationPermission?context=${rationaleContext.name}"

        const val USER_ID = Screen.USER_ID
        const val RATIONALE_CONTEXT = NotificationPermissionRationaleViewModel.RATIONALE_CONTEXT
    }

    companion object {
        // "userId" needs to be shared as it is a requirement for [DriveViewModelModule]
        // to work properly. This should be address later.
        const val USER_ID = UserViewModel.KEY_USER_ID

        private fun filesBrowsableRoute(prefix: String) =
            "$prefix/{userId}/files?folderId={folderId}&shareId={shareId}&folderName={folderName}"

        private fun filesBrowsableBuildRoute(
            prefix: String,
            userId: UserId,
            folderId: FolderId?,
            folderName: String?,
        ) =
            buildString {
                append(prefix)
                append("/${userId.id}/files")
                if (folderId != null) {
                    append("?shareId=${folderId.shareId.id}")
                    append("&folderId=${folderId.id}")
                    if (folderName != null) {
                        append("&folderName=$folderName")
                    }
                }
            }
    }
    data object Promo {
        data object Subscription : Screen("home/{userId}/promo/subscription?key={key}"){
            operator fun invoke(userId: UserId, key: String) = "home/${userId.id}/promo/subscription?key=$key"
            const val PROMO_KEY = SubscriptionPromoViewModel.PROMO_KEY
        }
    }
}

fun NavHostController.navigate(screen: Screen, builder: NavOptionsBuilder.() -> Unit) {
    val route = screen.route
    require(!route.contains('{')) { "Screen with arguments detected $route" }
    navigate(route, builder)
}

enum class PagerType(val type: String) {
    FOLDER("folder"),
    OFFLINE("offline"),
    SINGLE("single"),
    PHOTO("photo"),
    ALBUM("album"),
}

interface HomeTab {
    val route: String
    operator fun invoke(userId: UserId): String
}

@Serializable
@Parcelize
data class UriWithFileName(
    val uri: String,
    val fileName: String,
) : Parcelable

@Serializable
@Parcelize
data class UploadParameters(
    val uris: List<UriWithFileName>,
) : Parcelable
