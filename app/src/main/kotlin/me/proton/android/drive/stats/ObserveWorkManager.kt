/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.stats

import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import javax.inject.Inject

class ObserveWorkManager @Inject constructor(
    private val workManager: WorkManager,
    private val configurationProvider: ConfigurationProvider,
) {
    private val tickerFlow = flow {
        while (true) {
            emit(Unit)
            delay(configurationProvider.observeWorkManagerInterval)
        }
    }

    operator fun invoke(userId: UserId) = tickerFlow.transform {
        val workInfos = workManager.getWorkInfosByTagFlow(userId.id).firstOrNull() ?: emptyList<WorkInfo>()
        workInfos
            .filterNotNull()
            .takeIf { it.isNotEmpty() }
            ?.let { infos ->
                emit(
                    Event.Workers(
                        infos.mapIndexed { index, workInfo ->
                            Event.Workers.Infos(
                                id = workInfo.id,
                                name = workInfo.tags.firstOrNull { tag ->
                                    tag.contains("Worker")
                                } ?: "no-name-$index",
                                state = when (workInfo.state) {
                                    WorkInfo.State.ENQUEUED -> Event.Workers.Infos.State.ENQUEUED
                                    WorkInfo.State.RUNNING -> Event.Workers.Infos.State.RUNNING
                                    WorkInfo.State.SUCCEEDED -> Event.Workers.Infos.State.SUCCEEDED
                                    WorkInfo.State.FAILED -> Event.Workers.Infos.State.FAILED
                                    WorkInfo.State.BLOCKED -> Event.Workers.Infos.State.BLOCKED
                                    WorkInfo.State.CANCELLED -> Event.Workers.Infos.State.CANCELLED
                                },
                                attempts = workInfo.runAttemptCount,
                            )
                        }
                    )
                )
            }
    }.distinctUntilChanged()
}
