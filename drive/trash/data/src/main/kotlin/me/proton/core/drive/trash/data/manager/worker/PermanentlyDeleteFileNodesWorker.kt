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

package me.proton.core.drive.trash.data.manager.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.ids
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.trash.data.manager.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.trash.data.manager.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.trash.data.manager.worker.WorkerKeys.KEY_WORK_ID
import me.proton.core.drive.trash.domain.notification.DeleteFilesExtra
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository
import java.util.concurrent.TimeUnit
import me.proton.core.drive.i18n.R as I18N

@HiltWorker
class PermanentlyDeleteFileNodesWorker @AssistedInject constructor(
    private val driveTrashRepository: DriveTrashRepository,
    private val linkRepository: LinkRepository,
    private val trashRepository: LinkTrashRepository,
    private val broadcastMessages: BroadcastMessages,
    private val updateEventAction: UpdateEventAction,
    linkTrashRepository: LinkTrashRepository,
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : AbstractMultiResponseCoroutineWorker(linkTrashRepository, appContext, params) {

    private val userId = UserId(inputData.getString(KEY_USER_ID) ?: "")
    private val shareId = ShareId(userId, inputData.getString(KEY_SHARE_ID) ?: "")
    override val workId = inputData.getString(KEY_WORK_ID) ?: ""

    override suspend fun executeCall(links: List<Link>): Map<LinkId, DataResult<Unit>> =
        updateEventAction(shareId) {
            driveTrashRepository.deleteItemsFromTrash(shareId, links.ids)
        }

    override suspend fun handleSuccesses(linkIds: List<LinkId>) {
        linkRepository.delete(linkIds)
        broadcastMessages(
            userId = userId,
            message = applicationContext.resources.getQuantityString(
                I18N.plurals.trash_delete_operation_successful_format,
                linkIds.size,
                linkIds.size,
            ),
            type = BroadcastMessage.Type.SUCCESS,
            extra = DeleteFilesExtra(userId, shareId, linkIds)
        )
    }

    override suspend fun handleErrors(linkIds: List<LinkId>, exception: Exception?, message: String?) {
        trashRepository.insertOrUpdateTrashState(linkIds, TrashState.TRASHED)
        broadcastMessages(
            userId = userId,
            message = message ?: applicationContext.getString(I18N.string.trash_error_occurred_deleting_from_trash),
            type = BroadcastMessage.Type.ERROR,
            extra = DeleteFilesExtra(userId, shareId, linkIds, exception ?: RuntimeException(message))
        )
    }

    companion object {

        fun getWorkRequest(
            userId: UserId,
            shareId: ShareId,
            workId: String,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(PermanentlyDeleteFileNodesWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putString(KEY_SHARE_ID, shareId.id)
                        .putString(KEY_WORK_ID, workId)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }
}
