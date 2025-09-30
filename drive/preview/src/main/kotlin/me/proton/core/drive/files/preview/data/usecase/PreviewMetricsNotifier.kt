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

package me.proton.core.drive.files.preview.data.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.files.preview.data.extension.fileType
import me.proton.core.drive.files.preview.data.extension.pageType
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.observability.domain.metrics.MobilePerformancePreviewToFullContentHistogram
import me.proton.core.drive.observability.domain.metrics.MobilePerformancePreviewToThumbnailHistogram
import me.proton.core.drive.observability.domain.metrics.common.mobile.performance.AppLoadType
import me.proton.core.drive.observability.domain.metrics.common.mobile.performance.DataSource
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class PreviewMetricsNotifier @Inject constructor(
    private val getShare: GetShare,
    private val getDriveLink: GetDriveLink,
    private val getThumbnailAndContentCachedStatus: GetThumbnailAndContentCachedStatus,
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
) {
    private var previewThumbnailMetric: PreviewMetric? = null
    private val thumbnailCounter: MutableMap<FileId, Int> = mutableMapOf()
    private val thumbnailMutex: Mutex = Mutex()
    private var previewContentMetric: PreviewMetric? = null
    private val contentCounter: MutableMap<FileId, Int> = mutableMapOf()
    private val contentMutex: Mutex = Mutex()

    suspend fun previewStart(fileId: FileId, startTime: TimestampMs) {
        getThumbnailAndContentCachedStatus(fileId)
            .getOrNull()
            ?.let { (wasThumbnailCached, wasContentCached) ->
                previewThumbnailMetric = PreviewMetric(
                    fileId = fileId,
                    startTime = startTime,
                    wasCached = wasThumbnailCached,
                )
                previewContentMetric = PreviewMetric(
                    fileId = fileId,
                    startTime = startTime,
                    wasCached = wasContentCached,
                )
            }
    }

    suspend fun previewThumbnailRendered(
        fileId: FileId,
        stopTime: TimestampMs,
    ) = thumbnailMutex.withLock {
        previewThumbnailMetric?.let { previewMetric ->
            if (previewMetric.fileId != fileId) return@withLock
            val duration = (stopTime.value - previewMetric.startTime.value)
                .coerceAtLeast(minimumValue = 0)
                .toDuration(DurationUnit.MILLISECONDS)
            if (duration > Duration.ZERO) {
                previewToThumbnail(
                    fileId = previewMetric.fileId,
                    wasCached = previewMetric.wasCached,
                    count = getThumbnailCount(fileId),
                    duration = duration,
                )
                incrementThumbnailCount(fileId)
                previewThumbnailMetric = null
            }
        }
    }

    suspend fun previewContentRendered(
        fileId: FileId,
        stopTime: TimestampMs,
    ) = contentMutex.withLock {
        previewContentMetric?.let { previewMetric ->
            if (previewMetric.fileId != fileId) return@withLock
            val duration = (stopTime.value - previewMetric.startTime.value)
                .coerceAtLeast(minimumValue = 0)
                .toDuration(DurationUnit.MILLISECONDS)
            if (duration > Duration.ZERO) {
                previewToFullContent(
                    fileId = previewMetric.fileId,
                    wasCached = previewMetric.wasCached,
                    count = getContentCount(fileId),
                    duration = duration,
                )
                incrementContentCount(fileId)
                previewContentMetric = null
            }
        }
    }

    private suspend fun previewToFullContent(
        fileId: FileId,
        wasCached: Boolean,
        count: Int,
        duration: Duration,
    ) {
        getDriveLink(fileId).toResult().getOrNull()?.let { driveLink ->
            previewToFullContent(
                driveLink = driveLink,
                wasCached = wasCached,
                count = count,
                duration = duration,
            )
        }
    }

    private suspend fun previewToFullContent(
        driveLink: DriveLink,
        wasCached: Boolean,
        count: Int,
        duration: Duration,
    ) {
        getShare(driveLink.id.shareId).toResult().getOrNull()?.let { share ->
            val appLoadType = if (count > 1) AppLoadType.subsequent else AppLoadType.first
            val dataSource = if (wasCached) DataSource.local else DataSource.remote
            enqueueObservabilityEvent(
                MobilePerformancePreviewToFullContentHistogram(
                    Labels = MobilePerformancePreviewToFullContentHistogram.LabelsData(
                        pageType = share.pageType,
                        fileType = driveLink.link.fileType,
                        appLoadType = appLoadType,
                        dataSource = dataSource,
                    ),
                    Value = duration.inWholeMilliseconds,
                )
            ).also {
                CoreLogger.d(
                    tag = LogTag.METRIC,
                    message = buildString {
                        append("previewToFullContent ")
                        append("pageType=${share.pageType}, ")
                        append("fileType=${driveLink.link.fileType}, ")
                        append("appLoadType=$appLoadType, ")
                        append("dataSource=$dataSource, ")
                        append("duration=$duration")
                    },
                )
            }
        }
    }

    private suspend fun previewToThumbnail(
        fileId: FileId,
        wasCached: Boolean,
        count: Int,
        duration: Duration,
    ) {
        getDriveLink(fileId).toResult().getOrNull()?.let { driveLink ->
            previewToThumbnail(
                driveLink = driveLink,
                wasCached = wasCached,
                count = count,
                duration = duration,
            )
        }
    }

    private suspend fun previewToThumbnail(
        driveLink: DriveLink,
        wasCached: Boolean,
        count: Int,
        duration: Duration,
    ) {
        getShare(driveLink.id.shareId).toResult().getOrNull()
            ?.takeIf { share -> share.type == Share.Type.PHOTO }
            ?.let { share ->
                val appLoadType = if (count > 1) AppLoadType.subsequent else AppLoadType.first
                val dataSource = if (wasCached) DataSource.local else DataSource.remote
                enqueueObservabilityEvent(
                    MobilePerformancePreviewToThumbnailHistogram(
                        Labels = MobilePerformancePreviewToThumbnailHistogram.LabelsData(
                            pageType = share.pageType,
                            fileType = driveLink.link.fileType,
                            appLoadType = appLoadType,
                            dataSource = dataSource,
                        ),
                        Value = duration.inWholeMilliseconds,
                    )
                ).also {
                    CoreLogger.d(
                        tag = LogTag.METRIC,
                        message = buildString {
                            append("previewToThumbnail ")
                            append("pageType=${share.pageType}, ")
                            append("fileType=${driveLink.link.fileType}, ")
                            append("appLoadType=$appLoadType, ")
                            append("dataSource=$dataSource, ")
                            append("duration=$duration")
                        },
                    )
                }
            }
    }

    private fun getThumbnailCount(fileId: FileId): Int =
        thumbnailCounter.getOrPut(fileId) { 1 }

    private fun incrementThumbnailCount(fileId: FileId) {
        thumbnailCounter[fileId] = getThumbnailCount(fileId) + 1
    }

    private fun getContentCount(fileId: FileId): Int =
        contentCounter.getOrPut(fileId) { 1 }

    private fun incrementContentCount(fileId: FileId) {
        contentCounter[fileId] = getContentCount(fileId) + 1
    }

    private data class PreviewMetric(
        val fileId: FileId,
        val startTime: TimestampMs,
        val wasCached: Boolean,
    )
}
