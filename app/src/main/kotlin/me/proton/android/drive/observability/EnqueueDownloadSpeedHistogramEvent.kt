/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.observability

import me.proton.core.drive.announce.event.domain.extension.toKibibytesPerSecond
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.domain.log.LogTag.TRACKING
import me.proton.core.drive.observability.domain.metrics.DownloadSpeedHistogram
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.presentation.app.AppLifecycleObserver
import me.proton.core.presentation.app.AppLifecycleProvider.State.Foreground
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class EnqueueDownloadSpeedHistogramEvent @Inject constructor(
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
    private val appLifecycleObserver: AppLifecycleObserver,
) {
    suspend operator fun invoke(event: Event.DownloadSpeed) {
        val kibibytesPerSecond = event.toKibibytesPerSecond()
        CoreLogger.d(
            TRACKING,
            "Download speed $kibibytesPerSecond KiB/s; " +
                    "bytes(min): ${event.bytes.value}, " +
                    "elapsed(ms): ${event.elapsedTime.value}"
        )
        enqueueObservabilityEvent(
            DownloadSpeedHistogram(
                Labels = DownloadSpeedHistogram.LabelsData(
                    context = if (appLifecycleObserver.state.value == Foreground) {
                        DownloadSpeedHistogram.Context.foreground
                    } else {
                        DownloadSpeedHistogram.Context.background
                    }
                ),
                Value = kibibytesPerSecond
            )
        )
    }
}
