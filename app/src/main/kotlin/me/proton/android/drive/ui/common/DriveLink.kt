/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.android.drive.ui.common

import kotlinx.coroutines.flow.StateFlow
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.isProtonDocument

suspend fun DriveLink.onClick(
    navigateToFolder: (FolderId, String?) -> Unit,
    navigateToPreview: (FileId) -> Unit,
    openDocument: suspend (DriveLink.File) -> Unit,
    openProtonDocsInWebView: StateFlow<Boolean>,
) = when (this) {
    is DriveLink.Folder -> navigateToFolder(id, if (isNameEncrypted) null else name)
    is DriveLink.File -> when {
        this.isProtonDocument ->
            if (openProtonDocsInWebView.value) {
                navigateToPreview(id)
            } else {
                openDocument(this)
            }
        else -> navigateToPreview(id)
    }
}
