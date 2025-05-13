/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import me.proton.android.drive.photos.presentation.extension.details
import me.proton.android.drive.ui.viewmodel.AlbumOptionsViewModel
import me.proton.core.compose.component.bottomsheet.BottomSheetContent
import me.proton.core.drive.base.presentation.component.BottomSheetEntry
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.files.presentation.component.common.OptionsHeader
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailPainter
import me.proton.core.drive.base.presentation.R as BasePresentation

@Composable
fun AlbumOptions(
    runAction: RunAction,
    navigateToShareViaInvitations: (linkId: LinkId) -> Unit,
    navigateToManageAccess: (linkId: LinkId) -> Unit,
    navigateToRename: (linkId: LinkId) -> Unit,
    navigateToDelete: (AlbumId) -> Unit,
    navigateToLeave: (AlbumId) -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<AlbumOptionsViewModel>()
    val driveLink by viewModel.driveLink.collectAsStateWithLifecycle(initialValue = null)
    val coverLink by viewModel.coverLink.collectAsStateWithLifecycle(initialValue = null)
    val album = driveLink ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val entries by remember(viewModel, lifecycle) {
        viewModel.entries(
            runAction = runAction,
            navigateToShareViaInvitations = navigateToShareViaInvitations,
            navigateToManageAccess = navigateToManageAccess,
            navigateToRename = navigateToRename,
            navigateToDelete = navigateToDelete,
            navigateToLeave = navigateToLeave,
            dismiss = dismiss,
        ).flowWithLifecycle(
            lifecycle = lifecycle,
            minActiveState = Lifecycle.State.STARTED
        )
    }.collectAsState(initial = null)
    val albumEntries = entries ?: return
    AlbumOptions(
        album = album,
        coverLink = coverLink,
        entries = albumEntries,
        modifier = modifier
            .navigationBarsPadding()
            .testTag(AlbumOptionsTestTag.albumOptions),
    )
}

@Composable
fun AlbumOptions(
    album: DriveLink.Album,
    coverLink: DriveLink.File?,
    entries: List<FileOptionEntry<DriveLink.Album>>,
    modifier: Modifier = Modifier,
) {
    BottomSheetContent(
        modifier = modifier,
        header = {
            AlbumOptionsHeader(
                album = album,
                coverLink = coverLink,
            )
        },
        content = {
            entries.forEach { entry ->
                when (entry) {
                    is FileOptionEntry.SimpleEntry -> BottomSheetEntry(
                        leadingIcon = entry.icon,
                        title = entry.getLabel(),
                        onClick = { entry.onClick(album) },
                        trailingIcon = entry.trailingIcon,
                        trailingIconTintColor = entry.trailingIconTintColor,
                        notificationDotVisible = entry.notificationDotVisible,
                    )
                    is FileOptionEntry.StateBasedEntry -> BottomSheetEntry(
                        leadingIcon = entry.getIcon(album),
                        title = entry.getLabel(album),
                        onClick = { entry.onClick(album) },
                        trailingIcon = entry.trailingIcon,
                        trailingIconTintColor = entry.trailingIconTintColor,
                        notificationDotVisible = entry.notificationDotVisible,
                    )
                }
            }
        }
    )
}

@Composable
fun AlbumOptionsHeader(
    album: DriveLink.Album,
    coverLink: DriveLink.File?,
    modifier: Modifier = Modifier,
) {
    val coverPainter = coverLink?.thumbnailPainter()?.painter
    val localContext = LocalContext.current
    val details = remember (localContext) {
        album.details(localContext, useCreationTime = false)
    }
    if (coverPainter != null) {
        OptionsHeader(
            painter = coverPainter,
            title = album.name,
            isTitleEncrypted = album.isNameEncrypted,
            subtitle = details,
            modifier = modifier,
        )
    } else {
        OptionsHeader(
            title = album.name,
            isTitleEncrypted = album.isNameEncrypted,
            subtitle = details,
            modifier = modifier,
        ) { contentModifier ->
            Image(
                painter = painterResource(id = BasePresentation.drawable.ic_folder_album),
                contentDescription = null,
                modifier = contentModifier,
            )
        }
    }
}

object AlbumOptionsTestTag {
    const val albumOptions = "album options context menu"
}
