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

package me.proton.core.drive.documentsprovider.data.extension

import android.content.Context
import android.database.MatrixCursor
import android.provider.DocumentsContract
import androidx.annotation.DrawableRes
import me.proton.core.account.domain.entity.Account
import me.proton.core.drive.documentsprovider.data.R
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId

internal fun Account.addTo(context: Context, cursor: MatrixCursor.RowBuilder) {
    cursor.add(DocumentsContract.Root.COLUMN_ROOT_ID, userId.id)
    cursor.add(DocumentsContract.Root.COLUMN_ICON, context.icon)
    cursor.add(DocumentsContract.Root.COLUMN_TITLE, context.applicationName)
    cursor.add(DocumentsContract.Root.COLUMN_SUMMARY, email)
    cursor.add(
        DocumentsContract.Root.COLUMN_FLAGS,
        DocumentsContract.Root.FLAG_SUPPORTS_CREATE,
    )
    cursor.add(
        DocumentsContract.Root.COLUMN_DOCUMENT_ID,
        DocumentId(userId, null).encode()
    )
}

@get:DrawableRes
private val Context.icon: Int
    get() = metaData.getInt("drive.documentsprovider.icon", 0).let { icon ->
        if (icon == 0) {
            R.drawable.ic_drive_documents_provider_icon
        } else {
            icon
        }
    }
