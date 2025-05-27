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

package me.proton.core.drive.upload.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class BroadcastFilesBeingUploaded @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val broadcastMessages: BroadcastMessages,
){
    operator fun invoke(
        userId: UserId,
        folderName: String,
        uriStringSize: Int,
        uploadFileLinksSize: Int,
    ) {
        val ignoreFilesSize = uriStringSize - uploadFileLinksSize
        when {
            ignoreFilesSize == 0 -> broadcastMessages(
                userId = userId,
                message = appContext.resources.getQuantityString(
                    I18N.plurals.files_upload_being_uploaded_notification,
                    uploadFileLinksSize,
                    uploadFileLinksSize,
                    folderName,
                ),
                type = BroadcastMessage.Type.INFO,
            )
            uploadFileLinksSize == 0 -> broadcastMessages(
                userId = userId,
                message = appContext.resources.getQuantityString(
                    I18N.plurals.files_upload_failed_notification,
                    ignoreFilesSize,
                    ignoreFilesSize,
                    folderName,
                ),
                type = BroadcastMessage.Type.ERROR,
            )
            else -> broadcastMessages(
                userId = userId,
                message = buildString {
                    append(
                        appContext.resources.getQuantityString(
                            I18N.plurals.files_upload_being_uploaded_notification,
                            uploadFileLinksSize,
                            uploadFileLinksSize,
                            folderName,
                        )
                    )
                    append("\n")
                    append(
                        appContext.resources.getQuantityString(
                            I18N.plurals.files_upload_being_ignored_notification,
                            ignoreFilesSize,
                            ignoreFilesSize,
                            folderName,
                        )
                    )
                },
                type = BroadcastMessage.Type.INFO,
            )
        }
    }
}
