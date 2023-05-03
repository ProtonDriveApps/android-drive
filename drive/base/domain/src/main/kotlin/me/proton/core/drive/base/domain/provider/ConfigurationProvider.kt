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
package me.proton.core.drive.base.domain.provider

import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.KiB
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.extension.bytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface ConfigurationProvider {
    val host: String
    val baseUrl: String
    val appVersionHeader: String
    val uiPageSize: Int get() = 50
    val apiPageSize: Int get() = 150
    val apiBlockPageSize: Int get() = 50
    val dbPageSize: Int get() = 500
    val cacheMaxEntries: Int get() = 10_000
    val linkMaxNameLength: Int get() = 255
    val blockMaxSize: Bytes get() = 4.MiB
    val thumbnailMaxWidth: Int get() = 512
    val thumbnailMaxHeight: Int get() = 512
    val thumbnailMaxSize: Bytes get() = 60.KiB
    val downloadsInParallel: Int get() = 4
    val maxFileSizeToSendWithoutDownload: Bytes get() = blockMaxSize
    val preventScreenCapture: Boolean get() = false
    val passphraseSize: Bytes get() = 32.bytes
    val maxSharedLinkPasswordLength: Int get() = 50
    val maxSharedLinkExpirationDuration: Duration get() = 90.days
    val uploadBlocksInParallel: Int get() = 4
    val uploadsInParallel: Int get() = 4
    val decryptionInParallel: Int get() = 4
    val bulkUploadThreshold: Int get() = 10
    val validateUploadLimit: Boolean get() = true
    val uploadLimitThreshold: Int get() = 250
    val useExceptionMessage: Boolean get() = false
    val digestAlgorithms: List<String> get() = listOf("SHA1")
    val autoLockDurations: Set<Duration> get() = setOf(
        0.seconds, 60.seconds, 2.minutes, 5.minutes, 15.minutes, 30.minutes
    )
    val maxApiAutoRetries: Int get() = 10
}
