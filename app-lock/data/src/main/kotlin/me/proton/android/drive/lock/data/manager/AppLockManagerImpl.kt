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
package me.proton.android.drive.lock.data.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.android.drive.lock.domain.entity.AppLock
import me.proton.android.drive.lock.domain.entity.SecretKey
import me.proton.android.drive.lock.domain.manager.AppLockManager
import me.proton.android.drive.lock.domain.repository.AppLockRepository
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class AppLockManagerImpl @Inject constructor(
    private val appLockRepository: AppLockRepository,
    coroutineContext: CoroutineContext,
) : AppLockManager {
    private val coroutineScope = CoroutineScope(coroutineContext)
    override val enabled: StateFlow<Boolean> = appLockRepository.hasAppLockKeyFlow()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    private val appKey = MutableStateFlow<SecretKey?>(null)
    private val _locked: Flow<Boolean> = appKey.map { secretKey -> secretKey == null }
    override val locked: StateFlow<Boolean> = combine(enabled, _locked) {
        _, isLocked ->
        appLockRepository.hasAppLockKey() && isLocked
    }.distinctUntilChanged().stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override suspend fun isLocked(): Boolean = isEnabled() && appKey.value == null

    override suspend fun isEnabled(): Boolean = appLockRepository.hasAppLockKey()

    override suspend fun unlock(appKey: SecretKey) {
        this.appKey.value = appKey
    }

    override suspend fun lock() {
        appKey.value = null
    }

    override suspend fun enable(
        secretKey: SecretKey,
        appLock: AppLock,
    ): Result<Boolean> = coRunCatching {
        unlock(secretKey)
        appLockRepository.insertAppLockKey(appLock)
        appLockRepository.insertOrUpdateEnableAppLockTimestamp(System.currentTimeMillis())
        true
    }

    override suspend fun disable(): Result<Boolean> = coRunCatching {
        appLockRepository.deleteAppLockKey()
        lock()
        true
    }
}
