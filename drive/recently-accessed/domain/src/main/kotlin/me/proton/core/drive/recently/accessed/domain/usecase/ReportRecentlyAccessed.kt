/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.recently.accessed.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toApiException
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.IsRetryable
import me.proton.core.drive.base.domain.util.coRunCatchingException
import me.proton.core.drive.recently.accessed.domain.repository.RecentlyAccessedRepository
import me.proton.core.drive.recently.accessed.domain.reporter.RecentlyAccessedReporter
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.ProtonDriveSdkException
import javax.inject.Inject

class ReportRecentlyAccessed @Inject constructor(
    private val repository: RecentlyAccessedRepository,
    private val reporter: RecentlyAccessedReporter,
    private val configurationProvider: ConfigurationProvider,
    private val isRetryable: IsRetryable,
) {

    suspend operator fun invoke(userId: UserId) = coRunCatchingException {
        while (true) {
            val batch = repository.getAll(userId, configurationProvider.dbPageSize)
            if (batch.isEmpty()) {
                break
            }
            batch.groupBy { item -> item.volumeId }.forEach { (volumeId, items) ->
                reporter.report(userId, volumeId, items).onFailure { error ->
                    if (isRetryable(error) || !error.isClientError) throw error
                    CoreLogger.w(
                        tag = LogTag.DEFAULT,
                        e = error,
                        message = "Dropping ${items.size} recently accessed items rejected by the backend",
                    )
                }
                repository.delete(items)
            }
        }
    }

    private val Throwable.isClientError: Boolean get() {
        val apiException = when (this) {
            is ApiException -> this
            is ProtonDriveSdkException -> toApiException()
            else -> null
        }
        val httpCode = (apiException?.error as? ApiResult.Error.Http)?.httpCode ?: return false
        return httpCode in 400..499
    }
}
