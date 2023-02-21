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

package me.proton.core.drive.drivelink.domain.entity

import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.extension.isProcessing
import me.proton.core.drive.volume.domain.entity.VolumeId

sealed class DriveLink : BaseLink {
    internal abstract val link: Link
    abstract val volumeId: VolumeId
    abstract val cryptoName: CryptoProperty<String>
    abstract val cryptoXAttr: CryptoProperty<String?>
    abstract val isMarkedAsOffline: Boolean
    abstract val isAnyAncestorMarkedAsOffline: Boolean
    abstract val downloadState: DownloadState?
    abstract val trashState: TrashState?
    abstract val nameHash: String

    val isTrashed: Boolean
        get() = trashState != null && trashState != TrashState.TRASHING

    val isProcessing: Boolean
        get() = trashState?.isProcessing == true


    data class File(
        override val link: Link.File,
        override val volumeId: VolumeId,
        override val isMarkedAsOffline: Boolean,
        override val isAnyAncestorMarkedAsOffline: Boolean,
        override val downloadState: DownloadState?,
        override val trashState: TrashState?,
        override val cryptoName: CryptoProperty<String> = CryptoProperty.Encrypted(link.name),
        override val cryptoXAttr: CryptoProperty<String?> = CryptoProperty.Encrypted(link.xAttr)
    ) : DriveLink(), me.proton.core.drive.link.domain.entity.File by link {
        override val name: String
            get() = cryptoName.value
        override val nameHash: String
            get() = link.hash
    }

    data class Folder(
        override val link: Link.Folder,
        override val volumeId: VolumeId,
        override val isMarkedAsOffline: Boolean,
        override val isAnyAncestorMarkedAsOffline: Boolean,
        override val downloadState: DownloadState?,
        override val trashState: TrashState?,
        override val cryptoName: CryptoProperty<String> = CryptoProperty.Encrypted(link.name),
        override val cryptoXAttr: CryptoProperty<String?> = CryptoProperty.Encrypted(link.xAttr)
    ) : DriveLink(), me.proton.core.drive.link.domain.entity.Folder by link {
        override val name: String
            get() = cryptoName.value
        override val signatureAddress: String
            get() = link.signatureAddress
        override val nameHash: String
            get() = link.hash
    }
}
