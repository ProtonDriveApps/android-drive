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
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.hasShareLink
import me.proton.core.drive.drivelink.domain.extension.isPhoto
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.extension.off
import me.proton.core.drive.files.presentation.entry.DeletePermanentlyEntry
import me.proton.core.drive.files.presentation.entry.DownloadEntry
import me.proton.core.drive.files.presentation.entry.DownloadFileEntity
import me.proton.core.drive.files.presentation.entry.FileInfoEntry
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.files.presentation.entry.ManageAccessEntity
import me.proton.core.drive.files.presentation.entry.MoveEntry
import me.proton.core.drive.files.presentation.entry.MoveFileEntry
import me.proton.core.drive.files.presentation.entry.OpenInBrowserProtonDocsEntity
import me.proton.core.drive.files.presentation.entry.RemoveMeEntry
import me.proton.core.drive.files.presentation.entry.RenameFileEntry
import me.proton.core.drive.files.presentation.entry.SendFileEntry
import me.proton.core.drive.files.presentation.entry.ShareViaInvitationsEntity
import me.proton.core.drive.files.presentation.entry.ToggleOfflineEntry
import me.proton.core.drive.files.presentation.entry.ToggleTrashEntry
import me.proton.core.drive.files.presentation.entry.TrashEntry
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.isProtonCloudFile
import me.proton.core.drive.link.domain.extension.isSharedUrlExpired
import me.proton.core.drive.i18n.R as I18N
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
            notificationDotVisible: Boolean,
            createDocument: (FolderId) -> Unit,
        ) = folderEntry(
            icon = CorePresentation.drawable.ic_proton_file,
            labelResId = I18N.string.folder_option_create_document,
            notificationDotVisible = notificationDotVisible,
            runAction = runAction,
        ) {
            createDocument(id)
        }
    }

    data object CreateFolder : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED, State.SHARED, State.NOT_SHARED),
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

    data object Download : Option(
        ApplicableQuantity.All,
        ApplicableTo.ANY_DOWNLOADABLE_FILE,
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            showCreateDocumentPicker: (String) -> Unit,
        ) = DownloadFileEntity { driveLink ->
            showCreateDocumentPicker(driveLink.name)
        }

        fun build(
            runAction: RunAction,
            download: () -> Unit
        ) = DownloadEntry {
            runAction { download() }
        }
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
            runAction { navigateToMove(driveLink.id, driveLink.parentId) }
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
        setOf(ApplicableTo.FOLDER) + ApplicableTo.ANY_DOWNLOADABLE_FILE,
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
        ) = OpenInBrowserProtonDocsEntity { driveLink ->
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
        setOf(ApplicableTo.FILE_MAIN, ApplicableTo.FILE_DEVICE, ApplicableTo.FILE_PROTON_CLOUD, ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToRename: (linkId: LinkId) -> Unit,
        ) = RenameFileEntry { driveLink ->
            runAction { navigateToRename(driveLink.id) }
        }
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
        setOf(ApplicableTo.FOLDER) + ApplicableTo.ANY_FILE,
        setOf(State.NOT_TRASHED) + State.ANY_SHARED
    ) {
        fun build(
            runAction: RunAction,
            navigateToManageAccess: (LinkId) -> Unit,
        ) = ManageAccessEntity { driveLink ->
            runAction { navigateToManageAccess(driveLink.id) }
        }
    }

    data object ShareViaInvitations : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER) + ApplicableTo.ANY_FILE,
        setOf(State.NOT_TRASHED) + State.ANY_SHARED
    ) {
        fun build(
            runAction: RunAction,
            navigateToShareViaInvitations: (LinkId) -> Unit,
        ) = ShareViaInvitationsEntity { driveLink ->
            runAction { navigateToShareViaInvitations(driveLink.id) }
        }
    }
}

sealed class ApplicableQuantity(open val quantity: Long) {
    object Single : ApplicableQuantity(1L)
    object All : ApplicableQuantity(Long.MAX_VALUE)
}

enum class ApplicableTo {
    FILE_MAIN,
    FILE_PHOTO,
    FILE_DEVICE,
    FILE_PROTON_CLOUD,
    FOLDER;

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
}

fun Set<Option>.filter(driveLink: DriveLink) =
    filter { option ->
        option.applicableStates.containsAll(driveLink.toOptionState()) && driveLink.isApplicableTo(option.applicableTo)
    }

private val photosOptions = listOf(
    Option.OfflineToggle,
    Option.ShareViaInvitations,
    Option.ManageAccess,
    Option.SendFile,
    Option.Download,
    Option.Info,
    Option.Trash,
)

fun Iterable<Option>.filter(optionsFilter: OptionsFilter) =
    when (optionsFilter) {
        OptionsFilter.FILES -> this
        OptionsFilter.PHOTOS -> filter { option -> option in photosOptions }
    }

fun Iterable<Option>.filterRoot(driveLink: DriveLink, featureFlag: FeatureFlag) = filter { option ->
    if (driveLink.parentId == null) {
        when (option) {
            Option.Rename -> false //featureFlag.state == FeatureFlag.State.ENABLED
            Option.Trash -> featureFlag.state == FeatureFlag.State.ENABLED
            Option.Move -> false
            else -> true
        }
    } else {
        true
    }
}


fun Iterable<Option>.filterShareMember(isMember: Boolean) = filter { option ->
    if (!isMember) {
        when (option) {
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
        Option.CreateDocument -> permissions.canWrite
        Option.CreateFolder -> permissions.canWrite
        Option.DeletePermanently -> permissions.canWrite
        Option.Download -> permissions.canRead
        Option.Info -> permissions.canRead
        Option.ManageAccess -> permissions.isAdmin
        Option.Move -> permissions.canWrite
        Option.OfflineToggle -> permissions.canRead
        Option.OpenInBrowser -> permissions.canRead
        Option.Rename -> permissions.canWrite
        Option.SendFile -> permissions.canRead
        Option.ShareViaInvitations -> permissions.isAdmin
        Option.TakeAPhoto -> permissions.canWrite
        Option.Trash -> permissions.isAdmin
        Option.UploadFile -> permissions.canWrite
        Option.RemoveMe -> permissions.canRead
    }
}

fun Iterable<Option>.filterProtonDocs(killSwitch: FeatureFlag) = filter { option ->
    when (option) {
        Option.CreateDocument -> killSwitch.off
        Option.OpenInBrowser -> killSwitch.off
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
