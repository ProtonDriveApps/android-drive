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
package me.proton.android.drive.lock.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import me.proton.android.drive.lock.data.db.AppLockDatabase
import me.proton.android.drive.lock.data.db.entity.AutoLockDurationEntity
import me.proton.android.drive.lock.data.db.entity.EnableAppLockEntity
import me.proton.android.drive.lock.data.extension.toAppLock
import me.proton.android.drive.lock.data.extension.toAppLockEntity
import me.proton.android.drive.lock.data.extension.toLock
import me.proton.android.drive.lock.data.extension.toLockEntity
import me.proton.android.drive.lock.domain.entity.AppLock
import me.proton.android.drive.lock.domain.entity.AppLockType
import me.proton.android.drive.lock.domain.entity.LockKey
import me.proton.android.drive.lock.domain.repository.AppLockRepository
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AppLockRepositoryImpl @Inject constructor(
    private val db: AppLockDatabase,
) : AppLockRepository {

    override fun hasAppLockKeyFlow(): Flow<Boolean> = db.appLockDao.hasAppLockFlow()
    override suspend fun hasAppLockKey(): Boolean = db.appLockDao.hasAppLock()
    override suspend fun getAppLockKey(): AppLock = db.appLockDao.getAppLock().toAppLock()
    override suspend fun insertAppLockKey(appLock: AppLock) = db.appLockDao.insertOrUpdate(appLock.toAppLockEntity())
    override suspend fun deleteAppLockKey() = db.appLockDao.deleteAppLock()

    override suspend fun hasLockKey(lockType: AppLockType): Boolean = db.lockDao.hasLock(lockType)
    override suspend fun getLockKey(lockType: AppLockType): LockKey = db.lockDao.getLock(lockType).toLock()
    override suspend fun insertLockKey(lockKey: LockKey) = db.lockDao.insertOrUpdate(lockKey.toLockEntity())
    override suspend fun deleteLockKey(lockType: AppLockType) = db.lockDao.deleteLock(lockType)

    override suspend fun hasAutoLockDuration(): Boolean = db.autoLockDurationDao.hasAutoLockDuration(
        AUTO_LOCK_DURATION_KEY
    )

    override fun getAutoLockDuration(): Flow<Duration> = db.autoLockDurationDao.getAutoLockDuration(
        AUTO_LOCK_DURATION_KEY
    ).filterNotNull().map { autoLockDurationEntity ->
        autoLockDurationEntity.durationInSeconds.seconds
    }

    override suspend fun insertOrUpdateAutoLockDuration(duration: Duration) = db.autoLockDurationDao.insertOrUpdate(
        AutoLockDurationEntity(
            key = AUTO_LOCK_DURATION_KEY,
            durationInSeconds = duration.inWholeSeconds,
        )
    )

    override suspend fun hasEnableAppLockTimestamp(): Boolean = db.enableAppLockDao.hasEnableAppLock(ENABLE_LOCK_KEY)

    override suspend fun insertOrUpdateEnableAppLockTimestamp(timestamp: Long) = db.enableAppLockDao.insertOrUpdate(
        EnableAppLockEntity(
            key = ENABLE_LOCK_KEY,
            timestamp = timestamp,
        )
    )

    companion object {
        private const val AUTO_LOCK_DURATION_KEY = "auto-lock"
        private const val ENABLE_LOCK_KEY = "enable-lock"
    }
}
