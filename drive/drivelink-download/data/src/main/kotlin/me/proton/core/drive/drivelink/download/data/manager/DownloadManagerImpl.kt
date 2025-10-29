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

package me.proton.core.drive.drivelink.download.data.manager

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.await
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.LoggerLevel.ERROR
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.download.data.extension.observeNetworkTypes
import me.proton.core.drive.drivelink.download.data.manager.DownloadManagerImpl.DownloadFileTask
import me.proton.core.drive.drivelink.download.data.worker.FileDownloaderWorker
import me.proton.core.drive.drivelink.download.domain.entity.DownloadFileLink
import me.proton.core.drive.drivelink.download.domain.entity.DownloadParentLink
import me.proton.core.drive.drivelink.download.domain.entity.NetworkType
import me.proton.core.drive.drivelink.download.domain.extension.post
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.drivelink.download.domain.manager.DownloadManager
import me.proton.core.drive.drivelink.download.domain.manager.PipelineManager
import me.proton.core.drive.drivelink.download.domain.repository.DownloadFileRepository
import me.proton.core.drive.drivelink.download.domain.repository.DownloadParentLinkRepository
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadCleanup
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadFile
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadMetricsNotifier
import me.proton.core.drive.folder.domain.usecase.GetDescendants
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.File
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.isProtonCloudFile
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.AreAllAlbumPhotosDownloaded
import me.proton.core.drive.linkdownload.domain.usecase.AreAllFilesDownloaded
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import me.proton.core.drive.linkoffline.domain.usecase.IsMarkedAsOffline
import me.proton.core.drive.photo.domain.usecase.GetAllAlbumChildren
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class DownloadManagerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val pipelineManager: PipelineManager<DownloadFileTask>,
    private val downloadFileRepository: DownloadFileRepository,
    private val downloadParentLinkRepository: DownloadParentLinkRepository,
    private val downloadFile: DownloadFile,
    private val downloadCleanup: DownloadCleanup,
    private val workManager: WorkManager,
    private val areAllFilesDownloaded: AreAllFilesDownloaded,
    private val setDownloadState: SetDownloadState,
    private val getDescendants: GetDescendants,
    private val getDriveLink: GetDriveLink,
    private val isMarkedAsOffline: IsMarkedAsOffline,
    private val configurationProvider: ConfigurationProvider,
    private val getAllAlbumChildren: GetAllAlbumChildren,
    private val areAllAlbumPhotosDownloaded: AreAllAlbumPhotosDownloaded,
    private val downloadErrorManager: DownloadErrorManager,
    private val downloadMetricsNotifier: DownloadMetricsNotifier,
) : DownloadManager, DownloadManager.FileDownloader, PipelineManager.TaskProvider<DownloadFileTask> {
    private var userId: UserId? = null
    private val runningTasks = MutableStateFlow(emptySet<DownloadFileTask>())
    private val currentNetworkTypes: MutableStateFlow<Set<NetworkType>> = MutableStateFlow(emptySet())

    override suspend fun start(
        userId: UserId,
        coroutineContext: CoroutineContext,
    ): Result<Unit> = coRunCatching {
        this.userId = userId
        downloadFileRepository.resetAllState(userId, DownloadFileLink.State.IDLE)
        pipelineManager.start(
            taskProvider = this,
            coroutineContext = coroutineContext,
        ).getOrThrow()
        observeIdleFiles(userId, coroutineContext)
        observeParents(userId, coroutineContext)
        observeNetworkTypes(userId, coroutineContext)
    }

    override suspend fun stop(userId: UserId): Result<Unit> = coRunCatching {
        pipelineManager.stop().getOrThrow()
        if (this.userId == userId) {
            this.userId = null
        }
    }

    override suspend fun download(
        driveLink: DriveLink,
        priority: Long,
        retryable: Boolean,
        networkType: NetworkType,
    ) {
        CoreLogger.d(LogTag.DOWNLOAD, "download(driveLinkId=${driveLink.id.id.logId()}, priority=$priority, retryable=$retryable, networkType=$networkType)")
        when (driveLink) {
            is DriveLink.File -> downloadFile(
                volumeId = driveLink.volumeId,
                fileId = driveLink.id,
                revisionId = driveLink.activeRevisionId,
                priority = priority,
                retryable = retryable,
                networkType = networkType,
            )
            is DriveLink.Folder -> downloadFolder(
                volumeId = driveLink.volumeId,
                parentLink = driveLink.link,
                priority = priority,
                retryable = retryable,
                networkType = networkType,
            )
            is DriveLink.Album -> downloadAlbum(
                volumeId = driveLink.volumeId,
                albumId = driveLink.id,
                priority = priority,
                retryable = retryable,
                networkType = networkType,
            )
        }
        startFileDownloaderWorker(driveLink.userId)
    }

    override suspend fun cancel(driveLink: DriveLink) {
        CoreLogger.d(LogTag.DOWNLOAD, "cancel(driveLinkId=${driveLink.id.id.logId()})")
        when (driveLink) {
            is DriveLink.File -> cancelFileDownload(
                volumeId = driveLink.volumeId,
                fileId = driveLink.id,
                revisionId = driveLink.activeRevisionId,
            ).getOrNull(driveLink.id.logTag, "Failed to cancel file download")
            is DriveLink.Folder -> cancelFolderDownload(
                volumeId = driveLink.volumeId,
                parentLink = driveLink.link,
            )
            is DriveLink.Album -> cancelAlbumDownload(
                volumeId = driveLink.volumeId,
                albumId = driveLink.id,
            )
        }
    }

    override suspend fun cancelAll(userId: UserId) {
        pipelineManager.stopPipelines(immediately = true)
        downloadFileRepository.deleteAll(userId)
        downloadParentLinkRepository.deleteAll(userId)
    }

    override fun getProgressFlow(driveLink: DriveLink.File): Flow<Percentage>? =
        runningTasks
            .takeIf { runningTasks -> runningTasks.value.firstOrNull(driveLink.id) != null }
            ?.transform { runningTasks ->
                runningTasks
                    .firstOrNull(driveLink.id)
                    ?.let { task -> emitAll(task.progress) }
            }

    override suspend fun getNextTask(
        pipelineId: Long,
    ): Result<DownloadFileTask> = coRunCatching {
        downloadFileRepository
            .getNextIdleAndUpdate(
                userId = requireNotNull(userId),
                networkTypes = currentNetworkTypes.value,
                state = DownloadFileLink.State.RUNNING,
            )
            ?.let { downloadFileLink ->
                DownloadFileTask(
                    pipelineId = pipelineId,
                    downloadFileLink = downloadFileLink,
                    progress = MutableStateFlow(Percentage(0)),
                    downloadFile = downloadFile,
                ).also { task ->
                    runningTasks.value += task
                }
            } ?: throw NoSuchElementException("No download file link found")
    }

    override suspend fun taskCancelled(task: DownloadFileTask, isCancelledByStop: Boolean) {
        withContext(NonCancellable) {
            CoreLogger.d(
                task.downloadFileLink.fileId.logTag,
                "taskCancelled pipelineId=${task.pipelineId} isCancelledByStop=$isCancelledByStop"
            )
            downloadErrorManager.post(task.downloadFileLink.fileId, CancellationException(), true)
            runningTasks.value -= task
            if (task.downloadFileLink.retryable && !isCancelledByStop && task.downloadFileLink.numberOfRetries < configurationProvider.maxApiAutoRetries) {
                downloadFileRepository.updateStateToFailed(task.downloadFileLink.id)
                startFileDownloaderWorker(task.downloadFileLink.fileId.userId)
                setDownloadState(task.downloadFileLink.fileId, DownloadState.Error)
            } else {
                cancelFileDownload(
                    volumeId = task.downloadFileLink.volumeId,
                    fileId = task.downloadFileLink.fileId,
                    downloadFileId = task.downloadFileLink.id,
                    isCancelledByStop = isCancelledByStop,
                )
            }
        }
    }

    override suspend fun taskCompleted(task: DownloadFileTask, throwable: Throwable?) {
        if (throwable == null) {
            taskCompleteSuccessfully(task)
        } else {
            taskCompletedWithException(task, throwable)
        }
    }

    private suspend fun taskCompleteSuccessfully(task: DownloadFileTask) {
        CoreLogger.d(task.downloadFileLink.fileId.logTag, "taskCompleted pipelineId=${task.pipelineId}")
        downloadMetricsNotifier(task.downloadFileLink.fileId, true)
        runningTasks.value -= task
        downloadFileRepository.delete(task.downloadFileLink.id)
    }

    private suspend fun taskCompletedWithException(task: DownloadFileTask, throwable: Throwable) {
        CoreLogger.d(task.downloadFileLink.fileId.logTag, throwable, "taskCompleted pipelineId=${task.pipelineId}")
        downloadErrorManager.post(task.downloadFileLink.fileId, throwable)
        downloadMetricsNotifier(task.downloadFileLink.fileId, false, throwable)
        runningTasks.value -= task
        if (task.downloadFileLink.retryable && task.downloadFileLink.numberOfRetries < configurationProvider.maxApiAutoRetries) {
            downloadFileRepository.updateStateToFailed(task.downloadFileLink.id)
            startFileDownloaderWorker(task.downloadFileLink.fileId.userId)
        } else {
            downloadFileCleanup(task.downloadFileLink.volumeId, task.downloadFileLink.fileId)
            downloadFileRepository.delete(task.downloadFileLink.id)
        }
    }

    private suspend fun downloadFile(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
        priority: Long,
        retryable: Boolean,
        networkType: NetworkType,
        state: DownloadFileLink.State = DownloadFileLink.State.IDLE,
    ) {
        downloadFileRepository.add(
            DownloadFileLink(
                id = 0,
                volumeId = volumeId,
                fileId = fileId,
                revisionId = revisionId,
                priority = priority,
                retryable = retryable,
                state = state,
                numberOfRetries = 0,
                networkType = networkType,
            )
        )
    }

    private suspend fun downloadFolder(
        volumeId: VolumeId,
        parentLink: Link.Folder,
        priority: Long,
        retryable: Boolean,
        networkType: NetworkType,
    ) {
        getDescendants(parentLink, true).onFailure { error ->
            if (error is OutOfMemoryError) {
                System.gc()
            }
            error.log(LogTag.DOWNLOAD, "Failed to get descendants", ERROR)
        }.getOrNull()
            ?.filterNot { link -> link.isProtonCloudFile }
            ?.let { links ->
                CoreLogger.d(LogTag.DOWNLOAD, "downloadFolder descendants=${links.size}")
                if (links.isNotEmpty()) {
                    setDownloadState(parentLink, DownloadState.Downloading)
                    downloadParentLinkRepository.add(
                        DownloadParentLink(
                            id = 0L,
                            volumeId = volumeId,
                            linkId = parentLink.id,
                            priority = priority,
                            retryable = retryable,
                        )
                    )
                } else {
                    setDownloadState(parentLink, DownloadState.Ready)
                }
                links.forEach { link ->
                    when (link) {
                        is File -> getDriveLink(link.id).toResult().getOrNull(LogTag.DOWNLOAD)
                            ?.let { driveLink ->
                                downloadFile(
                                    volumeId = volumeId,
                                    fileId = link.id,
                                    revisionId = driveLink.activeRevisionId,
                                    priority = priority,
                                    retryable = retryable,
                                    networkType = networkType,
                                )
                            }
                        is Folder -> let {
                            setDownloadState(link, DownloadState.Downloading)
                            downloadParentLinkRepository.add(
                                DownloadParentLink(
                                    id = 0L,
                                    volumeId = volumeId,
                                    linkId = link.id,
                                    priority = priority,
                                    retryable = retryable,
                                )
                            )
                        }
                        else -> error("Unexpected link type: $link")
                    }
                }
            }
    }

    private suspend fun downloadAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        priority: Long,
        retryable: Boolean,
        networkType: NetworkType,
    ) {
        getAllAlbumChildren(
            volumeId = volumeId,
            albumId = albumId,
            refresh = true,
        ).getOrNull(LogTag.DOWNLOAD)
            ?.let { photos ->
                if (photos.isNotEmpty()) {
                    setDownloadState(albumId, DownloadState.Downloading)
                    downloadParentLinkRepository.add(
                        DownloadParentLink(
                            id = 0L,
                            volumeId = volumeId,
                            linkId = albumId,
                            priority = priority,
                            retryable = retryable,
                        )
                    )
                } else {
                    setDownloadState(albumId, DownloadState.Ready)
                }
                photos.forEach { fileId ->
                    getDriveLink(fileId).toResult().getOrNull(fileId.logTag)?.let { driveLink ->
                        downloadFile(
                            volumeId = driveLink.volumeId,
                            fileId = fileId,
                            revisionId = driveLink.activeRevisionId,
                            priority = priority,
                            retryable = retryable,
                            networkType = networkType,
                        )
                    }
                }
            }
    }

    private suspend fun cancelFileDownload(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
    ) = cancelFileDownload(
        volumeId = volumeId,
        fileId = fileId,
        shouldCleanup = true,
    ) {
        downloadFileRepository.delete(
            volumeId = volumeId,
            fileId = fileId,
            revisionId = revisionId,
        )
    }

    private suspend fun cancelFileDownload(
        volumeId: VolumeId,
        fileId: FileId,
        downloadFileId: Long,
        isCancelledByStop: Boolean,
    ) = cancelFileDownload(
        volumeId = volumeId,
        fileId = fileId,
        shouldCleanup = isCancelledByStop,
    ) {
        downloadFileRepository.delete(downloadFileId)
    }

    private suspend fun cancelFileDownload(
        volumeId: VolumeId,
        fileId: FileId,
        shouldCleanup: Boolean,
        deleteFromRepository: suspend () -> Unit,
    ) = coRunCatching(NonCancellable) {
        runningTasks.value.firstOrNull(fileId)?.let { task ->
            CoreLogger.d(fileId.logTag, "Stopping file download task")
            pipelineManager.stopPipeline(task.pipelineId)
            return@coRunCatching
        }
        if (shouldCleanup) {
            downloadFileCleanup(volumeId, fileId)
        }
        deleteFromRepository()
    }

    private suspend fun downloadFileCleanup(
        volumeId: VolumeId,
        fileId: FileId,
    ) = withContext(Dispatchers.IO) {
        downloadCleanup(
            volumeId = volumeId,
            linkId = fileId,
        )
            .onSuccess {
                CoreLogger.d(
                    fileId.logTag,
                    "Download cleanup successful"
                )
            }
            .getOrNull(fileId.logTag, "Download cleanup failed")
    }

    private suspend fun cancelFolderDownload(
        volumeId: VolumeId,
        parentLink: Link.Folder,
    ) = coRunCatching {
        downloadCleanup(volumeId, parentLink.id)
        downloadParentLinkRepository.delete(volumeId, parentLink.id)
        getDescendants(parentLink, false).onFailure { error ->
            if (error is OutOfMemoryError) {
                System.gc()
            }
            error.log(LogTag.DOWNLOAD, "Failed to get descendants", ERROR)
        }.getOrNull()
            ?.filterNot { link -> link.isProtonCloudFile }
            ?.let { links ->
                links
                    .filter { link -> isMarkedAsOffline(link.id).not() }
                    .forEach { link ->

                        when (link) {
                            is Link.File -> getDriveLink(link.id).toResult().getOrNull(LogTag.DOWNLOAD)
                                ?.let { driveLink ->
                                    cancelFileDownload(
                                        volumeId = volumeId,
                                        fileId = link.id,
                                        revisionId = driveLink.activeRevisionId,
                                    )
                                }
                            is Link.Folder -> let {
                                downloadCleanup(volumeId, link.id)
                                downloadParentLinkRepository.delete(volumeId, link.id)
                            }
                            else -> error("Unexpected link type: $link")
                        }
                    }
            }
    }

    private suspend fun cancelAlbumDownload(
        volumeId: VolumeId,
        albumId: AlbumId,
    ) = coRunCatching {
        downloadCleanup(volumeId, albumId)
        downloadParentLinkRepository.delete(volumeId, albumId)
        getAllAlbumChildren(
            volumeId = volumeId,
            albumId = albumId,
            refresh = false,
        ).getOrNull(LogTag.DOWNLOAD)
            ?.forEach { fileId ->
                getDriveLink(fileId).toResult().getOrNull(fileId.logTag)?.let { driveLink ->
                    cancelFileDownload(
                        volumeId = driveLink.volumeId,
                        fileId = fileId,
                        revisionId = driveLink.activeRevisionId,
                    )
                }
            }
    }

    private suspend fun removeDownloadedParents(userId: UserId) {
        downloadParentLinkRepository
            .getAllParentLinks(userId)
            .mapNotNull { downloadParentLink ->
                takeIf { downloadFileRepository.hasChildrenOf(userId, downloadParentLink.volumeId, downloadParentLink.linkId).not() }
                    ?.let { downloadParentLink }
            }
            .forEach { downloadParentLink: DownloadParentLink ->
                when (val parentId = downloadParentLink.linkId) {
                    is FolderId ->
                        coRunCatching { areAllFilesDownloaded(parentId) }.getOrNull()
                            ?.let { areAllFilesDownloaded ->
                                if (areAllFilesDownloaded) {
                                    downloadParentLinkRepository.delete(downloadParentLink.id)
                                    setDownloadState(parentId, DownloadState.Ready)
                                } else {
                                    CoreLogger.d(LogTag.DOWNLOAD, "Not all files are downloaded for folder ${parentId.id.logId()}")
                                }
                            }
                    is AlbumId -> coRunCatching { areAllAlbumPhotosDownloaded(parentId) }.getOrNull()
                        ?.let { areAllAlbumPhotosDownloaded ->
                            if (areAllAlbumPhotosDownloaded) {
                                downloadParentLinkRepository.delete(downloadParentLink.id)
                                setDownloadState(parentId, DownloadState.Ready)
                            } else {
                                CoreLogger.d(LogTag.DOWNLOAD, "Not all photos are downloaded for album ${parentId.id.logId()}")
                            }
                        }
                    else -> error("Unexpected parent type: $parentId")
                }
            }
    }

    private fun Set<DownloadFileTask>.firstOrNull(
        fileId: FileId,
    ): DownloadFileTask? = firstOrNull { task ->
        task.downloadFileLink.fileId == fileId
    }

    private val FileId.logTag: String get() = "${LogTag.DOWNLOAD}.${id.logId()}"

    private suspend fun startFileDownloaderWorker(userId: UserId) {
        workManager.enqueueUniqueWork(
            FileDownloaderWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            FileDownloaderWorker.getWorkRequest(userId)
        ).await()
    }

    private fun observeIdleFiles(userId: UserId, coroutineContext: CoroutineContext) {
        downloadFileRepository
            .getCountFlow(userId, DownloadFileLink.State.IDLE)
            .onEach { count ->
                if (count > 0) {
                    pipelineManager
                        .startPipelines()
                        .getOrNull(LogTag.DOWNLOAD, "Failed to start pipelines")
                }
            }
            .launchIn(CoroutineScope(coroutineContext))
    }

    private fun observeParents(userId: UserId, coroutineContext: CoroutineContext) {
        combine(
            downloadParentLinkRepository
                .getCountFlow(userId),
            downloadFileRepository
                .getCountFlow(userId)
        ) { _, _ ->
            removeDownloadedParents(userId)
        }.launchIn(CoroutineScope(coroutineContext))
    }

    private fun observeNetworkTypes(userId: UserId, coroutineContext: CoroutineContext) {
        appContext.observeNetworkTypes
            .distinctUntilChanged()
            .onEach { networkTypes ->
                CoreLogger.d(
                    tag = LogTag.DOWNLOAD,
                    message = "NetworkTypes old=${currentNetworkTypes.value.joinToString()}, new=${networkTypes.joinToString()}",
                )
                pipelineManager.stopPipelines(
                    immediately = true,
                    cause = CancellationException("Network types changed"),
                )
                downloadFileRepository.getCountFlow(
                    userId,
                    DownloadFileLink.State.RUNNING
                ).first { count -> count == 0 }
                currentNetworkTypes.value = networkTypes
                pipelineManager.startPipelines()
            }
            .launchIn(CoroutineScope(coroutineContext))
    }

    class DownloadFileTask(
        val pipelineId: Long,
        val downloadFileLink: DownloadFileLink,
        val progress: MutableStateFlow<Percentage>,
        val downloadFile: DownloadFile,
    ) : PipelineManager.Task {
        override suspend fun invoke(isCancelled: () -> Boolean) {
            downloadFile(
                volumeId = downloadFileLink.volumeId,
                fileId = downloadFileLink.fileId,
                revisionId = downloadFileLink.revisionId,
                isCancelled = isCancelled,
                progress = progress,
            ).getOrThrow()
        }
    }
}
