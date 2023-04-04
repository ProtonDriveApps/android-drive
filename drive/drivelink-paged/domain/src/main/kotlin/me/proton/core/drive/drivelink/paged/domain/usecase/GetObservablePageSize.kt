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
package me.proton.core.drive.drivelink.paged.domain.usecase

import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetMemoryInfo
import javax.inject.Inject

class GetObservablePageSize @Inject constructor(
    private val getMemoryInfo: GetMemoryInfo,
    private val configurationProvider: ConfigurationProvider,
) {
    operator fun invoke(): Int {
        val memoryInfo = getMemoryInfo().getOrNull()
        return if (memoryInfo == null || memoryInfo.isLowOnMemory) {
            configurationProvider.dbPageSize
        } else {
            val multiplier = when {
                memoryInfo.memoryClass > 512.MiB -> 4
                memoryInfo.memoryClass > 256.MiB -> 3
                memoryInfo.memoryClass > 128.MiB -> 2
                else -> 1
            }
            configurationProvider.dbPageSize * multiplier
        }
    }
}
