/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.share.user.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.usecase.ConvertExternalInvitation
import me.proton.core.drive.worker.data.LimitedRetryCoroutineWorker
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run

@HiltWorker
class ConvertExternalInvitationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    canRun: CanRun,
    run: Run,
    done: Done,
    private val convertExternalInvitation: ConvertExternalInvitation,
) : LimitedRetryCoroutineWorker(
    appContext,
    workerParams,
    canRun,
    run,
    done,
) {

    override val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val linkId = FileId(shareId, requireNotNull(inputData.getString(KEY_LINK_ID)))
    private val id = requireNotNull(inputData.getString(KEY_ID))
    override val logTag: String = SHARING

    override suspend fun doLimitedRetryWork(): Result = convertExternalInvitation(linkId, id).fold(
        onSuccess = { Result.success() },
        onFailure = { error ->
            if (error.isRetryable) {
                error.log(logTag, "Cannot convert external invitation: $id, will retry", WARNING)
                Result.retry()
            } else {
                error.log(logTag, "Cannot convert external invitation: $id")
                Result.failure()
            }
        }
    )

    companion object {

        private const val KEY_USER_ID = "userId"
        private const val KEY_SHARE_ID = "shareId"
        private const val KEY_LINK_ID = "linkId"
        private const val KEY_ID = "id"

        fun getWorkRequest(
            linkId: LinkId,
            id: String,
            tags: Collection<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(ConvertExternalInvitationWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    workDataOf(
                        linkId = linkId,
                        id = id,
                    )
                )
                .addTags(listOf(linkId.userId.id, id) + tags)
                .build()

        internal fun workDataOf(
            linkId: LinkId,
            id: String,
        ) = Data.Builder()
            .putString(KEY_USER_ID, linkId.userId.id)
            .putString(KEY_SHARE_ID, linkId.shareId.id)
            .putString(KEY_LINK_ID, linkId.id)
            .putString(KEY_ID, id)
            .build()
    }
}
