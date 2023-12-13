/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.upload.data.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import me.proton.core.drive.base.domain.log.LogTag.UploadTag.logTag
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class UploadErrorManagerImpl @Inject constructor() : UploadErrorManager{

    private val _errors = MutableSharedFlow<UploadErrorManager.Error>()
    override val errors: Flow<UploadErrorManager.Error> = _errors
    override suspend fun post(error: UploadErrorManager.Error) {
        CoreLogger.d(
            error.uploadFileLink.id.logTag(),
            error.throwable,
            "Posting error for: ${error.uploadFileLink.uriString} with tags: ${error.tags}"
        )
        _errors.emit(error)
    }
}


