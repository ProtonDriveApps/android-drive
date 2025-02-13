/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.files.presentation.extension

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.hasShareLink
import me.proton.core.drive.drivelink.domain.extension.isSharedWithUsers
import me.proton.core.drive.linkdownload.domain.entity.DownloadState

fun Modifier.driveLinkSemantics(link: DriveLink, linkLayoutType: LayoutType) =
    semantics(mergeDescendants = true) {
        this[DriveLinkSemanticsProperties.LinkName] = link.name
        this[DriveLinkSemanticsProperties.LayoutType] = linkLayoutType
        this[DriveLinkSemanticsProperties.ItemType] = ItemType.fromDriveLink(link)
        this[DriveLinkSemanticsProperties.HasThumbnail] =
            link is DriveLink.File && link.hasThumbnail
        this[DriveLinkSemanticsProperties.IsSharedByLink] = link.hasShareLink
        this[DriveLinkSemanticsProperties.IsSharedWithUsers] = link.isSharedWithUsers
        this[DriveLinkSemanticsProperties.DownloadState] = when (link.downloadState) {
            is DownloadState.Downloaded -> SemanticsDownloadState.Downloaded
            DownloadState.Downloading -> SemanticsDownloadState.Downloading
            DownloadState.Error -> SemanticsDownloadState.Error
            null -> SemanticsDownloadState.Null
        }
    }

object DriveLinkSemanticsProperties {
    val LinkName = SemanticsPropertyKey<String>(name = "LinkName")
    val LayoutType = SemanticsPropertyKey<LayoutType>(name = "LayoutType")
    val ItemType = SemanticsPropertyKey<ItemType>(name = "ItemType")
    val HasThumbnail = SemanticsPropertyKey<Boolean>(name = "HasThumbnail")
    val IsSharedByLink = SemanticsPropertyKey<Boolean>(name = "IsSharedByLink")
    val IsSharedWithUsers = SemanticsPropertyKey<Boolean>(name = "IsSharedWithUsers")
    val DownloadState = SemanticsPropertyKey<SemanticsDownloadState>(name = "DownloadState")
}

enum class ItemType {
    Folder,
    File,
    Album;

    companion object {
        fun fromDriveLink(link: DriveLink): ItemType = when (link) {
            is DriveLink.Folder -> Folder
            is DriveLink.File -> File
            is DriveLink.Album -> Album
        }
    }
}

enum class LayoutType {
    List,
    Grid,
}

enum class SemanticsDownloadState {
    Downloading,
    Error,
    Downloaded,
    Null,
}
