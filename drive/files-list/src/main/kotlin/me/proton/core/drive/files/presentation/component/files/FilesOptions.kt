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
package me.proton.core.drive.files.presentation.component.files

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import me.proton.core.compose.component.bottomsheet.BottomSheetContent
import me.proton.core.compose.component.bottomsheet.BottomSheetEntry
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.files.presentation.component.common.OptionsHeader
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.link.presentation.extension.getSize
import me.proton.core.drive.link.presentation.extension.lastModifiedRelative
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailPainter

@Composable
fun FilesOptions(
    file: DriveLink.File,
    entries: List<FileOptionEntry<DriveLink.File>>,
    modifier: Modifier = Modifier,
) {
    BottomSheetContent(
        modifier = modifier,
        header = {
            FilesOptionsHeader(file)
        },
        content = {
            entries.forEach { entry ->
                when (entry) {
                    is FileOptionEntry.SimpleEntry -> BottomSheetEntry(
                        icon = entry.icon,
                        title = entry.getLabel(),
                        onClick = { entry.onClick(file) }
                    )
                    is FileOptionEntry.StateBasedEntry -> BottomSheetEntry(
                        icon = entry.getIcon(file),
                        title = entry.getLabel(file),
                        onClick = { entry.onClick(file) }
                    )
                }
            }
        }
    )
}

@Composable
internal fun FilesOptionsHeader(
    file: DriveLink.File,
    modifier: Modifier = Modifier,
) {
    OptionsHeader(
        painter = file.thumbnailPainter().painter,
        title = file.name,
        isTitleEncrypted = file.isNameEncrypted,
        subtitle = "%s | %s".format(
            file.getSize(LocalContext.current),
            file.lastModifiedRelative(LocalContext.current)
        ),
        modifier = modifier
    )
}
