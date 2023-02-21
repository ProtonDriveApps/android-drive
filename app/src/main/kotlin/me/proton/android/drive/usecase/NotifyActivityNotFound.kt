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

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation

@Suppress("StaticFieldLeak")
class NotifyActivityNotFound @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val broadcastMessages: BroadcastMessages,
) {
    operator fun invoke(userId: UserId, @StringRes operation: Int) =
        broadcastMessages(
            userId = userId,
            message = appContext.getString(
                BasePresentation.string.in_app_notification_activity_not_found,
                appContext.getString(operation)
            )
        )
}
