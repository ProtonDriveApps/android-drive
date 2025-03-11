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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.exception.ShareException
import me.proton.core.network.domain.isApiProtonError
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

class OnFilesDriveLinkError @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(
        userId: UserId,
        previous: DataResult<DriveLink>?,
        error: DataResult.Error,
        contentState: MutableStateFlow<ListContentState>,
        shareType: Share.Type = Share.Type.MAIN,
    ) {
        when {
            error.isTransient(previous, shareType) -> error.broadcastMessage(userId)
            else -> contentState.emit(error.toListContentState())
        }
    }

    private fun DataResult.Error.isTransient(
        previous: DataResult<DriveLink>?,
        shareType: Share.Type,
    ): Boolean =
        (previous != null && previous !is DataResult.Error) ||
                ((cause as? ShareException.ShareLocked)?.let { e -> e.shareType == shareType } ?: false)

    private fun DataResult.Error.broadcastMessage(userId: UserId) = broadcastMessage?.let { message ->
        broadcastMessages(
            userId = userId,
            message = message,
            type = BroadcastMessage.Type.ERROR,
        )
    }

    private fun DataResult.Error.toListContentState() = if(cause?.isApiProtonError(ProtonApiCode.PHOTO_MIGRATION) == true) {
        ListContentState.Error(
            message = errorMessage,
            titleId = I18N.string.photos_error_backup_migration_title,
            descriptionResId = I18N.string.photos_error_backup_migration_description,
            imageResId = getThemeDrawableId(
                light = BasePresentation.drawable.img_update_required_light,
                dark = BasePresentation.drawable.img_update_required_dark,
                dayNight = BasePresentation.drawable.img_update_required_daynight,
            )
        )
    } else {
        ListContentState.Error(
            message = errorMessage,
            actionResId = if (this.isRetryable) I18N.string.common_retry else null
        )
    }

    private val DataResult.Error.broadcastMessage: String? get() =
        when (val cause = this.cause) {
            is NoSuchElementException -> null
            null -> null
            else -> cause.getDefaultMessage(appContext, configurationProvider.useExceptionMessage)
        }

    private val DataResult.Error.errorMessage: String get() =
        broadcastMessage ?: appContext.getString(
            I18N.string.error_could_not_load_folder_content
        )
}
