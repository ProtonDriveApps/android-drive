/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.documentsprovider.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.documentsprovider.data.DriveDocumentsProvider
import me.proton.core.drive.documentsprovider.data.DriveFileProvider
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.documentsprovider.domain.repository.DocumentsProviderRepository
import me.proton.core.drive.link.domain.entity.FileId
import javax.inject.Inject

class DocumentsProviderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
): DocumentsProviderRepository {

    override fun getDocumentsUri(documentId: DocumentId): Uri =
        DriveDocumentsProvider.getUri(context, documentId)

    override fun getFileUri(documentId: DocumentId): Uri =
        DriveFileProvider.getUri(context, documentId.userId, documentId.linkId as FileId)
}
