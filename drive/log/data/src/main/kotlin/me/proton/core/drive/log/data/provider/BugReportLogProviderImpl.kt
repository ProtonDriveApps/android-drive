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

package me.proton.core.drive.log.data.provider

import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.DeviceInfo
import me.proton.core.drive.base.domain.usecase.GetCacheTempFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidUserLogDisabled
import me.proton.core.drive.feature.flag.domain.extension.off
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.log.data.usecase.BaseExportLog
import me.proton.core.drive.log.domain.repository.LogRepository
import me.proton.core.report.domain.provider.BugReportLogProvider
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class BugReportLogProviderImpl @Inject constructor(
    repository: LogRepository,
    deviceInfo: DeviceInfo,
    dateTimeFormatter: DateTimeFormatter,
    private val getCacheTempFolder: GetCacheTempFolder,
    private val accountManager: AccountManager,
    private val configurationProvider: ConfigurationProvider,
    private val getFeatureFlag: GetFeatureFlag,
) : BugReportLogProvider, BaseExportLog(dateTimeFormatter, deviceInfo, repository, configurationProvider) {

    override suspend fun getLog(): File? = coRunCatching {
        accountManager.getPrimaryUserId().firstOrNull()?.let { userId ->
            val userLogKillSwitch = getFeatureFlag(driveAndroidUserLogDisabled(userId))
            takeIf { userLogKillSwitch.off }?.let {
                zipLog(userId)
            }
        }
    }
        .onFailure { error -> error.log(LogTag.LOG) }
        .getOrNull()

    override suspend fun releaseLog(log: File) {
        coRunCatching { log.delete() }
            .onFailure { error -> error.log(LogTag.LOG) }
    }

    private suspend fun zipLog(userId: UserId): File = getCacheTempFolder(userId).let { cacheFolder ->
        File(cacheFolder, configurationProvider.logZipFile.name)
            .apply {
                if (exists()) delete()
                createNewFile()
                FileOutputStream(this).toZipLog(userId)
            }
    }
}
