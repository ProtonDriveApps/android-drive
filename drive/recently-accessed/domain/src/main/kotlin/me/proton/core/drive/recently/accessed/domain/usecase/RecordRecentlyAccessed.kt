/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.recently.accessed.domain.usecase

import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatchingException
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveMobileRecentlyAccessed
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.isProtonCloudFile
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.recently.accessed.domain.entity.RecentlyAccessed
import me.proton.core.drive.recently.accessed.domain.manager.RecentlyAccessedManager
import me.proton.core.drive.recently.accessed.domain.repository.RecentlyAccessedRepository
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class RecordRecentlyAccessed @Inject constructor(
    private val getFeatureFlag: GetFeatureFlag,
    private val getLink: GetLink,
    private val getShare: GetShare,
    private val repository: RecentlyAccessedRepository,
    private val manager: RecentlyAccessedManager,
) {

    suspend operator fun invoke(
        userId: UserId,
        linkIds: List<LinkId>,
        accessTime: TimestampMs = TimestampMs(),
    ) = coRunCatchingException {
        if (!getFeatureFlag(driveMobileRecentlyAccessed(userId)).on) {
            return@coRunCatchingException
        }
        val recorded = linkIds.count { linkId ->
            coRunCatchingException { record(linkId, accessTime) }.getOrDefault(false)
        }
        if (recorded > 0) {
            manager.enqueueReport(userId)
        }
    }

    private suspend fun record(linkId: LinkId, accessTime: TimestampMs): Boolean {
        val link = getLink(linkId, flowOf(false)).toResult().getOrThrow()
        if (link.isProtonCloudFile) {
            return false
        }
        val share = getShare(linkId.shareId, flowOf(false)).toResult().getOrThrow()
        repository.add(
            RecentlyAccessed(
                linkId = linkId,
                volumeId = share.volumeId,
                accessTime = accessTime,
            )
        )
        return true
    }
}
