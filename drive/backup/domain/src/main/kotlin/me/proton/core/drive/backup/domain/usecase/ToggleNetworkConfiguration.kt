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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.drive.backup.domain.entity.BackupConfiguration
import me.proton.core.drive.backup.domain.entity.BackupNetworkType
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

class ToggleNetworkConfiguration @Inject constructor(
    private val getConfiguration: GetConfiguration,
    private val updateConfiguration: UpdateConfiguration,
) {
    suspend operator fun invoke(
        folderId: FolderId,
        defaultConfiguration: (FolderId) -> BackupConfiguration? = { null },
    ) = coRunCatching {
        (getConfiguration(folderId).firstOrNull()
            ?: defaultConfiguration(folderId))?.let { configuration ->
            updateConfiguration(
                configuration.copy(
                    networkType = when (configuration.networkType) {
                        BackupNetworkType.CONNECTED -> BackupNetworkType.UNMETERED
                        BackupNetworkType.UNMETERED -> BackupNetworkType.CONNECTED
                    }
                )
            ).getOrThrow()
        }
    }
}
