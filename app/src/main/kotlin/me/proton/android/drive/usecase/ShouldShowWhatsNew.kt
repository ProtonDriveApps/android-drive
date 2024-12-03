/*
 * Copyright (c) 2024 Proton AG.
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

import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidWhatsNew
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.drive.android.settings.domain.entity.WhatsNewKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShouldShowWhatsNew @Inject constructor(
    private val wasOnboardingShown: WasOnboardingShown,
    private val wasWhatsNewShown: WasWhatsNewShown,
    private val getFeatureFlagFlow: GetFeatureFlagFlow,
) {

    suspend operator fun invoke(userId: UserId): Result<WhatsNewKey?> = coRunCatching {
        if (wasOnboardingShown().getOrThrow() && getFeatureFlagFlow(
                featureFlagId = driveAndroidWhatsNew(userId),
                emitNotFoundInitially = false,
            ).first().on
        ) {
            when {
                canShow(WhatsNewKey.PROTON_DOCS) -> WhatsNewKey.PROTON_DOCS
                else -> null
            }
        } else {
            null
        }
    }

    private suspend fun ShouldShowWhatsNew.canShow(key: WhatsNewKey) =
        TimestampS() < key.limit && wasWhatsNewShown(key).getOrThrow().not()
}
