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

package me.proton.core.drive.log.data.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.DeviceInfo
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.log.domain.repository.LogRepository
import me.proton.core.drive.log.domain.usecase.ExportLog
import javax.inject.Inject

class ExportLogImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    repository: LogRepository,
    deviceInfo: DeviceInfo,
    configurationProvider: ConfigurationProvider,
    dateTimeFormatter: DateTimeFormatter,
) : ExportLog, BaseExportLog(dateTimeFormatter, deviceInfo, repository, configurationProvider) {

    override suspend fun invoke(userId: UserId, logUri: Uri): Result<Unit> = coRunCatching {
        appContext.contentResolver!!.openOutputStream(logUri)!!.use { outputStream ->
            outputStream.toZipLog(userId)
        }
    }
}
