/*
 * Copyright (c) 2021-2024 Proton AG.
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
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.drive.base.domain.extension.KiB
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.extension.bytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("MagicNumber", "AnnotateVersionCheck")
interface ConfigurationProvider {
    val host: String
    val baseUrl: String
    val appVersionHeader: String
    val uiPageSize: Int get() = 50
    val apiPageSize: Int get() = 150
    val apiBlockPageSize: Int get() = 50
    val apiListingPageSize: Int get() = 500
    val dbPageSize: Int get() = 500
    val cacheMaxEntries: Int get() = 10_000
    val linkMaxNameLength: Int get() = 255
    val blockMaxSize: Bytes get() = 4.MiB
    val thumbnailDefault: Thumbnail get() = Thumbnail(
        maxWidth = 512,
        maxHeight = 512,
        maxSize = 64.KiB,
    )
    val thumbnailPhoto: Thumbnail get() = Thumbnail(
        maxWidth = 1920,
        maxHeight = 1920,
        maxSize = 1.MiB
    )
    val downloadsInParallel: Int get() = 4
    val maxFileSizeToSendWithoutDownload: Bytes get() = blockMaxSize
    val preventScreenCapture: Boolean get() = false
    val passphraseSize: Bytes get() = 32.bytes
    val maxSharedLinkPasswordLength: Int get() = 50
    val maxSharedLinkExpirationDuration: Duration get() = 90.days
    val uploadBlocksInParallel: Int get() = 4
    val uploadsInParallel: Int get() = 6
    val nonUserUploadsInParallel: Int get() = 4
    val decryptionInParallel: Int get() = 4
    val bulkUploadThreshold: Int get() = 10
    val validateUploadLimit: Boolean get() = true
    val uploadLimitThreshold: Int get() = Int.MAX_VALUE
    val useExceptionMessage: Boolean get() = false
    val photosSavedCounter: Boolean get() = false
    val photosUpsellPhotoCount: Int get() = 5
    val backupLeftSpace: Bytes get() = 25.MiB
    val contentDigestAlgorithm: String get() = "SHA1"
    val digestAlgorithms: List<String> get() = listOf(contentDigestAlgorithm)
    val autoLockDurations: Set<Duration> get() = setOf(
        0.seconds, 60.seconds, 2.minutes, 5.minutes, 15.minutes, 30.minutes
    )
    val maxApiAutoRetries: Int get() = 10
    val logToFileInDebugEnabled: Boolean get() = true
    val allowBackupDeletedFilesEnabled: Boolean get() = false
    val scanBackupPageSize: Int get() = 100
    val backupDefaultBucketName: String get() = "Camera"
    val backupAdditionalBucketNames: List<String> get() = listOf("Raw", "Screenshots")
    val backupMaxAttempts: Long get() = 5
    val backupSyncWindow: Duration get() = 1.days
    val photoExportData: Boolean get() = false
    val checkDuplicatesPageSize: Int get() = 50
    val featureFlagFreshDuration: Duration get() = 10.minutes
    val useVerifier: Boolean get() = true
    val backupDefaultThumbnailsCacheLimit: Int get() = 1000
    val backupDefaultThumbnailsCacheLocalStorageThreshold: Bytes get() = 500.MiB
    val maxFreeSpace: Bytes get() = 5.GiB
    val activeUserPingDuration: Duration get() = 6.hours
    val disableFeatureFlagInDevelopment: Boolean get() = true
    val logDbMinLimit: Int get() = 1_000
    val logDbLimit: Int get() = 20_000
    val logDeviceInfoFile: LogFile get() = LogFile(
        name = "device_info.txt",
        mimeType = "text/plain",
    )
    val logCsvFile: LogFile get() = LogFile(
        name = "log.csv",
        mimeType = "text/csv",
    )
    val logZipFile: LogFile get() = LogFile(
        name = "log.zip",
        mimeType = "application/zip",
    )
    val minimumSharedVolumeEventFetchInterval: Duration get() = 10.minutes
    val minimumPublicAddressKeyFetchInterval: Duration get() = 10.minutes
    val minimumOrganizationFetchInterval: Duration get() = 1.days
    val protonDocsWebViewFeatureFlag: Boolean get() = true
    val observeWorkManagerInterval: Duration get() = 1.minutes
    val cacheInternalStorageLimit: Bytes get() = 512.MiB
    val drivePublicShareEditMode: Boolean get() = false // implement entitlements PublicCollaboration

    data class Thumbnail(
        val maxWidth: Int,
        val maxHeight: Int,
        val maxSize: Bytes,
    )

    data class LogFile(
        val name: String,
        val mimeType: String,
    )
}
