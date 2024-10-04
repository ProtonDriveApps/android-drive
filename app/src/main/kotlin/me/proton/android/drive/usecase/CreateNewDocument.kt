/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.usecase

import androidx.datastore.preferences.core.edit
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.document.create.domain.usecase.CreateNewDocument
import me.proton.core.drive.document.create.presentation.usecase.NewDocumentName
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject

class CreateNewDocument @Inject constructor(
    private val createNewDocument: CreateNewDocument,
    private val getUserDataStore: GetUserDataStore,
    private val newDocumentName: NewDocumentName,
) {
    suspend operator fun invoke(
        parentFolderId: FolderId,
        name: String
    ) = coRunCatching {
        createNewDocument(
            parentFolderId = parentFolderId,
            name = name,
        ).getOrThrow().also {
            getUserDataStore(parentFolderId.userId)
                .edit { preferences ->
                    preferences[GetUserDataStore.Keys.createDocumentActionInvoked] = true
                }
        }
    }


    suspend operator fun invoke(
        parentFolderId: FolderId,
    ) = coRunCatching {
        invoke(
            parentFolderId = parentFolderId,
            name = newDocumentName(),
        ).getOrThrow()
    }
}
