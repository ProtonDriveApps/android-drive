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

package me.proton.drive.android.settings.domain.usecase

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.drive.android.settings.domain.UiSettingsRepository
import javax.inject.Inject

class GetLayoutType @Inject constructor(
    private val repository: UiSettingsRepository
) {
    operator fun invoke(userId: UserId) = repository.getUiSettingsFlow(userId)
        .map { uiSettings -> uiSettings.layoutType }
        .distinctUntilChanged()
}
