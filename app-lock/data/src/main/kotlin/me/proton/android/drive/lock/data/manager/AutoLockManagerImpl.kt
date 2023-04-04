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

import android.os.Build
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import me.proton.android.drive.lock.data.worker.AppLockWorker
import me.proton.android.drive.lock.domain.manager.AutoLockManager
import me.proton.android.drive.lock.domain.usecase.GetAutoLockDuration
import me.proton.android.drive.lock.domain.usecase.LockApp
import javax.inject.Inject

class AutoLockManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val lockApp: LockApp,
    private val getAutoLockDuration: GetAutoLockDuration,
) : AutoLockManager {

    override suspend fun autoLock() {
        val lockAfter = getAutoLockDuration().first()
        if (lockAfter.inWholeSeconds == 0L || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            lockApp()
        } else {
            workManager.enqueue(
                AppLockWorker.getWorkRequest(lockAfter, listOf(TAG))
            )
        }
    }

    override fun cancelAutoLock() {
        workManager.cancelAllWorkByTag(TAG)
    }

    companion object {
        private const val TAG = "auto-lock"
    }
}
