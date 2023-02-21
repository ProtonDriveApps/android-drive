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

import androidx.work.ListenableWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.upload.data.exception.UploadCleanupException
import me.proton.core.drive.upload.data.worker.UploadCoroutineWorker

@ExperimentalCoroutinesApi
fun UploadCoroutineWorker.retryOrAbort(
    isRetryable: Boolean,
    error: Throwable,
    fileName: String,
): ListenableWorker.Result =
    if (isRetryable) ListenableWorker.Result.retry() else throw UploadCleanupException(error, fileName)
