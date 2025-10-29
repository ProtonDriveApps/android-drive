/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.log.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.function.processPagedList
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.log.data.db.LogDatabase
import me.proton.core.drive.log.data.db.entity.LogLevelEntity
import me.proton.core.drive.log.data.db.entity.LogOriginEntity
import me.proton.core.drive.log.data.extension.toLog
import me.proton.core.drive.log.data.extension.toLogEntity
import me.proton.core.drive.log.domain.entity.Log
import me.proton.core.drive.log.domain.repository.LogRepository
import me.proton.core.drive.log.domain.usecase.GetLogLimit
import javax.inject.Inject

class LogRepositoryImpl @Inject constructor(
    private val db: LogDatabase,
    private val configurationProvider: ConfigurationProvider,
    private val getLogLimit: GetLogLimit,
) : LogRepository {

    override suspend fun insert(logs: List<Log>) = db.inTransaction {
        if (logs.size == 1) {
            val log = logs.first()
            db.logDao.insertWithLimit(
                userId = log.userId,
                entity = log.toLogEntity(),
                limit = getLogLimit(),
            )
        } else {
            db.logDao.insertOrIgnore(*logs.map { it.toLogEntity() }.toTypedArray())
            logs.firstOrNull()?.let { log ->
                db.logDao.dropOldRowsToFitLimit(userId = log.userId, limit = getLogLimit())
            }
        }
        Unit
    }

    override suspend fun processAllLogs(userId: UserId, block: suspend (List<Log>) -> Unit) = db.inTransaction {
        processPagedList(
            pageSize = configurationProvider.dbPageSize,
            page = { fromIndex, count ->
                db.logDao.getLogs(userId, count, fromIndex)
            },
            block = { logEntities ->
                block(
                    logEntities.map { logEntity -> logEntity.toLog() }
                )
            },
        )
    }

    override suspend fun toggleLogLevel(userId: UserId, level: Log.Level) = db.inTransaction {
        if (db.logLevelDao.hasLogLevel(userId, level)) {
            db.logLevelDao.delete(userId, level)
        } else {
            db.logLevelDao.insertOrIgnore(
                LogLevelEntity(
                    userId = userId,
                    level = level,
                )
            )
        }
    }

    override fun getAllDeselectedLogLevels(userId: UserId, count: Int): Flow<Set<Log.Level>> =
        db.logLevelDao.getAll(userId, count).map { logLevelEntities ->
            logLevelEntities.map { logLevelEntity -> logLevelEntity.level }.toSet()
        }

    override suspend fun toggleLogOrigin(userId: UserId, origin: Log.Origin) = db.inTransaction {
        if (db.logOriginDao.hasLogOrigin(userId, origin)) {
            db.logOriginDao.delete(userId, origin)
        } else {
            db.logOriginDao.insertOrIgnore(
                LogOriginEntity(
                    userId = userId,
                    origin = origin,
                )
            )
        }
    }

    override fun getAllDeselectedLogOrigins(userId: UserId, count: Int): Flow<Set<Log.Origin>> =
        db.logOriginDao.getAll(userId, count).map { logOriginEntities ->
            logOriginEntities.map { logOriginEntity -> logOriginEntity.origin }.toSet()
        }

    override suspend fun clearLogs(userId: UserId) = db.inTransaction {
        db.logDao.deleteAll(userId)
        db.logLevelDao.deleteAll(userId)
        db.logOriginDao.deleteAll(userId)
    }
}
