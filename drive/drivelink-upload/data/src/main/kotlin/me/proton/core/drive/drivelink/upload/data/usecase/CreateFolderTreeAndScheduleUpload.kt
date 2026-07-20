/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.upload.data.usecase

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.data.extension.detailsOrNull
import me.proton.core.drive.base.data.extension.last
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.onProtonHttpException
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.upload.data.provider.DocumentFileProvider
import me.proton.core.drive.folder.create.domain.usecase.CreateFolder
import me.proton.core.drive.link.data.api.response.AlreadyExistsErrorResponseDetails
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadFileProperties
import me.proton.core.drive.linkupload.domain.usecase.CreateUploadBulk
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.trash.domain.usecase.SendToTrash
import me.proton.core.drive.upload.domain.exception.NotEnoughSpaceException
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import me.proton.core.drive.upload.domain.usecase.HasEnoughAvailableSpace
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class CreateFolderTreeAndScheduleUpload @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val documentFileProvider: DocumentFileProvider,
    private val createFolder: CreateFolder,
    private val hasEnoughAvailableSpace: HasEnoughAvailableSpace,
    private val sendToTrash: SendToTrash,
    private val uploadWorkManager: UploadWorkManager,
    private val createUploadBulk: CreateUploadBulk,
    private val getLink: GetLink,
    private val broadcastMessages: BroadcastMessages,
    private val announceEvent: AnnounceEvent,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(
        folder: DriveLink.Folder,
        uriString: String,
        shouldBroadcastMessage: Boolean,
    ): Result<Unit> = coRunCatching {
        CoreLogger.d(LogTag.UPLOAD, "Start processing upload folder from uri: $uriString")
        val rootFolder = requireNotNull(
            documentFileProvider.fromTreeUri(appContext, uriString.toUri())
        ) {
            "Failed creating root document file for uri: $uriString"
        }
        if (shouldBroadcastMessage) {
            broadcastMessages(
                userId = folder.id.userId,
                message = appContext.getString(I18N.string.files_upload_preparing),
                type = BroadcastMessage.Type.INFO,
            )
        }
        checkAvailableSpace(folder.id.userId, rootFolder)
        createFoldersAndUploadBulks(
            volumeId = folder.volumeId,
            folderId = folder.id,
            root = rootFolder,
            shouldBroadcastMessage = shouldBroadcastMessage,
        )
            .getOrThrow()
            .forEach { uploadBulk ->
                CoreLogger.d(LogTag.UPLOAD, "Scheduling upload bulk for parentId=${uploadBulk.parentLinkId.id.logId()}")
                uploadWorkManager.upload(
                    uploadBulk = uploadBulk,
                    folder = folder,
                    showPreparingUpload = false,
                    showFilesBeingUploaded = false,
                )
            }
        CoreLogger.i(LogTag.UPLOAD, "Finished processing upload folder from uri: $uriString")
    }

    private suspend fun checkAvailableSpace(userId: UserId, root: DocumentFile) {
        val uploadFileDescriptions = getAllFileDescendants(root)
            .map { documentFile ->
                UploadFileDescription(
                    uri = documentFile.uri.toString(),
                    properties = UploadFileProperties(
                        name = documentFile.name.orEmpty(),
                        mimeType = documentFile.type.orEmpty(),
                        size = documentFile.length().bytes,
                        lastModified = TimestampMs(documentFile.lastModified())
                    )
                )
            }
        val requiredSpace = uploadFileDescriptions
                .mapNotNull { uploadFileDescription -> uploadFileDescription.properties?.size }
                .sumOf { size -> size.value }
        CoreLogger.d(LogTag.UPLOAD, "Upload folder required space: $requiredSpace bytes")
        val hasEnoughSpace = hasEnoughAvailableSpace(
            userId = userId,
            uploadFileDescriptions = uploadFileDescriptions,
            onNotEnough = { needed ->
                announceEvent(
                    userId = userId,
                    event = Event.StorageFull(needed)
                )
            }
        )
        if (!hasEnoughSpace) {
            throw NotEnoughSpaceException("Not enough available space to schedule upload folder")
        }
    }

    private suspend fun createFoldersAndUploadBulks(
        volumeId: VolumeId,
        folderId: FolderId,
        root: DocumentFile,
        shouldBroadcastMessage: Boolean,
    ): Result<List<UploadBulk>> = withContext(Dispatchers.IO) {
        coRunCatching {
            val rootFolderId = getOrCreateFolder(
                parentFolderId = folderId,
                folderName = root.nameOrUnnamed,
                shouldUpdateEvent = true,
            ).getOrThrow()
            coRunCatching {
                val uploadBulks = mutableListOf<UploadBulk>()
                var createdFolders = 1
                val stack = ArrayDeque<Pair<DocumentFile, FolderId>>()
                stack.addLast(root to rootFolderId)
                while (stack.isNotEmpty()) {
                    val (current, parentFolderId) = stack.removeLast()
                    val documentFiles = current.listFiles()
                    val mutex = Mutex()
                    documentFiles
                        .filter { documentFile -> documentFile.isDirectory }
                        .chunked(configurationProvider.createFolderInParallel)
                        .flatMap { chunk ->
                            chunk.map { documentFile ->
                                async {
                                    val subFolderId = getOrCreateFolder(
                                        parentFolderId = parentFolderId,
                                        folderName = documentFile.nameOrUnnamed,
                                        shouldUpdateEvent = false,
                                    ).getOrThrow()
                                    mutex.withLock {
                                        stack.addLast(documentFile to subFolderId).also { createdFolders++ }
                                    }
                                }
                            }.awaitAll()
                        }
                    documentFiles
                        .filter { documentFile -> documentFile.isDirectory.not() }
                        .takeIfNotEmpty()
                        ?.let { files ->
                            uploadBulks.add(
                                createUploadBulk(
                                    volumeId = volumeId,
                                    parent = getLink(parentFolderId).toResult().getOrThrow(),
                                    uploadFileDescriptions = files
                                        .map { documentFile ->
                                            UploadFileDescription(
                                                uri = documentFile.uri.toString(),
                                                properties = UploadFileProperties(
                                                    name = documentFile.nameOrUnnamed,
                                                    mimeType = documentFile.type.orEmpty(),
                                                    size = documentFile.length().bytes,
                                                    lastModified = TimestampMs(documentFile.lastModified()),
                                                )
                                            )
                                        },
                                    cacheOption = CacheOption.THUMBNAIL_DEFAULT,
                                    shouldDeleteSource = false,
                                    networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
                                    shouldAnnounceEvent = true,
                                    priority = UploadFileLink.USER_PRIORITY,
                                    shouldBroadcastErrorMessage = true,
                                ).getOrThrow()
                            )
                        }
                }
                val numberOfFiles = uploadBulks.flatMap { uploadBulk ->
                    uploadBulk.uploadFileDescriptions
                }.count()
                CoreLogger.d(
                    LogTag.UPLOAD,
                    "Created $createdFolders folder(s) and ${uploadBulks.size} upload bulk(s) with totally $numberOfFiles file(s)",
                )
                if (shouldBroadcastMessage) {
                    broadcastMessages(
                        userId = folderId.userId,
                        message = buildString {
                            append(
                                appContext.resources.getQuantityString(
                                    I18N.plurals.folder_upload_created_subfolders,
                                    createdFolders,
                                    createdFolders,
                                )
                            )
                            if (numberOfFiles > 0) {
                                appendLine()
                                append(
                                    appContext.resources.getQuantityString(
                                        I18N.plurals.folder_upload_scheduled_files_for_upload,
                                        numberOfFiles,
                                        numberOfFiles,
                                    )
                                )
                            }
                        },
                        type = BroadcastMessage.Type.INFO,
                    )
                }
                uploadBulks
            }.onFailure {
                deleteRootFolder(volumeId, rootFolderId).getOrNull(
                    tag = LogTag.UPLOAD,
                    message = "Failed to delete root upload folder"
                )
            }.getOrThrow()
        }
    }

    private fun getAllFileDescendants(root: DocumentFile): List<DocumentFile> {
        val result = mutableListOf(root)
        val stack = ArrayDeque<DocumentFile>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            current
                .listFiles()
                .forEach { documentFile ->
                    if (documentFile.isDirectory) {
                        stack.addLast(documentFile)
                    } else {
                        result.add(documentFile)
                    }
                }
        }
        return result
    }

    private suspend fun deleteRootFolder(volumeId: VolumeId, folderId: FolderId) = coRunCatching {
        sendToTrash(folderId.userId, volumeId, listOf(folderId))
    }

    private suspend fun getOrCreateFolder(
        parentFolderId: FolderId,
        folderName: String,
        shouldUpdateEvent: Boolean,
    ): Result<FolderId> = coRunCatching {
        createFolder(
            parentFolderId = parentFolderId,
            folderName = folderName,
            shouldUpdateEvent = shouldUpdateEvent,
        ).recoverCatching { error ->
            error.onProtonHttpException { protonData ->
                if (protonData.code == ProtonApiCode.ALREADY_EXISTS) {
                    protonData
                        .detailsOrNull<AlreadyExistsErrorResponseDetails>()
                        ?.conflictLinkId
                        ?.let { conflictLinkId ->
                            folderName to FolderId(parentFolderId.shareId, conflictLinkId)
                        }
                        ?: throw error
                } else {
                    throw error
                }
            } ?: throw error
        }.getOrThrow().second
    }

    private val DocumentFile.nameOrUnnamed: String get() = name ?: uri.last ?: "unnamed"
}
