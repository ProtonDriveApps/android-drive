/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.feature.flag.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.isOlderThen
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository.RefreshId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshFeatureFlags @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val configurationProvider: ConfigurationProvider,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(
        userId: UserId,
        refreshId: RefreshId = RefreshId.Default,
    ): Result<Unit> = coRunCatching {
        mutex.withLock {
            takeIf {
                featureFlagRepository
                    .getLastRefreshTimestamp(userId, refreshId)
                    .isOlderThen(configurationProvider.featureFlagFreshDuration)
            }?.let {
                CoreLogger.d(LogTag.FEATURE_FLAG, "Refreshing feature flags ${refreshId.id}")
                featureFlagRepository.refresh(userId, refreshId).getOrThrow()
            } ?: throw IllegalStateException(
                """
                "Too many attempts. Refresh is allowed once in a
                ${configurationProvider.featureFlagFreshDuration.inWholeMinutes} minutes"
            """.trimIndent().replace("\n", " ")
            )
        }
    }
}
