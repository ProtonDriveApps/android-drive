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
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository

abstract class AbstractMultiResponseCoroutineWorker(
    private val repository: LinkTrashRepository,
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    protected abstract val workId: String

    override suspend fun doWork(): Result {
        val links = repository.getLinksAndRemoveWorkFromCache(workId)
        try {
            executeCall(links).onSuccess { successes ->
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
            handleErrors(links.map { link -> link.id }, e, null)
            return Result.failure()
        }
        return Result.success()
    }

    abstract suspend fun handleSuccesses(linkIds: List<LinkId>)

    abstract suspend fun handleErrors(linkIds: List<LinkId>, exception: Exception?, message: String?)

    abstract suspend fun executeCall(links: List<Link>): Map<LinkId, DataResult<Unit>>
}
