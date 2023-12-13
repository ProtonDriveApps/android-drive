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

package me.proton.android.drive.ui.options

import me.proton.android.drive.ui.common.FolderEntry
import me.proton.android.drive.ui.common.folderEntry
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.entry.CopySharedLinkEntity
import me.proton.core.drive.files.presentation.entry.DeletePermanentlyEntry
import me.proton.core.drive.files.presentation.entry.DownloadEntry
import me.proton.core.drive.files.presentation.entry.DownloadFileEntity
import me.proton.core.drive.files.presentation.entry.FileInfoEntry
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.files.presentation.entry.MoveEntry
import me.proton.core.drive.files.presentation.entry.MoveFileEntry
import me.proton.core.drive.files.presentation.entry.RenameFileEntry
import me.proton.core.drive.files.presentation.entry.SendFileEntry
import me.proton.core.drive.files.presentation.entry.ShareViaLinkEntry
import me.proton.core.drive.files.presentation.entry.StopSharingEntry
import me.proton.core.drive.files.presentation.entry.ToggleOfflineEntry
import me.proton.core.drive.files.presentation.entry.ToggleTrashEntry
import me.proton.core.drive.files.presentation.entry.TrashEntry
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.isSharedUrlExpired
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

sealed class Option(
    val applicableQuantity: ApplicableQuantity,
    val applicableTo: Set<ApplicableTo>,
    val applicableStates: Set<State>,
) {
    object CreateFolder : Option(
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

    object DeletePermanently : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FILE, ApplicableTo.FOLDER),
        setOf(State.TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToDelete: (linkId: LinkId) -> Unit,
        ) = DeletePermanentlyEntry { driveLink ->
            runAction { navigateToDelete(driveLink.id) }
        }
    }

    object Download : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FILE),
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

    object Info : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE, ApplicableTo.FOLDER),
        State.ANY_TRASHED + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToInfo: (linkId: LinkId) -> Unit,
        ) = FileInfoEntry { driveLink ->
            runAction { navigateToInfo(driveLink.id) }
        }
    }

    object Move : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FILE, ApplicableTo.FOLDER),
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

    object OfflineToggle : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE, ApplicableTo.FOLDER),
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

    object Rename : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE, ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            runAction: RunAction,
            navigateToRename: (linkId: LinkId) -> Unit,
        ) = RenameFileEntry { driveLink ->
            runAction { navigateToRename(driveLink.id) }
        }
    }

    object SendFile : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE),
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

    object TakeAPhoto : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            takeAPhoto: () -> Unit,
        ) = FolderEntry(
            icon = CorePresentation.drawable.ic_proton_camera,
            labelResId = I18N.string.folder_option_take_a_photo,
            onClick = { takeAPhoto() }
        )
    }

    object Trash : Option(
        ApplicableQuantity.All,
        setOf(ApplicableTo.FILE, ApplicableTo.FOLDER),
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

    object UploadFile : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED,
    ) {
        fun build(
            showFilePicker: () -> Unit,
        ) = FolderEntry(
            icon = CorePresentation.drawable.ic_proton_file_arrow_in_up,
            labelResId = I18N.string.folder_option_import_file,
            onClick = { showFilePicker() }
        )
    }

    object ShareViaLink : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE, ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED) + State.ANY_SHARED
    ) {
        fun build(
            runAction: RunAction,
            navigateToShareViaLink: (LinkId) -> Unit,
        ) = ShareViaLinkEntry { driveLink ->
            runAction { navigateToShareViaLink(driveLink.id) }
        }
    }

    object StopSharing : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE, ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED, State.SHARED, State.SHARED_EXPIRED),
    ) {
        fun build(
            runAction: RunAction,
            stopSharing: suspend (linkId: LinkId) -> Unit,
        ) = StopSharingEntry { driveLink ->
            runAction { stopSharing(driveLink.id) }
        }
    }

    object CopySharedLink : Option(
        ApplicableQuantity.Single,
        setOf(ApplicableTo.FILE, ApplicableTo.FOLDER),
        setOf(State.NOT_TRASHED, State.SHARED),
    ) {
        fun build(
            runAction: RunAction,
            copySharedLinkToClipboard: suspend (linkId: LinkId) -> Unit,
        ) = CopySharedLinkEntity { driveLink ->
            runAction { copySharedLinkToClipboard(driveLink.id) }
        }
    }
}

sealed class ApplicableQuantity(open val quantity: Long) {
    object Single : ApplicableQuantity(1L)
    object All : ApplicableQuantity(Long.MAX_VALUE)
}

enum class ApplicableTo {
    FILE,
    FOLDER,
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
        isShared && isSharedUrlExpired -> State.SHARED_EXPIRED
        isShared -> State.SHARED
        else -> State.NOT_SHARED
    },
)

fun DriveLink.isApplicableTo(applicableTo: Set<ApplicableTo>): Boolean = when (this) {
    is DriveLink.File -> applicableTo.contains(ApplicableTo.FILE)
    is DriveLink.Folder -> applicableTo.contains(ApplicableTo.FOLDER)
}

fun Set<Option>.filter(driveLink: DriveLink) =
    filter { option ->
        option.applicableStates.containsAll(driveLink.toOptionState()) && driveLink.isApplicableTo(option.applicableTo)
    }

private val photosOptions = listOf(
    Option.OfflineToggle,
    Option.ShareViaLink,
    Option.CopySharedLink,
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

fun Set<Option>.filterAll(driveLinks: List<DriveLink>) =
    filter { option ->
        driveLinks.size <= option.applicableQuantity.quantity &&
                driveLinks.all { driveLink ->
                    option.applicableStates.containsAll(driveLink.toOptionState()) &&
                            driveLink.isApplicableTo(option.applicableTo)
                }
    }
