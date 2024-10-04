/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.document.base.data.repository

import me.proton.core.drive.document.base.data.api.DocumentApiDataSource
import me.proton.core.drive.document.base.domain.entity.NewDocumentInfo
import me.proton.core.drive.document.base.domain.repository.DocumentRepository
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val api: DocumentApiDataSource,
) : DocumentRepository {

    override suspend fun createNewDocument(shareId: ShareId, newDocumentInfo: NewDocumentInfo): FileId =
        FileId(
            shareId = shareId,
            id = api.createDocument(shareId, newDocumentInfo).document.linkId,
        )
}
