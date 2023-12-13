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

package me.proton.android.drive.usecase

import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteFullException
import kotlinx.coroutines.runBlocking
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.domain.usecase.ClearCacheFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import java.io.IOException
import javax.inject.Inject

class HandleUncaughtException @Inject constructor(
    private val getInternalStorageInfo: GetInternalStorageInfo,
    private val clearCacheFolder: ClearCacheFolder,
    private val announceEvent: AnnounceEvent,
) {

    operator fun invoke(error: Throwable): Result<Boolean> = coRunCatching {
        val isNoSpaceLeftOnDevice = getInternalStorageInfo().getOrThrow().available.value == 0L
        if (isNoSpaceLeftOnDevice) runBlocking {
            announceEvent(Event.NoSpaceLeftOnDevice)
            clearCacheFolder()
        }
        when (error) {
            is IOException,
            is SQLiteDiskIOException,
            is SQLiteFullException -> isNoSpaceLeftOnDevice
            else -> false
        }
    }
}
