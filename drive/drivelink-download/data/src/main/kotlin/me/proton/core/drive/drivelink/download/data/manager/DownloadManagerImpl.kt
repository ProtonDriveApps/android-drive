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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.LoggerLevel.ERROR
import me.proton.core.drive.base.data.extension.isRetryable
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
import me.proton.core.drive.drivelink.download.data.worker.DownloadEventWorker
import me.proton.core.drive.drivelink.download.data.worker.FileDownloaderWorker
import me.proton.core.drive.drivelink.download.domain.entity.DownloadFileLink
import me.proton.core.drive.drivelink.download.domain.entity.DownloadParentLink
import me.proton.core.drive.drivelink.download.domain.entity.NetworkType
import me.proton.core.drive.drivelink.download.domain.extension.post
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.drivelink.download.domain.manager.DownloadManager
import me.proton.core.drive.drivelink.download.domain.manager.DownloadSdkManager
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
import me.proton.core.drive.linktrash.domain.usecase.IsLinkOrAnyAncestorTrashed
import me.proton.core.drive.photo.domain.usecase.GetAllAlbumChildren
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class DownloadManagerImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
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
    private val isLinkOrAnyAncestorTrashed: IsLinkOrAnyAncestorTrashed,
    private val downloadSdkManager: DownloadSdkManager,
) : DownloadManager, DownloadManager.FileDownloader, PipelineManager.TaskProvider<DownloadFileTask> {
    private var userId: UserId? = null
    private val runningTasks = MutableStateFlow(emptySet<DownloadFileTask>())
    private val cancelingFiles = MutableStateFlow(emptySet<Pair<VolumeId, FileId>>())
    private val currentNetworkTypes: MutableStateFlow<Set<NetworkType>> = MutableStateFlow(emptySet())
    private val awaitingCancelCompletion: MutableList<DownloadInfo> = mutableListOf()
    private val mutex = Mutex()

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
        observeCancelingFiles(coroutineContext)
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
    ) = mutex.withLock {
        CoreLogger.d(
            LogTag.DOWNLOAD,
            "download(driveLinkId=${driveLink.id.id.logId()}, priority=$priority, retryable=$retryable, networkType=$networkType)"
        )
        if (driveLink.isCanceling) {
            CoreLogger.d(
                LogTag.DOWNLOAD,
                "Link ${driveLink.id.id.logId()} is currently canceling, it should be re-downloaded once cancel is complete"
            )
            awaitingCancelCompletion.add(
                DownloadInfo(
                    driveLink = driveLink,
                    priority = priority,
                    retryable = retryable,
                    networkType = networkType,
                )
            )
        } else {
            driveLink.download(
                priority = priority,
                retryable = retryable,
                networkType = networkType,
            )
        }
        Unit
    }

    override suspend fun cancel(driveLink: DriveLink) {
        CoreLogger.d(LogTag.DOWNLOAD, "cancel(driveLinkId=${driveLink.id.id.logId()})")
        driveLink.cancelDownload()
    }

    override suspend fun cancelAll(userId: UserId) {
        pipelineManager.stopPipelines(immediately = true)
        downloadFileRepository.deleteAll(userId)
        downloadParentLinkRepository.deleteAll(userId)
    }

    override fun getProgressFlow(fileId: FileId): Flow<Percentage>? =
        runningTasks
            .takeIf { runningTasks -> runningTasks.value.firstOrNull(fileId) != null }
            ?.transform { runningTasks ->
                runningTasks
                    .firstOrNull(fileId)
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
                    setDownloadState = setDownloadState,
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
        setDownloadState(task.downloadFileLink.fileId, DownloadState.Ready)
        downloadSdkManager.close(
            volumeId = task.downloadFileLink.volumeId,
            fileId = task.downloadFileLink.fileId,
            revisionId = task.downloadFileLink.revisionId,
        )
        downloadMetricsNotifier(task.downloadFileLink.fileId, true)
        runningTasks.value -= task
        downloadFileRepository.delete(task.downloadFileLink.id)
    }

    private suspend fun taskCompletedWithException(task: DownloadFileTask, throwable: Throwable) {
        throwable.log(task.downloadFileLink.fileId.logTag, "taskCompleted pipelineId=${task.pipelineId}")
        setDownloadState(task.downloadFileLink.fileId, DownloadState.Error)
        downloadErrorManager.post(task.downloadFileLink.fileId, throwable)
        downloadMetricsNotifier(task.downloadFileLink.fileId, false, throwable)
        runningTasks.value -= task

        if (task.downloadFileLink.retryable && throwable.isRetryable && task.downloadFileLink.numberOfRetries < configurationProvider.maxApiAutoRetries) {
            downloadFileRepository.updateStateToFailed(task.downloadFileLink.id)
            startFileDownloaderWorker(task.downloadFileLink.fileId.userId)
        } else {
            downloadFileCleanup(task.downloadFileLink.volumeId, task.downloadFileLink.fileId)
            downloadFileRepository.delete(task.downloadFileLink.id)
        }
    }

    @JvmName("downloadDriveLink")
    private suspend fun DriveLink.download(
        priority: Long,
        retryable: Boolean,
        networkType: NetworkType,
    ) = when (this) {
            is DriveLink.File -> downloadFile(
                volumeId = volumeId,
                fileId = id,
                revisionId = activeRevisionId,
                priority = priority,
                retryable = retryable,
                networkType = networkType,
            )
            is DriveLink.Folder -> downloadFolder(
                volumeId = volumeId,
                parentLink = link,
                priority = priority,
                retryable = retryable,
                networkType = networkType,
            )
            is DriveLink.Album -> downloadAlbum(
                volumeId = volumeId,
                albumId = id,
                priority = priority,
                retryable = retryable,
                networkType = networkType,
            )
    }.also {
        startFileDownloaderWorker(userId)
        startDownloadEventWorker(userId)
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
            ?.filter { link ->  !isLinkOrAnyAncestorTrashed.invoke(link.id) }
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

    private suspend fun DriveLink.cancelDownload() = when (this) {
        is DriveLink.File -> cancelFileDownload(
            volumeId = volumeId,
            fileId = id,
            revisionId = activeRevisionId,
        ).getOrNull(id.logTag, "Failed to cancel file download")
        is DriveLink.Folder -> cancelFolderDownload(
            volumeId = volumeId,
            parentLink = link,
        )
        is DriveLink.Album -> cancelAlbumDownload(
            volumeId = volumeId,
            albumId = id,
        )
    }

    private val DriveLink.isCanceling: Boolean get() =
        cancelingFiles.value.any { (volumeId, fileId) ->
            this.id is FileId && this.id == fileId && this.volumeId == volumeId
        }

    private suspend fun cancelFileDownload(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
    ) = mutex.withLock {
        cancelFileDownloadInternal(
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
    ) = mutex.withLock {
        cancelFileDownloadInternal(
            volumeId = volumeId,
            fileId = fileId,
            downloadFileId = downloadFileId,
            isCancelledByStop = isCancelledByStop,
        )
    }

    private suspend fun cancelFileDownloadInternal(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
    ) = cancelFileDownloadInternal(
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

    private suspend fun cancelFileDownloadInternal(
        volumeId: VolumeId,
        fileId: FileId,
        downloadFileId: Long,
        isCancelledByStop: Boolean,
    ) = cancelFileDownloadInternal(
        volumeId = volumeId,
        fileId = fileId,
        shouldCleanup = isCancelledByStop,
    ) {
        downloadFileRepository.delete(downloadFileId)
    }

    private suspend fun cancelFileDownloadInternal(
        volumeId: VolumeId,
        fileId: FileId,
        shouldCleanup: Boolean,
        deleteFromRepository: suspend () -> Unit,
    ) = coRunCatching(NonCancellable) {
        runningTasks.value.firstOrNull(fileId)?.let { task ->
            cancelingFiles.value += volumeId to fileId
            CoreLogger.d(fileId.logTag, "Stopping file download task")
            pipelineManager.stopPipeline(task.pipelineId)
            return@coRunCatching
        }
        if (shouldCleanup) {
            downloadFileCleanup(volumeId, fileId)
        }
        deleteFromRepository()
        cancelingFiles.value -= volumeId to fileId
    }

    private suspend fun downloadFileCleanup(
        volumeId: VolumeId,
        fileId: FileId,
    ) = withContext(Dispatchers.IO) {
        downloadCleanup(
            volumeId = volumeId,
            linkId = fileId,
        ).onSuccess {
            CoreLogger.d(fileId.logTag, "File download cleanup successful")
        }.onFailure { error ->
            error.log(fileId.logTag, "File download cleanup failed")
        }
    }

    private suspend fun cancelFolderDownload(
        volumeId: VolumeId,
        parentLink: Link.Folder,
    ) = coRunCatching {
        mutex.withLock {
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
                                        cancelFileDownloadInternal(
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
    }

    private suspend fun cancelAlbumDownload(
        volumeId: VolumeId,
        albumId: AlbumId,
    ) = coRunCatching {
        mutex.withLock {
            downloadCleanup(volumeId, albumId)
            downloadParentLinkRepository.delete(volumeId, albumId)
            getAllAlbumChildren(
                volumeId = volumeId,
                albumId = albumId,
                refresh = false,
            ).getOrNull(LogTag.DOWNLOAD)
                ?.forEach { fileId ->
                    getDriveLink(fileId).toResult().getOrNull(fileId.logTag)?.let { driveLink ->
                        cancelFileDownloadInternal(
                            volumeId = driveLink.volumeId,
                            fileId = fileId,
                            revisionId = driveLink.activeRevisionId,
                        )
                    }
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
                    else -> error("Unexpected parent type: ${parentId.javaClass.name}")
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

    private suspend fun startDownloadEventWorker(userId: UserId) {
        workManager.enqueueUniqueWork(
            DownloadEventWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            DownloadEventWorker.getWorkRequest(userId)
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
                CoreLogger.i(
                    tag = LogTag.DOWNLOAD,
                    message = "NetworkTypes old=${currentNetworkTypes.value.joinToString()}, " +
                            "new=${networkTypes.joinToString()}",
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

    private fun observeCancelingFiles(coroutineContext: CoroutineContext) {
        cancelingFiles
            .onEach { cancelingFiles ->
                CoreLogger.d(
                    tag = LogTag.DOWNLOAD,
                    message = buildString {
                        append("Observing canceling files ")
                        append(cancelingFiles.joinToString { (_, fileId) -> fileId.id.logId() })
                    }
                )
                val downloadInfos = mutex.withLock {
                    val snapshot = awaitingCancelCompletion
                        .filterNot { (driveLink, _, _, _) ->
                            cancelingFiles.contains(driveLink.volumeId to driveLink.id)
                        }
                    awaitingCancelCompletion.removeAll(snapshot)
                    CoreLogger.d(
                        tag = LogTag.DOWNLOAD,
                        message = buildString {
                            append("Observing awaiting cancel completion ")
                            append(
                                awaitingCancelCompletion.joinToString { info ->
                                    info.driveLink.id.id.logId()
                                }
                            )
                        },
                    )
                    snapshot
                }
                downloadInfos
                    .forEach { downloadInfo ->
                        val driveLink = downloadInfo.driveLink
                        CoreLogger.d(
                            tag = LogTag.DOWNLOAD,
                            message = buildString {
                                append("re-download(driveLinkId=${driveLink.id.id.logId()}, ")
                                append("priority=${downloadInfo.priority}, ")
                                append("retryable=${downloadInfo.retryable}, ")
                                append("networkType=${downloadInfo.networkType})")
                            }
                        )
                        driveLink.download(
                            priority = downloadInfo.priority,
                            retryable = downloadInfo.retryable,
                            networkType = downloadInfo.networkType,
                        )
                }
            }
            .launchIn(CoroutineScope(coroutineContext))
    }

    class DownloadFileTask(
        val pipelineId: Long,
        val downloadFileLink: DownloadFileLink,
        val progress: MutableStateFlow<Percentage>,
        val setDownloadState: SetDownloadState,
        val downloadFile: DownloadFile,
    ) : PipelineManager.Task {
        override suspend fun invoke(isCancelled: () -> Boolean) {
            setDownloadState(downloadFileLink.fileId, DownloadState.Downloading)

            downloadFile(
                volumeId = downloadFileLink.volumeId,
                fileId = downloadFileLink.fileId,
                revisionId = downloadFileLink.revisionId,
                isCancelled = isCancelled,
                progress = progress,
            ).getOrThrow()
        }
    }

    private data class DownloadInfo(
        val driveLink: DriveLink,
        val priority: Long,
        val retryable: Boolean,
        val networkType: NetworkType,
    )
}
