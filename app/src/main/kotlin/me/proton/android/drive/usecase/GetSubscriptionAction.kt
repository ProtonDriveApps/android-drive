/*
 * Copyright (c) 2025 Proton AG.
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

import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

class GetSubscriptionAction @Inject constructor() {
    operator fun invoke(onAction: () -> Unit) = Action.Image(
        imageResId = subscriptionActionImageResId,
        contentDescriptionResId = I18N.string.content_description_subscription_action,
        onAction = onAction,
    )

    private val subscriptionActionImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.drive_subscription_badge_light,
        dark = BasePresentation.drawable.drive_subscription_badge_dark,
        dayNight = BasePresentation.drawable.drive_subscriptions_badge_daynight,
    )
}
