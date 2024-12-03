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

package me.proton.core.drive.worker.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.worker.domain.repository.WorkerRepository
import javax.inject.Inject

class CanRun @Inject constructor(
    private val workerRepository: WorkerRepository,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(userId: UserId, workerId: String): Result<Boolean> = coRunCatching {
        invoke(workerRepository.getAllWorkerRuns(userId, workerId).size).getOrThrow()
    }

    operator fun invoke(attempt: Int): Result<Boolean> = Result.success(
        attempt < configurationProvider.maxApiAutoRetries
    )
}
