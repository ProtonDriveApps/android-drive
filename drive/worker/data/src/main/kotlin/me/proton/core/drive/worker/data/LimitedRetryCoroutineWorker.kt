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
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.WorkInfoStopReason
import me.proton.core.drive.base.data.extension.stopReasonAsEnum
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.worker.data.extension.toSystemConstraint
import me.proton.core.drive.worker.domain.entity.Constraint
import me.proton.core.drive.worker.domain.usecase.WorkerRuns
import me.proton.core.util.kotlin.CoreLogger

abstract class LimitedRetryCoroutineWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val workerRuns: WorkerRuns,
) : CoroutineWorker(context, workerParams) {
    protected abstract val userId: UserId
    protected abstract val logTag: String
    protected open val runtimeConstraints: Flow<Constraint.Runtime> = emptyFlow()

    protected var blockedBy: Constraint.Runtime? = null
        private set

    private val workerId: String get() = id.toString()

    final override suspend fun doWork(): Result {
        var hasStarted = false
        var runId: Long? = null
        try {
            if (isLimitOverreached()) {
                CoreLogger.w(logTag, "Max retries already reached, hard stop")
                onRetriesExhausted()
                return Result.failure()
            }
            hasStarted = true
            runId = withContext(NonCancellable) { workerRuns.run(userId, workerId) }.getOrNull(logTag)
            val outcome = runUntilBlocked(runtimeConstraints.onEach { constraint -> blockedBy = constraint }) {
                doLimitedRetryWork()
            }
            val result = when (outcome) {
                is BlockedOr.Completed -> outcome.result
                is BlockedOr.Blocked -> if (blocked(outcome.by.toString(), runId, outcome.by.isLimited)) {
                    return Result.retry()
                } else {
                    Result.retry()
                }
            }
            return when (result) {
                is Result.Retry -> if (canRetry()){
                    result
                } else {
                    CoreLogger.d(logTag, "Max retries reached, giving up")
                    done()
                    onRetriesExhausted()
                    Result.failure()
                }
                else -> {
                    done()
                    result
                }
            }
        } catch (e: Exception) {
            val blockingStop = blockingStop()
            when {
                blockingStop != null -> blocked(blockingStop.description, runId, blockingStop.isLimited)
                hasStarted -> done()
            }
            throw e
        }
    }

    abstract suspend fun doLimitedRetryWork(): Result

    protected open suspend fun onRetriesExhausted() = Unit

    suspend fun canRetry(): Boolean = withContext(NonCancellable) {
        workerRuns.canRun(userId, workerId).getOrNull(logTag) ?: true
    }

    protected suspend fun canBlock(): Boolean =
        workerRuns.canBlock(userId, workerId).getOrNull(logTag) == true

    private suspend fun isLimitOverreached(): Boolean {
        val blockedRunCount = workerRuns.getBlockedRunCount(userId, workerId).getOrNull(logTag) ?: 0
        return workerRuns.canRun(runAttemptCount - 1 - blockedRunCount).getOrNull(logTag) != true
    }

    private fun blockingStop(): BlockingStop? = when {
        !isStopped -> null
        SDK_INT < VERSION_CODES.S -> BlockingStop(UNKNOWN_STOP_REASON, isLimited = true)
        stopReasonAsEnum == WorkInfoStopReason.CANCELLED_BY_APP -> null
        else -> stopReasonAsEnum.let { reason ->
            val constraint = reason.toSystemConstraint()
            BlockingStop(
                description = constraint?.toString() ?: reason.name,
                isLimited = constraint?.type != Constraint.System.Type.CONNECTIVITY,
            )
        }
    }

    private data class BlockingStop(val description: String, val isLimited: Boolean)

    private suspend fun blocked(
        stop: String,
        runId: Long?,
        isLimited: Boolean = true,
    ): Boolean = withContext(NonCancellable) {
        if (isLimited && workerRuns.canBlock(userId, workerId).getOrNull(logTag) != true) {
            CoreLogger.w(logTag, "Max blocked attempts reached, $stop counts as an attempt (id=$workerId)")
            if (runId == null) {
                workerRuns.run(userId, workerId).getOrNull(logTag)
            }
            return@withContext false
        }
        CoreLogger.d(logTag, "Blocked by $stop (id=$workerId, runAttemptCount=$runAttemptCount)")
        if (runId != null) {
            workerRuns.markRunBlocked(userId, workerId, runId, isLimited).getOrNull(logTag)
        } else {
            workerRuns.run(userId, workerId, isBlocked = true, isLimited = isLimited).getOrNull(logTag)
        }
        true
    }

    private suspend fun done() = withContext(NonCancellable) {
        workerRuns.done(userId, workerId)
    }

    private companion object {
        const val UNKNOWN_STOP_REASON = "unknown stop reason"
    }
}
