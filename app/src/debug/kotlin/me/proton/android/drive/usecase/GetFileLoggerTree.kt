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

import android.util.Log
import fr.bipi.tressence.common.filters.Filter
import fr.bipi.tressence.file.FileLoggerTree
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.extension.percentageOfAsciiChars
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.startsWith
import timber.log.Timber
import javax.inject.Inject

class GetFileLoggerTree @Inject constructor(
    private val getDebugLogFile: GetDebugLogFile,
) {
    operator fun invoke(): Result<Timber.Tree> = coRunCatching {
        val debugLog = getDebugLogFile()
        FileLoggerTree.Builder()
            .withFileName(debugLog.name)
            .withDir(requireNotNull(debugLog.parentFile))
            .withSizeLimit(25.MiB.value.toInt())
            .withFileLimit(1)
            .withMinPriority(Log.DEBUG)
            .appendToFile(true)
            .withFilter(
                object : Filter {
                    override fun isLoggable(priority: Int, tag: String?): Boolean = true

                    override fun skipLog(
                        priority: Int,
                        tag: String?,
                        message: String,
                        t: Throwable?
                    ): Boolean =
                        tag != null &&
                        tag.startsWith("core.network") &&
                        message.percentageOfAsciiChars < Percentage(50)
                }
            )
            .build()
    }
}
