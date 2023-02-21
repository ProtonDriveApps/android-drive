/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.files.domain.operation

import me.proton.core.drive.files.R
import me.proton.core.drive.files.domain.operation.notification.MoveFileExtra
import me.proton.core.drive.files.domain.usecase.MoveFile
import me.proton.core.drive.messagequeue.domain.ActionProvider
import java.io.Serializable
import javax.inject.Inject

class FileOperationActionProvider @Inject constructor(
    private val moveFile: MoveFile,
) : ActionProvider {

    override fun provideAction(extra: Serializable?): ActionProvider.Action? =
        if (extra is MoveFileExtra) {
            extra.provideAction()
        } else {
            null
        }

    private fun MoveFileExtra.provideAction(): ActionProvider.Action? = if (exception != null) {
        ActionProvider.Action(R.string.files_operation_retry_action) {
            moveFile(userId, links.map { pair -> pair.second }, folderId)
        }
    } else if (allowUndo) {
        ActionProvider.Action(R.string.files_operation_undo_action) {
            val folderChildren = links.groupBy { pair -> pair.first }
            folderChildren.keys.forEach { originalFolderId ->
                originalFolderId?.let {
                    moveFile(
                        userId = userId,
                        linkIds = folderChildren[originalFolderId]!!.map { pair -> pair.second },
                        folderId = originalFolderId,
                        allowUndo = false,
                    )
                }
            }
        }
    } else {
        null
    }
}
