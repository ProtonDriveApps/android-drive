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
package me.proton.core.drive.documentsprovider.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.documentsprovider.domain.usecase.OpenDocument
import me.proton.core.drive.documentsprovider.domain.usecase.WithDriveLinkFile
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.util.kotlin.CoreLogger

class DriveFileProvider : ContentProvider() {

    private val injections by lazy(LazyThreadSafetyMode.NONE) {
        EntryPointAccessors.fromApplication<HiltEntryPoint>(requireNotNull(context))
    }

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = runBlocking {
        injections.withDriveLinkFile(uri.toDocumentId()) { _, driveLink ->
            CoreLogger.d(LogTag.DOCUMENTS_PROVIDER, "Querying info for: $uri")
            val cols = arrayOfNulls<String>(COLUMNS.size)
            val values = arrayOfNulls<Any>(COLUMNS.size)
            cols[0] = OpenableColumns.DISPLAY_NAME
            values[0] = driveLink.name
            cols[1] = OpenableColumns.SIZE
            values[1] = driveLink.size.value
            MatrixCursor(cols, 1).apply { addRow(values) }
        }
    }

    override fun getType(uri: Uri) = runBlocking {
        injections.withDriveLinkFile(uri.toDocumentId()) { _, driveLink ->
            CoreLogger.d(LogTag.DOCUMENTS_PROVIDER, "mime type is ${driveLink.mimeType} for: $uri")
            driveLink.mimeType
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor = runBlocking {
        injections.openDocument(uri.toDocumentId(), mode, null)
    }

    companion object {
        private val COLUMNS = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)

        private val Context.AUTHORITY get() = "$packageName.files"

        fun getUri(context: Context, userId: UserId, fileId: FileId): Uri =
            Uri.parse("""content://${context.AUTHORITY}/${userId.id}/${fileId.shareId.id}/${fileId.id}""")

        private fun Uri.toDocumentId() =
            DocumentId(UserId(pathSegments[0]), FileId(ShareId(UserId(pathSegments[0]), pathSegments[1]), pathSegments[2]))

    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltEntryPoint {
        val withDriveLinkFile: WithDriveLinkFile
        val openDocument: OpenDocument
    }
}
