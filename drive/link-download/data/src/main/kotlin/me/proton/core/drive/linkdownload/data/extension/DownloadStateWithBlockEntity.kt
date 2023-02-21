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

package me.proton.core.drive.linkdownload.data.extension

import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadStateWithBlockEntity
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadState
import me.proton.core.drive.linkdownload.domain.entity.DownloadState

fun List<LinkDownloadStateWithBlockEntity>.toDownloadState() = if (isEmpty()) {
    null
} else when (first().downloadStateEntity.state) {
    LinkDownloadState.DOWNLOADING -> DownloadState.Downloading
    LinkDownloadState.ERROR -> DownloadState.Error
    LinkDownloadState.DOWNLOADED -> DownloadState.Downloaded(
        blocks = mapNotNull { downloadStateWithBlock -> downloadStateWithBlock.toBlock() },
        manifestSignature = first().downloadStateEntity.manifestSignature,
        signatureAddress = first().downloadStateEntity.signatureAddress,
    )
}

fun LinkDownloadStateWithBlockEntity.toBlock() = if (index == null || uri == null) {
    null
} else {
    Block(
        index = index,
        url = uri,
        hashSha256 = null,
        encSignature = encryptedSignature,
    )
}
