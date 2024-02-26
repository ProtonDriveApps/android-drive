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

package me.proton.core.drive.base.presentation.usecase

import android.app.usage.UsageEvents
import android.app.usage.UsageEvents.Event.STANDBY_BUCKET_CHANGED
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.STANDBY_BUCKET_ACTIVE
import android.app.usage.UsageStatsManager.STANDBY_BUCKET_RARE
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import me.proton.core.drive.base.domain.usecase.IsBackgroundRestricted
import me.proton.core.presentation.app.AppLifecycleProvider
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class IsBackgroundRestrictedImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appLifecycleProvider: AppLifecycleProvider,
) : IsBackgroundRestricted {

    private val usageStatsManager by lazy {
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val powerManager by lazy {
        appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val flow = channelFlow {
        appLifecycleProvider.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val isLimited = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                !powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
                        && usageStatsManager.isLimitedByAppStandbyBucket()
            } else {
                false
            }
            channel.send(isLimited)
        }
    }.stateIn(appLifecycleProvider.lifecycle.coroutineScope, SharingStarted.WhileSubscribed(), false)

    @RequiresApi(Build.VERSION_CODES.P)
    private fun UsageStatsManager.isLimitedByAppStandbyBucket(): Boolean {
        var maxBucket = 0
        return queryEventsForSelf(
            System.currentTimeMillis() - THREE_DAYS,
            System.currentTimeMillis()
        )?.let { queryEventsForSelf ->
            val event = UsageEvents.Event()
            while (queryEventsForSelf.hasNextEvent()) {
                queryEventsForSelf.getNextEvent(event)
                if (event.eventType == STANDBY_BUCKET_CHANGED) {
                    maxBucket = maxOf(maxBucket, event.appStandbyBucket)
                }
            }
            if (appStandbyBucket < STANDBY_BUCKET_ACTIVE) {
                false
            } else {
                maxBucket >= STANDBY_BUCKET_RARE
            }
        } ?: false
    }

    override operator fun invoke(): Flow<Boolean> = flow

    private companion object {
        val THREE_DAYS = 3.days.inWholeMilliseconds
    }
}
