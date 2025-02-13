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

package me.proton.android.drive.photos.domain.usecase

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.photo.domain.usecase.GetPhotoCount
import me.proton.core.drive.user.domain.entity.UserMessage
import me.proton.core.drive.user.domain.extension.hasSubscriptionWithMoreStorage
import me.proton.core.drive.user.domain.usecase.HasCanceledUserMessages
import me.proton.core.user.domain.usecase.GetUser
import javax.inject.Inject

class ShowUpsell @Inject constructor(
    private val hasCanceledUserMessages: HasCanceledUserMessages,
    private val configurationProvider: ConfigurationProvider,
    private val isPhotosEnabled: IsPhotosEnabled,
    private val getPhotoCount: GetPhotoCount,
    private val getUser: GetUser,
) {
    operator fun invoke(userId: UserId) = combine(
        hasCanceledUserMessages(userId, UserMessage.UPSELL_PHOTOS),
        isPhotosEnabled(userId),
        getPhotoCount(userId),
    ) { hasCanceledUserMessages, isPhotosEnabled, photoCount ->
        if (hasCanceledUserMessages) {
            false
        } else {
            isPhotosEnabled && hasEnoughPhotosUploaded(photoCount) && isFreeUser(userId) == true
        }
    }.distinctUntilChanged()

    private fun hasEnoughPhotosUploaded(photoCount: Int) =
        photoCount >= configurationProvider.photosUpsellPhotoCount

    private suspend fun isFreeUser(userId: UserId) =
        coRunCatching { getUser(userId, false) }.getOrNull()
            ?.hasSubscriptionWithMoreStorage?.not()
}
