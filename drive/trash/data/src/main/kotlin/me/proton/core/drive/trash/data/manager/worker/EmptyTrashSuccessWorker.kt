/*
 * Copyright (c) 2023-2024 Proton AG.
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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.i18n.R
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.trash.data.manager.worker.WorkerKeys.KEY_USER_ID

@HiltWorker
class EmptyTrashSuccessWorker @AssistedInject constructor(
    private val broadcastMessages: BroadcastMessages,
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val userId = UserId(inputData.getString(KEY_USER_ID) ?: "")

    override suspend fun doWork(): Result {
        broadcastMessages(
            userId = userId,
            message = applicationContext.getString(R.string.trash_empty_operation_successful),
            type = BroadcastMessage.Type.SUCCESS,
        )
        return Result.success()
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest = OneTimeWorkRequest.Builder(EmptyTrashSuccessWorker::class.java)
            .setInputData(
                workDataOf(userId)
            )
            .addTags(listOf(userId.id) + tags)
            .build()

        fun workDataOf(userId: UserId) = Data.Builder()
            .putString(KEY_USER_ID, userId.id)
            .build()
    }
}
