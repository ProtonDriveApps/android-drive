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

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import me.proton.core.drive.base.domain.usecase.IsIgnoringBatteryOptimizations
import me.proton.core.presentation.app.AppLifecycleProvider
import javax.inject.Inject

class IsIgnoringBatteryOptimizationsImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appLifecycleProvider: AppLifecycleProvider,
) : IsIgnoringBatteryOptimizations {

    private val powerManager by lazy {
        appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val flow = channelFlow {
        appLifecycleProvider.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            channel.send(powerManager.isIgnoringBatteryOptimizations(appContext.packageName))
        }
    }

    override operator fun invoke(): Flow<Boolean> = flow
}
