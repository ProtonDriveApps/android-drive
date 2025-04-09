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

package me.proton.android.drive.photos.presentation.extension

import android.content.Context
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult
import me.proton.core.drive.photo.domain.extension.onAddToAlreadyExists
import me.proton.core.drive.photo.domain.extension.onAddToFailure
import me.proton.core.drive.photo.domain.extension.onRemoveFromFailure
import me.proton.core.drive.photo.domain.extension.onSuccess
import me.proton.core.drive.i18n.R as I18N

fun AddToRemoveFromAlbumResult.processAdd(context: Context, block: (String, BroadcastMessage.Type) -> Unit) =
    this
        .onSuccess { count ->
            block(
                context.quantityString(
                    I18N.plurals.in_app_notification_add_to_album_success,
                    count,
                ),
                BroadcastMessage.Type.INFO,
            )
        }
        .onAddToAlreadyExists { alreadyExistsCount, successCount ->
            block(
                buildString {
                    append(
                        context.quantityString(
                            I18N.plurals.in_app_notification_add_to_album_already_exists,
                            alreadyExistsCount,
                        )
                    )
                    if (successCount > 0) {
                        append(", ")
                        append(
                            context.quantityString(
                                I18N.plurals.in_app_notification_add_to_album_success,
                                successCount,
                            )
                        )
                    }
                },
                BroadcastMessage.Type.WARNING,
            )
        }
        .onAddToFailure { failedCount, successCount, alreadyExistsCount ->
            block(
                buildString {
                    append(
                        context.quantityString(
                            I18N.plurals.in_app_notification_add_to_album_failed,
                            failedCount,
                        )
                    )
                    if (alreadyExistsCount > 0) {
                        append(", ")
                        append(
                            context.quantityString(
                                I18N.plurals.in_app_notification_add_to_album_already_exists,
                                alreadyExistsCount,
                            )
                        )
                    }
                    if (successCount > 0) {
                        append(", ")
                        append(
                            context.quantityString(
                                I18N.plurals.in_app_notification_add_to_album_success,
                                successCount,
                            )
                        )
                    }
                },
                BroadcastMessage.Type.ERROR,
            )
        }

fun AddToRemoveFromAlbumResult.processRemove(context: Context, block: (String, BroadcastMessage.Type) -> Unit) =
    this
        .onSuccess { count ->
            block(
                context.quantityString(
                    I18N.plurals.in_app_notification_remove_from_album_success,
                    count,
                ),
                BroadcastMessage.Type.INFO,
            )
        }
        .onRemoveFromFailure { failedCount, successCount ->
            block(
                buildString {
                    append(
                        context.quantityString(
                            I18N.plurals.in_app_notification_removed_from_album_failed,
                            failedCount,
                        )
                    )
                    if (successCount > 0) {
                        append(", ")
                        append(
                            context.quantityString(
                                I18N.plurals.in_app_notification_remove_from_album_success,
                                successCount,
                            )
                        )
                    }
                },
                BroadcastMessage.Type.ERROR,
            )
        }
