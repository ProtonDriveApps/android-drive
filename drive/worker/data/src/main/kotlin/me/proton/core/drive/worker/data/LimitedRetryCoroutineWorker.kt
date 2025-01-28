/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.worker.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
import me.proton.core.util.kotlin.CoreLogger

abstract class LimitedRetryCoroutineWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val canRun: CanRun,
    private val run: Run,
    private val done: Done,
) : CoroutineWorker(context, workerParams) {
    protected abstract val userId: UserId
    protected abstract val logTag: String

    final override suspend fun doWork(): Result {
        if (isLimitOverreached()) {
            CoreLogger.w(logTag, "Max retries already reached, hard stop")
            return Result.failure()
        }
        try {
            run(userId, id.toString())
            return when (val result = doLimitedRetryWork()) {
                is Result.Retry -> if (canRetry()){
                    result
                } else {
                    CoreLogger.i(logTag, "Max retries reached, giving up")
                    done()
                    Result.failure()
                }
                else -> {
                    done()
                    result
                }
            }
        } catch (e: Exception) {
            done()
            throw e
        }
    }

    abstract suspend fun doLimitedRetryWork(): Result

    suspend fun canRetry(): Boolean = canRun(userId, id.toString()).getOrNull(logTag) ?: true

    private fun isLimitOverreached() = canRun(runAttemptCount - 1).getOrNull(logTag) != true

    private suspend fun done() = done(userId, id.toString())
}
