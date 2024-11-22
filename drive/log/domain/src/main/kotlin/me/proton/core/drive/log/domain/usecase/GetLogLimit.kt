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

package me.proton.core.drive.log.domain.usecase

import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetInternalStorageInfo
import javax.inject.Inject

class GetLogLimit @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val getInternalStorageInfo: GetInternalStorageInfo,
) {
    operator fun invoke(): Int {
        val storageInfo = getInternalStorageInfo().getOrNull(LogTag.LOG, "Getting internal storage info failed")
        val availableBytes = storageInfo?.available ?: 0.bytes
        return when (availableBytes.value) {
            in 0..configurationProvider.cacheInternalStorageLimit.value -> configurationProvider.logDbMinLimit
            else -> configurationProvider.logDbLimit
        }
    }
}
