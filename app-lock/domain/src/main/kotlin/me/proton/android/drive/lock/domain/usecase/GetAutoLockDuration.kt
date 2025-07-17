/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.lock.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.android.drive.lock.domain.extension.autoLockDefaultDuration
import me.proton.android.drive.lock.domain.repository.AppLockRepository
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import javax.inject.Inject
import kotlin.time.Duration

class GetAutoLockDuration @Inject constructor(
    private val appLockRepository: AppLockRepository,
    private val configurationProvider: ConfigurationProvider,
) {
    operator fun invoke(): Flow<Duration> = flow {
        if (appLockRepository.hasAutoLockDuration().not()) {
            emit(configurationProvider.autoLockDefaultDuration)
        }
        emitAll(appLockRepository.getAutoLockDuration())
    }
}
