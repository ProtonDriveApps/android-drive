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

package me.proton.core.drive.files.domain.usecase

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.folder.domain.usecase.HasAnyCachedFolderChildren
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.observability.domain.metrics.MobilePerformanceToFirstItemHistogram
import me.proton.core.drive.observability.domain.metrics.common.mobile.performance.AppLoadType
import me.proton.core.drive.observability.domain.metrics.common.mobile.performance.DataSource
import me.proton.core.drive.observability.domain.metrics.common.mobile.performance.PageType
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.drive.share.user.domain.entity.ShareTargetType
import me.proton.core.drive.share.user.domain.repository.SharedRepository
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.usecase.GetOldestActiveVolume
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class ToFirstItemMetricsNotifier @Inject constructor(
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
    private val hasAnyCachedFolderChildren: HasAnyCachedFolderChildren,
    private val getShares: GetShares,
    private val linkTrashRepository: LinkTrashRepository,
    private val getOldestActiveVolume: GetOldestActiveVolume,
    private val sharedRepository: SharedRepository,
) {
    private var toFirstItemMetric: ToFirstItemMetric? = null
    private val counter: MutableMap<String, Int> = mutableMapOf()
    private val mutex: Mutex = Mutex()

    fun reset() {
        toFirstItemMetric = null
    }

    suspend fun toFirstItemStart(userId: UserId, pageType: PageType, startTime: TimestampMs) {
        toFirstItemMetric = ToFirstItemMetric(
            pageType = pageType,
            startTime = startTime,
            wasCached = pageType.wasCached(userId),
        )
    }

    suspend fun itemThumbnailRendered(
        userId: UserId,
        pageType: PageType,
        stopTime: TimestampMs,
    ) = mutex.withLock {
        toFirstItemMetric?.let { firstItemMetric ->
            if (firstItemMetric.pageType != pageType) return@withLock
            val duration = (stopTime.value - firstItemMetric.startTime.value)
                .coerceAtLeast(minimumValue = 0)
                .toDuration(DurationUnit.MILLISECONDS)
            if (duration > Duration.ZERO) {
                toFirstItemHistogram(
                    pageType = firstItemMetric.pageType,
                    wasCached = firstItemMetric.wasCached,
                    count = getCount(userId, pageType),
                    duration = duration,
                )
                incrementCount(userId, pageType)
                reset()
            }
        }
    }
    private suspend fun PageType.wasCached(userId: UserId) = when (this) {
        PageType.my_files -> hasAnyCachedFolderChildren(
            listOfNotNull(
                getShares(userId, Share.Type.MAIN, flowOf(false)).toResult().getOrNull()
            ).flatten()
        )
        PageType.photos -> hasAnyCachedFolderChildren(
            listOfNotNull(
                getShares(userId, Share.Type.PHOTO, flowOf(false)).toResult().getOrNull()
            ).flatten()
        )
        PageType.computers -> hasAnyCachedFolderChildren(
            listOfNotNull(
                getShares(userId, Share.Type.DEVICE, flowOf(false)).toResult().getOrNull()
            ).flatten()
        )
        PageType.shared_by_me -> hasAnySharedByMeLinks(userId)
        PageType.shared_with_me -> hasAnySharedWithMeLinks(userId)
        PageType.trash -> hasAnyTrashedLinks(userId)
    }

    private suspend fun toFirstItemHistogram(
        pageType: PageType,
        wasCached: Boolean,
        count: Int,
        duration: Duration,
    ) {
        val appLoadType = if (count > 1) AppLoadType.subsequent else AppLoadType.first
        val dataSource = if (wasCached) DataSource.local else DataSource.remote
        enqueueObservabilityEvent(
            MobilePerformanceToFirstItemHistogram(
                Labels = MobilePerformanceToFirstItemHistogram.LabelsData(
                    pageType = pageType,
                    appLoadType = appLoadType,
                    dataSource = dataSource,
                ),
                Value = duration.inWholeMilliseconds,
            )
        ).also {
            CoreLogger.d(
                tag = LogTag.METRIC,
                message = buildString {
                    append("toFirstItemHistogram ")
                    append("pageType=$pageType, ")
                    append("appLoadType=$appLoadType, ")
                    append("dataSource=$dataSource, ")
                    append("duration=$duration")
                },
            )
        }
    }

    private fun getCount(userId: UserId, pageType: PageType): Int =
        counter.getOrPut(userId.counterKey(pageType)) { 1 }

    private fun incrementCount(userId: UserId, pageType: PageType) {
        counter[userId.counterKey(pageType)] = getCount(userId, pageType) + 1
    }

    private fun UserId.counterKey(pageType: PageType) = "$this.$pageType"

    private suspend fun hasAnyTrashedLinks(
        userId: UserId
    ): Boolean = getOldestActiveVolume(userId, Volume.Type.REGULAR)
        .mapSuccessValueOrNull()
        .mapNotNull { volume -> volume?.id }
        .transform { volumeId ->
            emitAll(linkTrashRepository.hasTrashContent(userId, volumeId))
        }
        .firstOrNull() ?: false

    private suspend fun hasAnySharedByMeLinks(
        userId: UserId,
    ): Boolean = sharedRepository.getSharedByMeListingCount(userId) > 0

    private suspend fun hasAnySharedWithMeLinks(
        userId: UserId,
    ): Boolean = sharedRepository.getSharedWithMeListingCount(
        userId = userId,
        types = setOf(
            ShareTargetType.File,
            ShareTargetType.Photo,
            ShareTargetType.Folder,
            ShareTargetType.Document
        ),
    ) > 0

    private data class ToFirstItemMetric(
        val pageType: PageType,
        val startTime: TimestampMs,
        val wasCached: Boolean,
    )
}
