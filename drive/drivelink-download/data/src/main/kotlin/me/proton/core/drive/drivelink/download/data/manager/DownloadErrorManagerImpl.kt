/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.download.data.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import javax.inject.Inject

class DownloadErrorManagerImpl @Inject constructor(

) : DownloadErrorManager {
    private val _errors: MutableSharedFlow<DownloadErrorManager.Error> = MutableSharedFlow()
    override val errors: Flow<DownloadErrorManager.Error> = _errors

    override suspend fun post(error: DownloadErrorManager.Error) {
        _errors.emit(error)
    }
}
