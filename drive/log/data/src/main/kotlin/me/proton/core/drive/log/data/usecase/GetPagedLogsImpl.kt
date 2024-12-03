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

package me.proton.core.drive.log.data.usecase

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.log.data.db.LogDatabase
import me.proton.core.drive.log.data.extension.toLog
import me.proton.core.drive.log.domain.entity.Log
import me.proton.core.drive.log.domain.usecase.GetPagedLogs
import javax.inject.Inject

class GetPagedLogsImpl @Inject constructor(
    private val db: LogDatabase,
    private val configurationProvider: ConfigurationProvider,
) : GetPagedLogs {
    override fun invoke(userId: UserId): Flow<PagingData<Log>> =
        Pager(
            PagingConfig(
                pageSize = configurationProvider.uiPageSize,
                enablePlaceholders = true,
            ),
            pagingSourceFactory = {
                db.logDao.getLogsPagingSource(userId = userId)
            }
        )
            .flow
            .map { pagingData ->
                pagingData.map { logEntity -> logEntity.toLog() }
            }
}
