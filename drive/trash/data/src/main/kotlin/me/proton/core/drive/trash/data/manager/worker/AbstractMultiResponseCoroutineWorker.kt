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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.data.extension.areRetryable
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.extension.onSuccess
import me.proton.core.drive.base.domain.log.ItemLifecycle
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository

abstract class AbstractMultiResponseCoroutineWorker(
    private val repository: LinkTrashRepository,
    private val itemLifecycle: ItemLifecycle,
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    protected abstract val workId: String

    /** The critical flow whose items this worker finishes (invariant-checks). */
    protected abstract val lifecycleFlow: ItemLifecycle.Flow

    override suspend fun doWork(): Result {
        val links = repository.getLinksAndRemoveWorkFromCache(workId)
        var results: Map<LinkId, DataResult<Unit>>? = null
        try {
            results = executeCall(links).also { response -> logFinished(links, response) }
            results.onSuccess { successes ->
                handleSuccesses(successes.keys.toList())
                return Result.success()
            }.onFailure { errors, successes ->
                if (errors.isNotEmpty()) {
                    handleErrors(
                        linkIds = errors.keys.toList(),
                        exception = if (errors.areRetryable) RuntimeException() else null,
                        message = errors.values.takeIf { errors.size == 1 }?.first()?.message,
                    )
                }
                if (successes.isNotEmpty()) {
                    handleSuccesses(successes.keys.toList())
                }
                return Result.failure()
            }
        } catch (e: Exception) {
            if (results == null) {
                links.forEach { link ->
                    itemLifecycle.finished(lifecycleFlow, link.id.id, ItemLifecycle.Outcome.FAILED)
                }
            }
            handleErrors(links.map { link -> link.id }, e, null)
            return Result.failure()
        }
        return Result.success()
    }

    /** Every requested link ends succeeded or failed; one missing from the results was dropped. */
    private fun logFinished(links: List<Link>, results: Map<LinkId, DataResult<Unit>>) =
        links.forEach { link ->
            val outcome = when (results[link.id]) {
                null -> ItemLifecycle.Outcome.DROPPED
                is DataResult.Success -> ItemLifecycle.Outcome.SUCCEEDED
                else -> ItemLifecycle.Outcome.FAILED
            }
            itemLifecycle.finished(lifecycleFlow, link.id.id, outcome)
        }

    abstract suspend fun handleSuccesses(linkIds: List<LinkId>)

    abstract suspend fun handleErrors(linkIds: List<LinkId>, exception: Exception?, message: String?)

    abstract suspend fun executeCall(links: List<Link>): Map<LinkId, DataResult<Unit>>
}
