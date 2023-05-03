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

package me.proton.core.drive.worker.data.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.worker.data.db.WorkerDatabase
import me.proton.core.drive.worker.data.extension.toWorkerRun
import me.proton.core.drive.worker.data.extension.toWorkerRunEntity
import me.proton.core.drive.worker.domain.entity.WorkerRun
import me.proton.core.drive.worker.domain.repository.WorkerRepository
import javax.inject.Inject

class WorkerRepositoryImpl @Inject constructor(
    db: WorkerDatabase,
) : WorkerRepository {
    private val workerDao = db.workerDao

    override suspend fun getAllWorkerRuns(userId: UserId, workerId: String): List<WorkerRun> =
        workerDao.getAll(userId, workerId).map { workerRunEntity -> workerRunEntity.toWorkerRun() }

    override suspend fun insertWorkerRun(workerRun: WorkerRun) = workerDao.insertOrIgnore(workerRun.toWorkerRunEntity())

    override suspend fun deleteAllWorkerRuns(userId: UserId, workerId: String) = workerDao.deleteAll(userId, workerId)

}
