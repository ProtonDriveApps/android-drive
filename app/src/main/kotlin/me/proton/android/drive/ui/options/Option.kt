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

package me.proton.android.drive.ui.options

import me.proton.android.drive.ui.common.FolderEntry
import me.proton.android.drive.ui.common.folderEntry
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.isViewerOrEditorOnly
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.hasShareLink
import me.proton.core.drive.drivelink.domain.extension.isPhoto
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.extension.off
import me.proton.core.drive.files.presentation.entry.AddToAlbumsEntry
import me.proton.core.drive.files.presentation.entry.AddToAlbumsFileEntry
import me.proton.core.drive.files.presentation.entry.DeleteAlbumEntry
import me.proton.core.drive.files.presentation.entry.DeletePermanentlyEntry
import me.proton.core.drive.files.presentation.entry.DownloadEntry
import me.proton.core.drive.files.presentation.entry.DownloadFileEntry
import me.proton.core.drive.files.presentation.entry.FileInfoEntry
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.files.presentation.entry.LeaveAlbumEntry
import me.proton.core.drive.files.presentation.entry.ManageAccessEntry
import me.proton.core.drive.files.presentation.entry.MoveEntry
import me.proton.core.drive.files.presentation.entry.MoveFileEntry
import me.proton.core.drive.files.presentation.entry.OpenInBrowserProtonDocsEntry
import me.proton.core.drive.files.presentation.entry.RemoveFromAlbumEntry
import me.proton.core.drive.files.presentation.entry.RemoveFromAlbumFileEntry
import me.proton.core.drive.files.presentation.entry.RemoveMeEntry
import me.proton.core.drive.files.presentation.entry.RenameFileEntry
import me.proton.core.drive.files.presentation.entry.SaveSharePhotoEntry
import me.proton.core.drive.files.presentation.entry.SendFileEntry
import me.proton.core.drive.files.presentation.entry.SetAsAlbumCoverEntry
import me.proton.core.drive.files.presentation.entry.ShareMultiplePhotosEntry
import me.proton.core.drive.files.presentation.entry.ShareViaInvitationsEntry
import me.proton.core.drive.files.presentation.entry.TagPhotoEntry
import me.proton.core.drive.files.presentation.entry.TagPhotoFileEntry
import me.proton.core.drive.files.presentation.entry.ToggleFavoriteFileEntry
import me.proton.core.drive.files.presentation.entry.ToggleOfflineEntry
import me.proton.core.drive.files.presentation.entry.ToggleTrashEntry
import me.proton.core.drive.files.presentation.entry.TrashEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.isProtonCloudFile
import me.proton.core.drive.link.domain.extension.isSharedUrlExpired
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

sealed class Option(
    val applicableQuantity: ApplicableQuantity,
    val applicableTo: Set<ApplicableTo>,
    val applicableStates: Set<State>,
) {
    data object CreateDocument : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            createDocument: (FolderId) -> Unit,
        ) = folderEntry(
            icon = BasePresentation.drawable.ic_proton_docs_filled,
            labelResId = I18N.string.folder_option_create_document,
            runAction = runAction,
        ) {
            createDocument(id)
        }
    }

    data object CreateFolder : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToCreateFolder: (FolderId) -> Unit,
        ) = folderEntry(
            icon = CorePresentation.drawable.ic_proton_folder_plus,
            labelResId = I18N.string.folder_option_create_folder,
            runAction = runAction,
        ) {
            navigateToCreateFolder(id)
        }
    }

    data object CreateSpreadsheet : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            createSpreadsheet: (FolderId) -> Unit,
        ) = folderEntry(
            icon = BasePresentation.drawable.ic_proton_sheets_filled,
            labelResId = I18N.string.folder_option_create_spreadsheet,
            runAction = runAction,
        ) {
            createSpreadsheet(id)
        }
    }

    data object DeletePermanently : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FOLDER) + ApplicableTo.ANY_FILE,
        setOf(State.TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToDelete: (linkId: LinkId) -> Unit,
        ) = DeletePermanentlyEntry { driveLink ->
            runAction { navigateToDelete(driveLink.id) }
        }
    }

    data object DeleteAlbum : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.ALBUM),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToDelete: (albumId: AlbumId) -> Unit,
        ) = DeleteAlbumEntry { driveLink ->
                runAction { navigateToDelete(driveLink.id) }
            }
    }

    data object Download : Option(
        ApplicableQuantity.All,
        ApplicableTo.ANY_DOWNLOADABLE_FILE,
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            showCreateDocumentPicker: (String) -> Unit,
        ) = DownloadFileEntry { driveLink ->
            showCreateDocumentPicker(driveLink.name)
        }

        fun build(
            runAction: RunAction,
            download: () -> Unit
        ) = DownloadEntry {
            runAction { download() }
        }
    }

    data object FavoriteToggle : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE_PHOTO),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            toggleFavorite: suspend (DriveLink.File) -> Unit,
        ) = ToggleFavoriteFileEntry { driveLink ->
            runAction { toggleFavorite(driveLink) }
        } as FileOptionEntry<DriveLink>
    }

    data object Info : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER) + ApplicableTo.ANY_FILE,
        State.ANY_TRASHED + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToInfo: (linkId: LinkId) -> Unit,
        ) = FileInfoEntry { driveLink ->
            runAction { navigateToInfo(driveLink.id) }
        }
    }

    data object Move : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FILE_MAIN, ApplicableTo.FILE_DEVICE, ApplicableTo.FILE_PROTON_CLOUD, ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToMove: (linkId: LinkId, parentId: FolderId?) -> Unit,
        ) = MoveFileEntry { driveLink ->
            runAction { navigateToMove(driveLink.id, driveLink.parentId as? FolderId) }
        }

        fun build(
            runAction: RunAction,
            navigateToMoveAll: () -> Unit,
        ) = MoveEntry {
            runAction { navigateToMoveAll() }
        }
    }

    data object OfflineToggle : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER, ApplicableTo.ALBUM) + ApplicableTo.ANY_DOWNLOADABLE_FILE,
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            toggleOffline: suspend (DriveLink) -> Unit,
        ) = ToggleOfflineEntry { driveLink ->
            runAction {
                toggleOffline(driveLink)
            }
        }
    }

    data object OpenInBrowser : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE_PROTON_CLOUD),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            openInBrowser: suspend (DriveLink.File) -> Unit,
        ) = OpenInBrowserProtonDocsEntry { driveLink ->
            runAction {
                openInBrowser(driveLink)
            }
        } as FileOptionEntry<DriveLink>
    }

    data object RemoveMe : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER) + ApplicableTo.ANY_FILE,
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            removeMe: suspend (DriveLink) -> Unit,
        ) = RemoveMeEntry { driveLink ->
            runAction {
                removeMe(driveLink)
            }
        }
    }

    data object Rename : Option(
        ApplicableQuantity.Single,
        setOf(
            ApplicableTo.FILE_MAIN,
            ApplicableTo.FILE_DEVICE,
            ApplicableTo.FILE_PROTON_CLOUD,
            ApplicableTo.FOLDER,
            ApplicableTo.ALBUM,
        ),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToRename: (linkId: LinkId) -> Unit,
        ) = RenameFileEntry { driveLink ->
            runAction { navigateToRename(driveLink.id) }
        }
    }


    data object SaveSharePhoto : Option(
        ApplicableQuantity.Single,
        setOf(
            ApplicableTo.FILE_PHOTO,
        ),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            saveSharePhoto: (link: DriveLink.File) -> Unit,
        ) = SaveSharePhotoEntry { driveLink ->
            runAction { saveSharePhoto(driveLink) }
        } as FileOptionEntry<DriveLink>
    }

    data object SendFile : Option(
        ApplicableQuantity.Single,
        ApplicableTo.ANY_DOWNLOADABLE_FILE,
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        @Suppress("UNCHECKED_CAST")
        fun build(
            runAction: RunAction,
            navigateToSendFile: (fileId: FileId) -> Unit,
        ) = SendFileEntry { driveLink ->
            runAction { navigateToSendFile(driveLink.id) }
        } as FileOptionEntry<DriveLink>
    }

    data object TakeAPhoto : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            notificationDotVisible: Boolean = false,
            takeAPhoto: () -> Unit,
        ) = FolderEntry(
            icon = CorePresentation.drawable.ic_proton_camera,
            labelResId = I18N.string.folder_option_take_a_photo,
            notificationDotVisible = notificationDotVisible,
            onClick = { takeAPhoto() }
        )
    }

    data object Trash : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FOLDER) + ApplicableTo.ANY_FILE,
        State.ANY_TRASHED + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            toggleTrash: suspend (DriveLink) -> Unit,
        ) = ToggleTrashEntry { driveLink ->
            runAction {
                toggleTrash(driveLink)
            }
        }

        fun build(
            runAction: RunAction,
            moveToTrash: suspend () -> Unit,
        ) = TrashEntry {
            runAction {
                moveToTrash()
            }
        }
    }

    data object UploadFile : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            notificationDotVisible: Boolean = false,
            showFilePicker: () -> Unit,
        ) = FolderEntry(
            icon = CorePresentation.drawable.ic_proton_file_arrow_in_up,
            labelResId = I18N.string.folder_option_import_file,
            notificationDotVisible = notificationDotVisible,
            onClick = { showFilePicker() }
        )
    }

    data object ManageAccess : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER, ApplicableTo.ALBUM) + ApplicableTo.ANY_FILE,
        setOf(State.NOT_TRASHED) + State.ANY_SHARED
    ) {
        fun build(
            runAction: RunAction,
            navigateToManageAccess: (LinkId) -> Unit,
        ) = ManageAccessEntry { driveLink ->
            runAction { navigateToManageAccess(driveLink.id) }
        }
    }

    data object ShareViaInvitations : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER, ApplicableTo.ALBUM) + ApplicableTo.ANY_FILE,
        setOf(State.NOT_TRASHED) + State.ANY_SHARED
    ) {
        fun build(
            runAction: RunAction,
            navigateToShareViaInvitations: (LinkId) -> Unit,
        ) = ShareViaInvitationsEntry { driveLink ->
            runAction { navigateToShareViaInvitations(driveLink.id) }
        }
    }

    data object ShareMultiplePhotos : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FILE_PHOTO),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToShareMultiplePhotosOptions: () -> Unit,
        ) = ShareMultiplePhotosEntry {
            runAction { navigateToShareMultiplePhotosOptions() }
        }
    }

    data object AddToAlbums : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FILE_PHOTO),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED
    ) {
        fun build(
            runAction: RunAction,
            navigateToAddToAlbumsOptions: () -> Unit,
        ) = AddToAlbumsEntry {
            runAction { navigateToAddToAlbumsOptions() }
        }

        fun build(
            runAction: RunAction,
            addToAlbums: (DriveLink.File) -> Unit,
        ) = AddToAlbumsFileEntry { driveLink ->
            runAction { addToAlbums(driveLink) }
        } as FileOptionEntry<DriveLink>
    }

    data object SetAsAlbumCover : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE_PHOTO),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED
    ) {
        fun build(
            runAction: RunAction,
            setAsAlbumCover: (DriveLink.File) -> Unit,
        ) = SetAsAlbumCoverEntry { driveLink ->
            runAction { setAsAlbumCover(driveLink) }
        } as FileOptionEntry<DriveLink>
    }

    data object RemoveFromAlbum : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FILE_PHOTO),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED
    ) {
        fun build(
            runAction: RunAction,
            removeFromAlbum: (DriveLink.File) -> Unit,
        ) = RemoveFromAlbumFileEntry { driveLink ->
            runAction {
                removeFromAlbum(driveLink)
            }
        } as FileOptionEntry<DriveLink>

        fun build(
            runAction: RunAction,
            removeSelectedFromAlbum: () -> Unit,
        ) = RemoveFromAlbumEntry {
            runAction {
                removeSelectedFromAlbum()
            }
        }
    }

    data object LeaveAlbum : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.ALBUM),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            leaveAlbum: suspend (DriveLink.Album) -> Unit,
        ) = LeaveAlbumEntry { album ->
            runAction {
                leaveAlbum(album)
            }
        }
    }

    data object TagPhotoFile : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FILE_PHOTO),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            tagPhoto: suspend (DriveLink.File) -> Unit,
        ) = TagPhotoFileEntry { photoFile ->
            runAction {
                tagPhoto(photoFile)
            }
        } as FileOptionEntry<DriveLink>
        fun build(
            runAction: RunAction,
            tagPhotos: suspend () -> Unit,
        ) = TagPhotoEntry {
            runAction {
                tagPhotos()
            }
        }
    }
}

sealed class ApplicableQuantity(open val quantity: Long) {
    data object Single : ApplicableQuantity(1L)
    data object All : ApplicableQuantity(Long.MAX_VALUE)
}

enum class ApplicableTo {
    FILE_MAIN,
    FILE_PHOTO,
    FILE_DEVICE,
    FILE_PROTON_CLOUD,
    FOLDER,
    ALBUM;

    companion object {
        val ANY_FILE: Set<ApplicableTo> get() = setOf(FILE_MAIN, FILE_PHOTO, FILE_DEVICE, FILE_PROTON_CLOUD)
        val ANY_DOWNLOADABLE_FILE: Set<ApplicableTo> get() = setOf(FILE_MAIN, FILE_PHOTO, FILE_DEVICE)
    }
}

enum class State {
    TRASHED,
    NOT_TRASHED,
    SHARED,
    SHARED_EXPIRED,
    NOT_SHARED;

    companion object {
        val ANY_TRASHED: Set<State> get() = setOf(TRASHED, NOT_TRASHED)
        val ANY_SHARED: Set<State> get() = setOf(SHARED, SHARED_EXPIRED, NOT_SHARED)
    }
}

fun DriveLink.toOptionState(): Set<State> = setOf(
    if (isTrashed) State.TRASHED else State.NOT_TRASHED,
    when {
        hasShareLink && isSharedUrlExpired -> State.SHARED_EXPIRED
        hasShareLink -> State.SHARED
        else -> State.NOT_SHARED
    },
)

fun DriveLink.isApplicableTo(applicableTo: Set<ApplicableTo>): Boolean = when (this) {
    is DriveLink.File -> when {
        isPhoto -> applicableTo.contains(ApplicableTo.FILE_PHOTO)
        isProtonCloudFile -> applicableTo.contains(ApplicableTo.FILE_PROTON_CLOUD)
        else -> applicableTo.any { it in setOf(ApplicableTo.FILE_MAIN, ApplicableTo.FILE_DEVICE) }
    }
    is DriveLink.Folder -> applicableTo.contains(ApplicableTo.FOLDER)
    is DriveLink.Album -> applicableTo.contains(ApplicableTo.ALBUM)
}

fun Set<Option>.filter(driveLink: DriveLink) =
    filter { option ->
        option.applicableStates.containsAll(driveLink.toOptionState()) && driveLink.isApplicableTo(option.applicableTo)
    }

fun Iterable<Option>.filterRoot(driveLink: DriveLink, featureFlag: FeatureFlag) = filter { option ->
    if (driveLink.parentId == null) {
        when (option) {
            Option.Rename -> false //featureFlag.state == FeatureFlag.State.ENABLED
            Option.Trash -> featureFlag.state == FeatureFlag.State.ENABLED
            Option.Move -> false
            Option.FavoriteToggle -> false
            else -> true
        }
    } else {
        true
    }
}


fun Iterable<Option>.filterShareMember(isMember: Boolean) = filter { option ->
    if (!isMember) {
        when (option) {
            Option.LeaveAlbum -> false
            Option.RemoveMe -> false
            else -> true
        }
    } else {
        true
    }
}

fun Iterable<Option>.filterPermissions(
    permissions: Permissions
) = filter { option ->
    when (option) {
        Option.AddToAlbums -> permissions.canWrite
        Option.CreateDocument -> permissions.canWrite
        Option.CreateFolder -> permissions.canWrite
        Option.CreateSpreadsheet -> permissions.canWrite
        Option.DeleteAlbum -> permissions.isAdmin
        Option.DeletePermanently -> permissions.canWrite
        Option.Download -> permissions.canRead
        Option.Info -> permissions.canRead
        Option.LeaveAlbum -> permissions.canRead
        Option.ManageAccess -> permissions.isAdmin
        Option.Move -> permissions.canWrite
        Option.OfflineToggle -> permissions.canRead
        Option.FavoriteToggle -> permissions.canWrite
        Option.OpenInBrowser -> permissions.canRead
        Option.Rename -> permissions.canWrite
        Option.RemoveFromAlbum -> permissions.canWrite
        Option.RemoveMe -> permissions.canRead
        Option.SaveSharePhoto -> permissions.isViewerOrEditorOnly
        Option.SendFile -> permissions.canRead
        Option.SetAsAlbumCover -> permissions.isAdmin
        Option.ShareViaInvitations -> permissions.isAdmin
        Option.ShareMultiplePhotos -> permissions.isAdmin
        Option.TagPhotoFile -> permissions.isAdmin
        Option.TakeAPhoto -> permissions.canWrite
        Option.Trash -> permissions.isAdmin
        Option.UploadFile -> permissions.canWrite
    }
}

fun Iterable<Option>.filterProtonDocs(killSwitch: FeatureFlag) = filter { option ->
    when (option) {
        Option.CreateDocument -> killSwitch.off
        else -> true
    }
}

fun Iterable<Option>.filterProtonSheets(isEnabled: Boolean) = filter { option ->
    when (option) {
        Option.CreateSpreadsheet -> isEnabled
        else -> true
    }
}

fun Iterable<Option>.filterAlbums(
    isEnabled: Boolean,
    killSwitch: FeatureFlag,
    albumId: AlbumId? = null,
) = filter { option ->
    val featureEnabled = isEnabled && killSwitch.off
    when (option) {
        Option.TagPhotoFile -> featureEnabled && albumId == null
        Option.AddToAlbums -> featureEnabled && albumId == null
        Option.ShareMultiplePhotos -> featureEnabled && albumId == null
        Option.RemoveFromAlbum -> featureEnabled && albumId != null
        Option.SetAsAlbumCover -> featureEnabled && albumId != null
        Option.Trash -> albumId == null
        Option.SaveSharePhoto -> featureEnabled && albumId != null
        else -> true
    }
}

fun Iterable<Option>.filterPhotoFavorite(
    isEnabled: Boolean,
    killSwitch: FeatureFlag,
) = filter { option ->
    val featureEnabled = isEnabled && killSwitch.off
    when (option) {
        Option.FavoriteToggle -> featureEnabled
        else -> true
    }
}

fun Iterable<Option>.filterPhotoTag(
    isEnabled: Boolean,
) = filter { option ->
    when (option) {
        Option.TagPhotoFile -> isEnabled
        else -> true
    }
}

fun Iterable<Option>.filterShare(
    shareTempDisabledOn: Boolean,
    albumId: AlbumId? = null,
) = filter { option ->
    when (option) {
        Option.ShareViaInvitations -> albumId == null && !shareTempDisabledOn
        Option.ManageAccess -> albumId == null && !shareTempDisabledOn
        Option.ShareMultiplePhotos -> !shareTempDisabledOn
        else -> true
    }
}

fun Set<Option>.filterAll(driveLinks: List<DriveLink>) =
    filter { option ->
        driveLinks.size <= option.applicableQuantity.quantity &&
                driveLinks.all { driveLink ->
                    option.applicableStates.containsAll(driveLink.toOptionState()) &&
                            driveLink.isApplicableTo(option.applicableTo)
                }
    }
