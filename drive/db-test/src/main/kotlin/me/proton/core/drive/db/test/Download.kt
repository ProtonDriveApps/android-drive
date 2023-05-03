/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.db.test

import me.proton.core.drive.linkdownload.data.db.entity.DownloadBlockEntity
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadState
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadStateEntity

data class DownloadContext(val fileContext: FileContext) : BaseContext()

suspend fun FileContext.download(
    state: LinkDownloadState = LinkDownloadState.DOWNLOADED,
    block: suspend DownloadContext.() -> Unit = {}
) {
    db.linkDownloadDao.insertOrIgnore(
        LinkDownloadStateEntity(
            userId = user.userId,
            shareId = share.id,
            linkId = link.id,
            revisionId = revisionId,
            state = state,
            null,
            null
        )
    )
    DownloadContext(this).block()
}

suspend fun DownloadContext.block(index: Long) {
    with(fileContext) {
        db.linkDownloadDao.insertOrIgnore(
            DownloadBlockEntity(
                userId = user.userId,
                shareId = share.id,
                linkId = link.id,
                revisionId = revisionId,
                index = index,
                uri = "",
                encryptedSignature = null
            )
        )
    }
}
