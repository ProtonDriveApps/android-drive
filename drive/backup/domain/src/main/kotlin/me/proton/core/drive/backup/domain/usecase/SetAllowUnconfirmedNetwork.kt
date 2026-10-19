/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.drive.backup.domain.entity.BackupConfiguration
import me.proton.core.drive.backup.domain.entity.BackupNetworkType
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

class SetAllowUnconfirmedNetwork @Inject constructor(
    private val getConfiguration: GetConfiguration,
    private val updateConfiguration: UpdateConfiguration,
) {
    suspend operator fun invoke(folderId: FolderId, allow: Boolean) = coRunCatching {
        val configuration = getConfiguration(folderId).firstOrNull() ?: BackupConfiguration(
            folderId = folderId,
            networkType = BackupNetworkType.UNMETERED,
        )
        val networkType = when (configuration.networkType) {
            BackupNetworkType.ANY -> BackupNetworkType.ANY
            BackupNetworkType.UNMETERED,
            BackupNetworkType.UNCONFIRMED,
            -> if (allow) BackupNetworkType.UNCONFIRMED else BackupNetworkType.UNMETERED
        }
        updateConfiguration(configuration.copy(networkType = networkType)).getOrThrow()
    }
}
