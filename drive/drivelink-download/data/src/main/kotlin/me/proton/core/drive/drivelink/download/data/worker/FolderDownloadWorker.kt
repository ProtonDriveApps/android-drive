/*
 * Copyright (c) 2021-2023 Proton AG.
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
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.data.extension.logTag
import me.proton.core.drive.drivelink.download.data.extension.uniqueFolderWorkName
import me.proton.core.drive.drivelink.download.data.extension.uniqueWorkName
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_VOLUME_ID
import me.proton.core.drive.folder.domain.usecase.GetDescendants
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
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
    override val logTag = folderId.logTag

    override suspend fun doLimitedRetryWork(): Result {
        CoreLogger.d(logTag, "Started downloading folder")
        val folder = getLink(folderId).toResult().getOrNull() ?: return Result.failure()
        val descendants = getDescendants(folder, true).getOrNull() ?: return Result.retry().also {
            CoreLogger.d(logTag, "Failed to get descendants, retrying")
        }
        downloadFolderAndDescendants(
            folder = folder,
            descendants = descendants.mapNotNull { link -> getLinkNode(link.id).toResult().getOrNull() }
        )
        return Result.success()
    }

    @Suppress("EnqueueWork")
    private fun downloadFolderAndDescendants(
        folder: Link.Folder,
        descendants: List<LinkNode>,
    ) {
        val folderTag = uniqueFolderWorkName(folderId)
        // First we create task for WorkManager's to set all node as downloading
        var workContinuation = workManager.beginWith(
            (listOf(folder) + descendants.map { node -> node.link }).setAsDownloading(userId, folderTag)
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
            val parentId = currentDescendant.parentId
            workContinuation = when (currentDescendant) {
                is Link.Folder -> {
                    // Since it's a folder, we assume all its children have already been taken care of
                    // so we mark it as downloaded (this task will follow after all the node inside have
                    // been downloaded). And we remove it from the unhandled children
                    mutableDescendants.removeFirst()
                    workContinuation.then(currentDescendant.setAsDownloaded(userId, folderTag))
                }
                is Link.File -> {
                    // We are handling a file inside a folder, we retrieve all the files from that same
                    // folder to treat them at the same time and we create a task to download each file
                    // We also remove them from the list of unhandled children
                    mutableDescendants
                        .filter { link -> link.parentId == parentId && link is Link.File }
                        .also { siblings -> mutableDescendants.removeAll(siblings) }
                        .fold(workContinuation) { continuation, link ->
                            continuation.then((link as Link.File).setAsToDownload(userId, folderTag))
                        }
                }
            }
            // If descendants list does not contain any child of parent folder, we can mark parent folder as downloaded
            if (mutableDescendants.none { link -> link.parentId == parentId }) {
                mutableDescendants
                    .filterIsInstance(Link.Folder::class.java)
                    .find { link -> link.id == parentId}
                    ?.let { parent ->
                        workContinuation = workContinuation.then(parent.setAsDownloaded(userId, folderTag))
                    }
            }
        }
        // Finally, we can mark this drive node as downloaded and finish this worker successfully
        workContinuation.then(folder.setAsDownloaded(userId, folderTag)).enqueue()
    }

    private fun List<Link>.setAsDownloading(userId: UserId, folderTag: String) =
        filterIsInstance<Link.Folder>()
            .map { link ->
                (link as? Link.Folder)?.let { folder ->
                    FolderDownloadStateUpdateWorker.getWorkRequest(
                        userId = userId,
                        folderId = folder.id,
                        isDownloadFinished = false,
                        tags = listOf(folderTag),
                    )
                }
            }

    private fun Link.Folder.setAsDownloaded(userId: UserId, folderTag: String) =
        FolderDownloadStateUpdateWorker.getWorkRequest(
            userId = userId,
            folderId = id,
            isDownloadFinished = true,
            tags = listOf(folderTag),
        )

    private fun Link.File.setAsToDownload(userId: UserId, folderTag: String) =
        FileDownloadWorker.getWorkRequest(
            userId = userId,
            volumeId = volumeId,
            fileId = id,
            revisionId = activeRevisionId,
            isRetryable = true,
            tags = listOf(folderTag),
        )

    companion object {
        fun getWorkRequest(
            driveLink: DriveLink.Folder,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(FolderDownloadWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, driveLink.userId.id)
                        .putString(KEY_VOLUME_ID, driveLink.volumeId.id)
                        .putString(KEY_SHARE_ID, driveLink.id.shareId.id)
                        .putString(KEY_FOLDER_ID, driveLink.id.id)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTags(listOf(driveLink.userId.id, driveLink.uniqueWorkName) + tags)
                .build()
    }
}
