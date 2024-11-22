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
package me.proton.core.drive.linkdownload.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.extension.revisionId
import me.proton.core.drive.linkdownload.domain.repository.LinkDownloadRepository
import javax.inject.Inject

class SetDownloadState @Inject constructor(
    private val linkDownloadRepository: LinkDownloadRepository,
    private val getLink: GetLink,
) {
    suspend operator fun invoke(
        link: Link,
        downloadState: DownloadState,
        blocks: List<Block>? = null,
    ) =
        linkDownloadRepository.insertOrUpdateDownloadState(
            linkId = link.id,
            revisionId = link.revisionId,
            downloadState = downloadState,
            blocks = blocks,
        )

    suspend operator fun invoke(
        linkId: LinkId,
        downloadState: DownloadState,
        blocks: List<Block>? = null,
    ): Result<Unit> = coRunCatching {
            invoke(
                link = getLink(linkId).toResult().getOrThrow(),
                downloadState = downloadState,
                blocks = blocks,
            )
        }
}
