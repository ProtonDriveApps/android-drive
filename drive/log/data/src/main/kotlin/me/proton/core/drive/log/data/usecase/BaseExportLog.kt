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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.DeviceInfo
import me.proton.core.drive.log.domain.entity.Log
import me.proton.core.drive.log.domain.repository.LogRepository
import java.io.OutputStream
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class BaseExportLog(
    private val dateTimeFormatter: DateTimeFormatter,
    private val deviceInfo: DeviceInfo,
    private val repository: LogRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    private fun Log.toCsvLine() = buildString {
        append(dateTimeFormatter.formatToIso8601String(Date(creationTime.value)))
        append(",")
        append(level)
        append(",")
        append(origin)
        append(",")
        append(
            message
                .replaceCommaWithSemiColon
                .replaceDoubleQuoteWithSingleQuote
                .wrapInQuotes
        )
        append(",")
        append(
            moreContent
                ?.replaceCommaWithSemiColon
                ?.replaceDoubleQuoteWithSingleQuote
                ?.wrapInQuotes
                ?: ""
        )
        append("\n")
    }

    protected suspend fun OutputStream.toZipLog(userId: UserId) =
        ZipOutputStream(this).use { zos ->
            zos.putNextEntry(ZipEntry(configurationProvider.logDeviceInfoFile.name))
            zos.exportDeviceInfo()
            zos.closeEntry()
            zos.putNextEntry(ZipEntry(configurationProvider.logCsvFile.name))
            zos.exportLogDataAsCsv(userId)
            zos.closeEntry()
        }

    private fun ZipOutputStream.exportDeviceInfo() {
        deviceInfo { info ->
            write("$info\n".toByteArray())
        }
    }

    private suspend fun ZipOutputStream.exportLogDataAsCsv(userId: UserId) {
        repository.getAllLogs(userId).forEach { log ->
            write(log.toCsvLine().toByteArray())
        }
    }

    private val String.replaceCommaWithSemiColon: String get() =
        this.replace(",", ";")

    private val String.replaceDoubleQuoteWithSingleQuote: String get() =
        this.replace("\"", "'")

    private val String.wrapInQuotes: String get() = "\"$this\""
}
