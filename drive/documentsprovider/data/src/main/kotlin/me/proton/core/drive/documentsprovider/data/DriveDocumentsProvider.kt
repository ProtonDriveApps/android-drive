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

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.runBlocking
import me.proton.core.drive.documentsprovider.data.extension.addTo
import me.proton.core.drive.documentsprovider.data.extension.asCursor
import me.proton.core.drive.documentsprovider.data.extension.encode
import me.proton.core.drive.documentsprovider.data.extension.toDocumentId
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.documentsprovider.domain.usecase.CreateDocument
import me.proton.core.drive.documentsprovider.domain.usecase.GetDocumentThumbnail
import me.proton.core.drive.documentsprovider.domain.usecase.GetDocumentsProviderRoots
import me.proton.core.drive.documentsprovider.domain.usecase.OpenDocument
import me.proton.core.drive.documentsprovider.domain.usecase.WithDriveLink
import me.proton.core.drive.documentsprovider.domain.usecase.WithDriveLinkFolder
import me.proton.core.drive.documentsprovider.domain.usecase.WithUploadFileLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.list.domain.usecase.GetPagedDriveLinksList
import me.proton.core.drive.drivelink.rename.domain.usecase.RenameLink
import me.proton.core.drive.trash.domain.usecase.SendToTrash
import me.proton.core.drive.upload.domain.usecase.CancelUploadFile
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

class DriveDocumentsProvider : DocumentsProvider() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val injections by lazy(LazyThreadSafetyMode.NONE) {
        EntryPointAccessors.fromApplication<HiltEntryPoint>(requireNotNull(context))
    }

    /**
     * [android.content.ContentProvider]s open and close [Cursor]s to get their information. We need to keep track of
     * the data which was requested before in order to provide the same [PagingData] flow when the provider is just
     * requesting a new [Cursor] because it was notified the data changed.
     */
    private val trigger = MutableStateFlow<String?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val children: Flow<PagingData<DriveLink>> = trigger.transformLatest { parentDocumentId ->
        emit(PagingData.empty())
        if (parentDocumentId != null) {
            emitAll(getQueryChildDocumentsFlow(parentDocumentId).cachedIn(scope))
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed(TimeUnit.MINUTES.toMillis(5)), 1)

    private fun getQueryChildDocumentsFlow(documentId: String) = runBlocking {
        injections.withDriveLinkFolder(documentId.toDocumentId()) { _, driveLink ->
            injections.getDecryptedDriveLinks(driveLink.id)
        }
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor = runBlocking {
        MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            val context = requireNotNull(context)
            injections.getDocumentsProviderRoots().forEach { account ->
                account.addTo(context, newRow())
            }
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor = runBlocking {
        injections.withUploadFileLink(documentId.toDocumentId()) { _, uploadFileLink ->
            MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
                uploadFileLink.addTo(newRow())
            }
        } ?:
        injections.withDriveLink(documentId.toDocumentId()) { _, driveLink ->
            MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
                driveLink.addTo(newRow())
            }
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor = runBlocking {
        parentDocumentId ?: throw FileNotFoundException("parentDocumentId should not be null")
        val context = requireNotNull(context)
        trigger.value = parentDocumentId
        children.asCursor(
            context = context,
            uri = DocumentsContract.buildChildDocumentsUri(context.AUTHORITY, parentDocumentId),
            projection = projection ?: DEFAULT_DOCUMENT_PROJECTION,
        ) { driveLink ->
            driveLink.addTo(this)
        }
    }

    override fun createDocument(parentDocumentId: String?, mimeType: String?, displayName: String?): String = runBlocking {
        injections.createDocument(parentDocumentId.toDocumentId(), mimeType, displayName).encode()
    }

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor = runBlocking {
        injections.openDocument(documentId.toDocumentId(), mode, signal)
    }

    override fun openDocumentThumbnail(
        documentId: String?,
        sizeHint: Point?,
        signal: CancellationSignal?,
    ): AssetFileDescriptor = runBlocking {
        injections.getDocumentThumbnail(documentId.toDocumentId(), signal)
    }

    override fun renameDocument(documentId: String?, displayName: String?): String? = runBlocking {
        injections.withDriveLink(documentId.toDocumentId()) { _, driveLink ->
            requireNotNull(displayName) { "displayName cannot be null" }
            injections.rename(driveLink.id, displayName).getOrThrow()
            null // DocumentId has not changed
        }
    }

    override fun deleteDocument(documentId: String?) = runBlocking {
        injections.withUploadFileLink(documentId.toDocumentId()) { _, uploadFileNode ->
            injections.cancelUploadFile(uploadFileNode)
        } ?: injections.withDriveLink(documentId.toDocumentId()) { userId, driveLink ->
            injections.sendToTrash(userId, driveLink)
        }
    }

    companion object {
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
        )

        private val Context.AUTHORITY get() = "$packageName.documents"

        /**
         * Needs to be called on logins and logouts
         */
        fun notifyRootsHaveChanged(context: Context) {
            val rootsUri = DocumentsContract.buildRootsUri(context.AUTHORITY)
            context.contentResolver.notifyChange(rootsUri, null)
        }

        fun getUri(context: Context, documentId: DocumentId): Uri =
            DocumentsContract.buildDocumentUri(context.AUTHORITY, documentId.encode())
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltEntryPoint {
        // region With...
        val withUploadFileLink: WithUploadFileLink
        val withDriveLink: WithDriveLink
        val withDriveLinkFolder: WithDriveLinkFolder
        // endregion
        val getDocumentsProviderRoots: GetDocumentsProviderRoots
        val createDocument: CreateDocument
        val openDocument: OpenDocument
        val getDocumentThumbnail: GetDocumentThumbnail
        val getDecryptedDriveLinks: GetPagedDriveLinksList
        val sendToTrash: SendToTrash
        val rename: RenameLink
        val cancelUploadFile: CancelUploadFile
    }
}
