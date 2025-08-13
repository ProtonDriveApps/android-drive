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

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.download.domain.entity.Pipeline
import me.proton.core.drive.drivelink.download.domain.manager.PipelineManager
import me.proton.core.util.kotlin.CoreLogger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class PipelineManagerImpl<T : PipelineManager.Task>(
    private val maxNumberOfPipelines: Int,
    private val logTag: String,
) : PipelineManager<T> {
    private val mutex = Mutex()
    private var scope: CoroutineScope? = null
    private var taskProvider: PipelineManager.TaskProvider<T>? = null
    private var runningPipelines = mutableSetOf<Pipeline>()
    private var lastPipelineId: Long = 0L
    private var canRun: Boolean = true

    override suspend fun start(
        taskProvider: PipelineManager.TaskProvider<T>,
        coroutineContext: CoroutineContext,
    ) = coRunCatching {
        mutex.withLock {
            require(this.scope == null) {
                "Call start only once or call stop before calling start"
            }
            CoreLogger.d(
                tag = logTag,
                message = "PipelineManager start",
            )
            scope = coroutineContext.supervisorJobScope()
            this.taskProvider = taskProvider
        }
        startPipelines()
            .onFailure { error ->
                error.log(logTag, "Starting pipelines failed")
            }
            .getOrThrow()
    }

    override suspend fun stop() = coRunCatching {
        CoreLogger.d(
            tag = logTag,
            message = "PipelineManager stop",
        )
        mutex.withLock {
            val scope = this.scope
            this.taskProvider = null
            this.scope = null
            scope?.cancel()
        }
        Unit
    }

    override suspend fun startPipelines() = coRunCatching {
        mutex.withLock {
            val scope = requireNotNull(this.scope) { "Cannot start pipelines without a scope" }
            val taskProvider = requireNotNull(this.taskProvider) {
                "Cannot start pipelines without a task provider"
            }
            canRun = true
            val pipelinesCount = (maxNumberOfPipelines - runningPipelines.prune().size)
                .coerceIn(0, Int.MAX_VALUE)
            repeat(pipelinesCount) {
                val pipelineId = lastPipelineId++
                scope.launch {
                    CoreLogger.d(
                        tag = logTag,
                        message = "Pipeline ($pipelineId) started",
                    )
                    while (isActive && canRun) {
                        taskProvider.getNextTask(pipelineId)
                            .getOrNull()
                            ?.let { task ->
                                try {
                                    CoreLogger.d(logTag, "Pipeline ($pipelineId) task started")
                                    task.invoke { !isActive }
                                    CoreLogger.d(logTag, "Pipeline ($pipelineId) task finished (isActive=$isActive, canRun=$canRun)")
                                    taskProvider.taskCompleted(task)
                                } catch (e: CancellationException) {
                                    CoreLogger.d(
                                        tag = logTag,
                                        message = "Pipeline ($pipelineId) cancelled (isCancelledByStop=${e is PipelineManager.StopCancelledException})",
                                    )
                                    taskProvider.taskCancelled(task, e is PipelineManager.StopCancelledException)
                                    throw e
                                } catch (e: Throwable) {
                                    taskProvider.taskCompleted(task, e)
                                }
                            }
                            ?: break
                    }
                    CoreLogger.d(
                        tag = logTag,
                        message = "Pipeline ($pipelineId) finished",
                    )
                }.also { job ->
                    runningPipelines.add(Pipeline(pipelineId, job))
                }
            }
        }
    }

    override suspend fun stopPipelines(immediately: Boolean) = mutex.withLock {
        if (immediately) {
            runningPipelines.forEach { pipeline -> pipeline.job.cancel(PipelineManager.StopCancelledException()) }
        } else {
            canRun = false
        }
    }

    override suspend fun stopPipeline(pipelineId: Long) = mutex.withLock {
        runningPipelines
            .prune()
            .firstOrNull { pipeline -> pipeline.id == pipelineId }
            ?.job?.cancel(PipelineManager.StopCancelledException()) ?: Unit
    }

    @VisibleForTesting
    fun getRunningPipelines(shouldPrune: Boolean = false): Set<Pipeline> =
        if (shouldPrune) runningPipelines.prune() else runningPipelines

    private fun MutableSet<Pipeline>.prune() = this.apply {
        removeIf { pipeline -> pipeline.job.isActive.not() }
    }

    private fun CoroutineContext.supervisorJobScope() = CoroutineScope(
        context = this + SupervisorJob(this[Job])
    )
}
