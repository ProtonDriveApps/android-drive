/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.drivelink.download.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.LoggerLevel.ERROR
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.data.extension.logTag
import me.proton.core.drive.drivelink.download.data.extension.uniqueFolderWorkName
import me.proton.core.drive.drivelink.download.data.extension.uniqueWorkName
import me.proton.core.drive.drivelink.download.data.worker.FolderDownloadStateUpdateWorker.Companion.ROOT_FOLDER_DOWNLOAD_STATE_UPDATE_TAG
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_NETWORK_TYPE
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_VOLUME_ID
import me.proton.core.drive.folder.domain.usecase.GetDescendants
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.isProtonCloudFile
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.linknode.domain.entity.LinkNode
import me.proton.core.drive.linknode.domain.extension.ancestors
import me.proton.core.drive.linknode.domain.usecase.GetLinkNode
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.worker.data.LimitedRetryCoroutineWorker
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.TimeUnit

@HiltWorker
class FolderDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val workManager: WorkManager,
    private val getLink: GetLink,
    private val getLinkNode: GetLinkNode,
    private val getDescendants: GetDescendants,
    canRun: CanRun,
    run: Run,
    done: Done,
) : LimitedRetryCoroutineWorker(appContext, workerParams, canRun, run, done) {
    override val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val volumeId = VolumeId(requireNotNull(inputData.getString(KEY_VOLUME_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val folderId = FolderId(shareId, requireNotNull(inputData.getString(KEY_FOLDER_ID)))
    private val networkType = inputData.getString(KEY_NETWORK_TYPE)
        ?.let { name -> NetworkType.values().find { it.name == name } } ?: NetworkType.CONNECTED
    override val logTag = folderId.logTag

    override suspend fun doLimitedRetryWork(): Result {
        CoreLogger.d(logTag, "Started downloading folder")
        val folder = getLink(folderId).toResult().onFailure { error ->
            error.log(logTag, "Cannot get link")
        }.getOrNull() ?: return Result.failure()
        val descendants = getDescendants(folder, true).onFailure { error ->
            if (error is OutOfMemoryError) {
                System.gc()
                error.log(logTag, "Failed to get descendants", ERROR)
                return Result.failure()
            }
            error.log(logTag, "Failed to get descendants, will retry", WARNING)
        }.getOrNull()?.filterNot { link -> link.isProtonCloudFile } ?: return Result.retry()
        downloadFolderAndDescendants(
            folder = folder,
            descendants = descendants.mapNotNull { link -> getLinkNode(link.id).toResult().getOrNull() }
        )
        return Result.success()
    }

    @Suppress("EnqueueWork")
    private suspend fun downloadFolderAndDescendants(
        folder: Link.Folder,
        descendants: List<LinkNode>,
    ) {
        val folderTag = uniqueFolderWorkName(folderId)
        // First we create task for WorkManager's to set all node as downloading
        var workContinuation = workManager.beginWith(
            (listOf(folder) + descendants.map { node -> node.link }).setAsDownloading(userId, folder.id, networkType)
        )
        // We then sort them from deepest in the hierarchy to highest, keeping folders before files
        val mutableDescendants = descendants.sortedWith(
            compareByDescending<LinkNode> { linkNode -> linkNode.ancestors.count() }
                .thenBy { linkNode -> if (linkNode.link is Link.Folder) 0 else 1 }
        )
            .map { linkNode -> linkNode.link }
            .toMutableList()
        CoreLogger.d(logTag, "mutableDescendants.size = ${mutableDescendants.size}")
        // We loop on the descendants which have not been handled yet
        while (mutableDescendants.isNotEmpty()) {
            // We peek at the first node in the list
            val currentDescendant = mutableDescendants.first()
            val folderId = currentDescendant.parentId as? FolderId
            // If descendants list does not contain any child of parent folder, we can mark parent folder as downloaded
            if (mutableDescendants.none { link -> link.parentId == folderId }) {
                mutableDescendants
                    .filterIsInstance<Link.Folder>()
                    .find { link -> link.id == folderId}
                    ?.let { parent ->
                        workContinuation = workContinuation.then(parent.setAsDownloaded(userId, folder.id, networkType))
                    }
            }
            workContinuation = when (currentDescendant) {
                is Link.Album -> error("TODO")
                is Link.Folder -> {
                    // Since it's a folder, we assume all its children have already been taken care of
                    // so we mark it as downloaded (this task will follow after all the node inside have
                    // been downloaded). And we remove it from the unhandled children
                    mutableDescendants.removeAt(0)
                    workContinuation.then(currentDescendant.setAsDownloaded(userId, folder.id, networkType))
                }
                is Link.File -> {
                    // We are handling a file inside a folder, we retrieve all the files from that same
                    // folder to treat them at the same time and we create a task to download each file
                    // We also remove them from the list of unhandled children
                    mutableDescendants
                        .filter { link -> link.parentId == folderId && link is Link.File }
                        .also { siblings -> mutableDescendants.removeAll(siblings) }
                        .fold(workContinuation) { continuation, link ->
                            continuation.then((link as Link.File).setAsToDownload(userId, folderTag))
                        }
                }
            }
        }
        // Finally, we can mark this drive node as downloaded and finish this worker successfully
        workContinuation.then(folder.setAsDownloaded(userId, folder.id, networkType)).enqueue().await()
    }

    private fun List<Link>.setAsDownloading(userId: UserId, rootFolderId: FolderId, networkType: NetworkType) =
        filterIsInstance<Link.Folder>()
            .map { folder ->
                FolderDownloadStateUpdateWorker.getWorkRequest(
                    userId = userId,
                    folderId = folder.id,
                    rootFolderId = rootFolderId,
                    isDownloadFinished = false,
                    networkType = networkType,
                    tags = listOf(uniqueFolderWorkName(rootFolderId)),
                )
            }

    private fun Link.Folder.setAsDownloaded(userId: UserId, rootFolderId: FolderId, networkType: NetworkType) =
        FolderDownloadStateUpdateWorker.getWorkRequest(
            userId = userId,
            folderId = id,
            rootFolderId = rootFolderId,
            isDownloadFinished = true,
            networkType = networkType,
            tags = listOfNotNull(
                uniqueFolderWorkName(rootFolderId),
                takeIf { id == rootFolderId }?.let { ROOT_FOLDER_DOWNLOAD_STATE_UPDATE_TAG }
            ),
        )

    private fun Link.File.setAsToDownload(userId: UserId, folderTag: String) =
        FileDownloadWorker.getWorkRequest(
            userId = userId,
            volumeId = volumeId,
            fileId = id,
            revisionId = activeRevisionId,
            isRetryable = true,
            fileTags = listOfNotNull(
                folderTag,
                (parentId as? FolderId)?.let { uniqueFolderWorkName(it) },
            ),
        )

    companion object {
        fun getWorkRequest(
            driveLink: DriveLink.Folder,
            networkType: NetworkType = NetworkType.CONNECTED,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(FolderDownloadWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(networkType)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, driveLink.userId.id)
                        .putString(KEY_VOLUME_ID, driveLink.volumeId.id)
                        .putString(KEY_SHARE_ID, driveLink.id.shareId.id)
                        .putString(KEY_FOLDER_ID, driveLink.id.id)
                        .putString(KEY_NETWORK_TYPE, networkType.name)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTags(listOf(driveLink.userId.id, driveLink.uniqueWorkName) + tags)
                .build()
    }
}
