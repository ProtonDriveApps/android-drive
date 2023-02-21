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
package me.proton.core.drive.drivelink.download.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.Revision
import me.proton.core.drive.file.base.domain.usecase.GetRevision
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import javax.inject.Inject

class SetDownloadingAndGetRevision @Inject constructor(
    private val getRevision: GetRevision,
    private val setDownloadState: SetDownloadState,
) {
    suspend operator fun invoke(
        fileId: FileId, revisionId: String
    ): Result<Revision> = coRunCatching {
        setDownloadState(fileId, DownloadState.Downloading)
        getRevision(fileId, revisionId).getOrThrow()
    }
}
