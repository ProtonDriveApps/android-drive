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
package me.proton.core.drive.base.data.usecase

import android.app.ActivityManager
import me.proton.core.drive.base.domain.entity.MemoryInfo
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.usecase.GetMemoryInfo
import javax.inject.Inject

class GetMemoryInfoImpl @Inject constructor(
    private val activityManager: ActivityManager,
) : GetMemoryInfo {
    override operator fun invoke(): Result<MemoryInfo> = runCatching {
        with(
            ActivityManager.MemoryInfo().also { memoryInfo ->
                activityManager.getMemoryInfo(memoryInfo)
            }
        ) {
            MemoryInfo(
                isLowOnMemory = lowMemory,
                memoryClass = (activityManager.memoryClass * 1024 * 2024).bytes
            )
        }
    }
}
