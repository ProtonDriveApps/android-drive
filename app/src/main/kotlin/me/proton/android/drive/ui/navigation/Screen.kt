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

package me.proton.android.drive.ui.navigation

import android.net.Uri
import android.os.Parcelable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.proton.android.drive.ui.options.OptionsFilter
import me.proton.android.drive.ui.viewmodel.FileOrFolderOptionsViewModel
import me.proton.android.drive.ui.viewmodel.MoveToFolderViewModel
import me.proton.android.drive.ui.viewmodel.MultipleFileOrFolderOptionsViewModel
import me.proton.android.drive.ui.viewmodel.ParentFolderOptionsViewModel
import me.proton.android.drive.ui.viewmodel.UploadToViewModel
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.rename.presentation.RenameViewModel
import me.proton.core.drive.drivelink.shared.presentation.viewmodel.SharedDriveLinkViewModel
import me.proton.core.drive.folder.create.presentation.CreateFolderViewModel
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction

sealed class Screen(val route: String) {
    open fun deepLink(baseUrl: String): String? = "$baseUrl/$route"

    data object Launcher : Screen("launcher")

    data object SigningOut : Screen("signingOut/{userId}") {
        operator fun invoke(userId: UserId) = "signingOut/${userId.id}"

        const val USER_ID = Screen.USER_ID
    }

    data object Home : Screen("home/{userId}") {
        operator fun invoke(userId: UserId) = "home/${userId.id}"

        const val USER_ID = Screen.USER_ID
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
        "options/link/{userId}/shares/{shareId}/linkId={linkId}?optionsFilter={optionsFilter}"
    ) {
        operator fun invoke(
            userId: UserId,
            linkId: LinkId,
            optionsFilter: OptionsFilter = OptionsFilter.FILES,
        ) = "options/link/${userId.id}/shares/${linkId.shareId.id}/linkId=${linkId.id}?optionsFilter=${optionsFilter.type}"

        const val SHARE_ID = FileOrFolderOptionsViewModel.KEY_SHARE_ID
        const val LINK_ID = FileOrFolderOptionsViewModel.KEY_LINK_ID
        const val OPTIONS_FILTER = FileOrFolderOptionsViewModel.OPTIONS_FILTER
    }

    data object MultipleFileOrFolderOptions : Screen(
        "options/multiple/{userId}/selectionId={selectionId}?optionsFilter={optionsFilter}"
    ) {
        operator fun invoke(
            userId: UserId,
            selectionId: SelectionId,
            optionsFilter: OptionsFilter = OptionsFilter.FILES,
        ) = "options/multiple/${userId.id}/selectionId=${selectionId.id}?optionsFilter=${optionsFilter.type}"

        const val SELECTION_ID = MultipleFileOrFolderOptionsViewModel.KEY_SELECTION_ID
        const val OPTIONS_FILTER = MultipleFileOrFolderOptionsViewModel.OPTIONS_FILTER
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

    data object Info : Screen("info/{userId}/shares/{shareId}/files?linkId={linkId}") {
        operator fun invoke(
            userId: UserId,
            linkId: LinkId,
        ) = "info/${userId.id}/shares/${linkId.shareId.id}/files?linkId=${linkId.id}"

        const val USER_ID = Screen.USER_ID
        const val LINK_ID = "linkId"
        const val SHARE_ID = "shareId"
    }

    object Files : Screen(filesBrowsableRoute("home")), HomeTab {

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

            data object ConfirmEmptyTrash : Screen("delete/{userId}/trash") {
                operator fun invoke(userId: UserId) = "delete/${userId.id}/trash"
            }

            data object Rename : Screen("rename/{userId}/shares/{shareId}/files?fileId={fileId}&folderId={folderId}") {
                operator fun invoke(
                    userId: UserId,
                    linkId: LinkId,
                ) = when (linkId) {
                    is FileId -> "rename/${userId.id}/shares/${linkId.shareId.id}/files?fileId=${linkId.id}"
                    is FolderId -> "rename/${userId.id}/shares/${linkId.shareId.id}/files?folderId=${linkId.id}"
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

            data object ConfirmStopSharing : Screen("share_url/{userId}/shares/{shareId}/links/{linkId}/confirm_delete?confirmPopUpRoute={confirmPopUpRoute}&confirmPopUpRouteInclusive={confirmPopUpRouteInclusive}") {
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
                const val SHARE_ID = "shareId"
                const val CONFIRM_POP_UP_ROUTE = "confirmPopUpRoute"
                const val CONFIRM_POP_UP_ROUTE_INCLUSIVE = "confirmPopUpRouteInclusive"
            }
        }

        const val USER_ID = Screen.USER_ID
        const val FOLDER_ID = "folderId"
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

    object Shared : Screen("home/{userId}/shared/{shareId}"), HomeTab {

        override fun invoke(userId: UserId) = invoke(userId, null)

        operator fun invoke(userId: UserId, shareId: ShareId?) = "home/${userId.id}/shared/${shareId?.id}"

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = "shareId"
    }
    object Photos : Screen("home/{userId}/photos/{shareId}"), HomeTab {

        override fun invoke(userId: UserId) = invoke(userId, null)

        operator fun invoke(userId: UserId, shareId: ShareId?) = "home/${userId.id}/photos/${shareId?.id}"

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = "shareId"
    }
    data object BackupIssues : Screen("backup/issues/{userId}/shares/{shareId}/folder/{folderId}") {

        fun invoke(folderId: FolderId) = "backup/issues/${folderId.shareId.userId.id}/shares/${folderId.shareId.id}/folder/${folderId.id}"

        object Dialogs {
            object ConfirmSkipIssues : Screen("backup/issues/{userId}/shares/{shareId}/folder/{folderId}/confirm_skip?confirmPopUpRoute={confirmPopUpRoute}&confirmPopUpRouteInclusive={confirmPopUpRouteInclusive}"){
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

    object Computers : Screen(filesBrowsableRoute("computers") + "&syncedFolders={syncedFolders}"), HomeTab {
        override fun invoke(userId: UserId) =
            filesBrowsableBuildRoute("computers", userId, null, null)

        operator fun invoke(userId: UserId, folderId: FolderId, folderName: String?, syncedFolders: Boolean) =
            filesBrowsableBuildRoute("computers", userId, folderId, folderName) + "&syncedFolders=${syncedFolders}"

        const val USER_ID = Screen.USER_ID
        const val SYNCED_FOLDERS = "syncedFolders"
    }

    data object Trash : Screen("trash/{userId}") {
        operator fun invoke(userId: UserId) = "trash/${userId.id}"

        const val USER_ID = Screen.USER_ID
    }

    object OfflineFiles : Screen(filesBrowsableRoute("offline")) {
        operator fun invoke(userId: UserId, folderId: FolderId? = null, folderName: String? = null) =
            filesBrowsableBuildRoute("offline", userId, folderId, folderName)
    }

    object PagerPreview : Screen("pager/{pagerType}/preview/{userId}/shares/{shareId}/files/{fileId}?optionsFilter={optionsFilter}") {
        operator fun invoke(
            pagerType: PagerType,
            userId: UserId,
            fileId: FileId,
            optionsFilter: OptionsFilter = OptionsFilter.FILES
        ) =
            "pager/${pagerType.type}/preview/${userId.id}/shares/${fileId.shareId.id}/files/${fileId.id}?optionsFilter=${optionsFilter.type}"

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = "shareId"
        const val FILE_ID = "fileId"
        const val PAGER_TYPE = "pagerType"
        const val OPTIONS_FILTER = FileOrFolderOptions.OPTIONS_FILTER
    }

    object Settings : Screen("settings/{userId}") {
        operator fun invoke(userId: UserId) = "settings/${userId.id}"

        const val USER_ID = Screen.USER_ID
        const val SHARE_ID = "shareId"
        const val FOLDER_ID = "folderId"

        object AppAccess : Screen("settings/{userId}/appAccess") {

            operator fun invoke(userId: UserId) = "settings/${userId.id}/appAccess"

            object Dialogs {

                object SystemAccess : Screen("settings/{userId}/appAccess/systemAccess") {
                    operator fun invoke(userId: UserId) = "settings/${userId.id}/appAccess/systemAccess"
                }
            }
        }

        object AutoLockDurations : Screen("settings/{userId}/autoLockDurations") {
            operator fun invoke(userId: UserId) = "settings/${userId.id}/autoLockDurations"
        }

        object PhotosBackup : Screen("settings/{userId}/photosBackup") {
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
    }

    object SendFile : Screen("send/{userId}/shares/{shareId}/files/{fileId}") {
        operator fun invoke(userId: UserId, fileId: FileId) =
            "send/${userId.id}/shares/${fileId.shareId.id}/files/${fileId.id}"

        const val SHARE_ID = "shareId"
        const val FILE_ID = "fileId"
    }

    object ShareViaLink : Screen("shareViaLink/{userId}/shares/{shareId}/linkId/{linkId}") {
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

    object Upload : Screen("upload/{userId}/files?uris={uris}&parentShareId={parentShareId}&parentId={parentId}") {
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
        object SignOut : Screen("home/{userId}/signout") {
            operator fun invoke(userId: UserId) = "home/${userId.id}/signout"

            const val USER_ID = Files.USER_ID
        }

        object StorageFull : Screen("storage/{userId}/full") {
            operator fun invoke(userId: UserId) = "storage/${userId.id}/full"

            const val USER_ID = Screen.USER_ID
        }
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
}

fun NavHostController.navigate(screen: Screen, builder: NavOptionsBuilder.() -> Unit) {
    val route = screen.route
    require(!route.contains('{')) { "Screen with arguments detected $route" }
    navigate(route, builder)
}

enum class PagerType(val type: String) {
    FOLDER("folder"), OFFLINE("offline"), SINGLE("single"), PHOTO("photo")
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
