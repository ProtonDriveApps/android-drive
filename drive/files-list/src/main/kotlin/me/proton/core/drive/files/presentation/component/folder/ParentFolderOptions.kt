/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.files.presentation.component.folder

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.component.bottomsheet.BottomSheetContent
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrong
import me.proton.core.drive.base.presentation.component.BottomSheetEntry
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.i18n.R as I18N

@Composable
fun ParentFolderOptions(
    folder: DriveLink.Folder,
    entries: List<FileOptionEntry<DriveLink.Folder>>,
    modifier: Modifier = Modifier,
) {
    BottomSheetContent(
        modifier = modifier,
        header = {
            val title = if (folder.isNameEncrypted) {
                stringResource(id = I18N.string.folder_options_header_title_encrypted)
            } else {
                stringResource(id = I18N.string.folder_options_header_title, folder.name)
            }
            ParentFolderOptionsHeader(
                title = title,
                isTitleEncrypted = folder.isNameEncrypted
            )
        },
        content = {
            entries.forEach { entry ->
                when (entry) {
                    is FileOptionEntry.SimpleEntry ->
                        BottomSheetEntry(
                            leadingIcon = entry.icon,
                            trailingIcon = entry.trailingIcon,
                            trailingIconTintColor = entry.trailingIconTintColor,
                            notificationDotVisible = entry.notificationDotVisible,
                            title = entry.getLabel(),
                            onClick = { entry.onClick(folder) },
                        )
                    else -> throw NotImplementedError("Action for ${entry.javaClass.simpleName} is missing")
                }
            }
        },
    )
}

@Composable
internal fun ParentFolderOptionsHeader(
    title: String,
    isTitleEncrypted: Boolean,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = isTitleEncrypted,
        modifier = modifier,
    ) { isEncrypted ->
        if (isEncrypted) {
            Row {
                Text(text = title, Modifier.padding(end = ProtonDimens.SmallSpacing))
                EncryptedItem()
            }
        } else {
            TextWithMiddleEllipsis(
                text = title,
                style = ProtonTheme.typography.defaultSmallStrong,
                maxLines = 1,
            )
        }
    }
}
