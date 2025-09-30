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

package me.proton.core.drive.drivelink.download.domain.manager

import kotlinx.coroutines.CancellationException
import kotlin.coroutines.CoroutineContext

interface PipelineManager<T : PipelineManager.Task> {
    suspend fun start(
        taskProvider: TaskProvider<T>,
        coroutineContext: CoroutineContext,
    ): Result<Unit>
    suspend fun startPipelines(): Result<Unit>
    suspend fun stopPipelines(immediately: Boolean = false, cause: CancellationException? = StopCancelledException())
    suspend fun stopPipeline(pipelineId: Long, cause: CancellationException? = StopCancelledException())
    suspend fun stop(): Result<Unit>

    interface Task {
        suspend operator fun invoke(isCancelled: () -> Boolean)
    }

    interface TaskProvider<T : Task> {
        suspend fun getNextTask(pipelineId: Long): Result<T>
        suspend fun taskCancelled(task: T, isCancelledByStop: Boolean = false)
        suspend fun taskCompleted(task: T, throwable: Throwable? = null)
    }

    class StopCancelledException : CancellationException()
}
