/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.upload.data.extension

import androidx.work.CoroutineWorker
import androidx.work.Data
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_SIZE

internal fun CoroutineWorker.getSizeData(size: Long) =
    Data.Builder().putLong(KEY_SIZE, size).build()

internal suspend fun CoroutineWorker.setSize(size: Long) = setProgress(getSizeData(size))
