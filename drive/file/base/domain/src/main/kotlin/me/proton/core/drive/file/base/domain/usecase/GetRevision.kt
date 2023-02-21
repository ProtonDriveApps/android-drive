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
package me.proton.core.drive.file.base.domain.usecase

import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.file.base.domain.entity.Revision
import me.proton.core.drive.file.base.domain.repository.FileRepository
import me.proton.core.drive.link.domain.entity.FileId
import javax.inject.Inject

class GetRevision @Inject constructor(
    private val fileRepository: FileRepository,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(
        fileId: FileId,
        revisionId: String,
    ): Result<Revision> = coRunCatching {
        val blocks = mutableListOf<Block>()
        var revision: Revision
        do {
            revision = fileRepository.fetchRevision(
                fileId = fileId,
                revisionId = revisionId,
                fromBlockIndex = blocks.size + 1,
                pageSize = configurationProvider.uiPageSize,
            )
            blocks.addAll(revision.blocks)
        } while (revision.blocks.size == configurationProvider.uiPageSize)
        revision.copy(
            blocks = blocks
        )
    }
}
