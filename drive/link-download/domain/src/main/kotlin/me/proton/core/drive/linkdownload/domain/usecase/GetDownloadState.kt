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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.extension.transformSuccess
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.extension.revisionId
import me.proton.core.drive.linkdownload.domain.repository.LinkDownloadRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetDownloadState @Inject constructor(
    private val linkDownloadRepository: LinkDownloadRepository,
    private val getLink: GetLink,
) {
    operator fun invoke(link: Link): Flow<DataResult<DownloadState?>> =
        linkDownloadRepository.getDownloadStateFlow(
            linkId = link.id,
            revisionId = link.revisionId
        )

    operator fun invoke(linkId: LinkId): Flow<DataResult<DownloadState?>> =
        getLink(linkId).transformSuccess { result ->
            emitAll(invoke(result.value))
        }
}
