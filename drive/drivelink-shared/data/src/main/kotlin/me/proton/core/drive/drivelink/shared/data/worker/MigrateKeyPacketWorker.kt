/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.drivelink.shared.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.drivelink.shared.domain.usecase.MigrateKeyPacket
import me.proton.core.util.kotlin.CoreLogger

@HiltWorker
class MigrateKeyPacketWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val migrateKeyPacket: MigrateKeyPacket,
) : CoroutineWorker(appContext, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    override suspend fun doWork(): Result {
        CoreLogger.d(LogTag.SHARING, "MigrateKeyPacketWorker: Starting migration")
        return migrateKeyPacket(userId).fold(
            onFailure = { error ->
                error.log(LogTag.SHARING, "Cannot migrate key packet")
                if (error.isRetryable) Result.retry() else Result.failure()
            },
            onSuccess = {
                Result.success()
            }
        )
    }

    companion object {
        private const val KEY_USER_ID = "USER_ID"
        fun getWorkRequest(
            userId: UserId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(MigrateKeyPacketWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    workDataOf(userId)
                )
                .addTags(listOf(userId.id) + tags)
                .build()

        internal fun workDataOf(userId: UserId) = Data.Builder()
            .putString(KEY_USER_ID, userId.id)
            .build()
    }
}
