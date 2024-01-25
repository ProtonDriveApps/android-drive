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

import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupConfiguration
import me.proton.core.drive.backup.domain.repository.BackupConfigurationRepository
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import javax.inject.Inject

class ObserveConfigurationChanges @Inject constructor(
    private val backupConfigurationRepository: BackupConfigurationRepository,
    private val onConfigurationChanged: OnConfigurationChanged,
) {
    operator fun invoke(userId: UserId) =
        backupConfigurationRepository.getAll(userId).distinctUntilChanged()
            .mapWithPrevious { previous, configurations ->
                configurations.onEach { configuration ->
                    previous?.firstOrNull { it.folderId == configuration.folderId }
                        ?.let { previousConfiguration ->
                            compare(
                                previousConfiguration = previousConfiguration,
                                configuration = configuration
                            )
                        }
                }
            }

    private suspend fun compare(
        previousConfiguration: BackupConfiguration,
        configuration: BackupConfiguration,
    ) {
        if (previousConfiguration != configuration) {
            onConfigurationChanged(previousConfiguration, configuration).getOrThrow()
        }
    }
}
