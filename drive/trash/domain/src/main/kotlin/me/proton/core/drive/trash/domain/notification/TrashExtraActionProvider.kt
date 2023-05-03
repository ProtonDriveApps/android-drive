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

package me.proton.core.drive.trash.domain.notification

import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.messagequeue.domain.ActionProvider
import me.proton.core.drive.trash.domain.usecase.DeleteFromTrash
import me.proton.core.drive.trash.domain.usecase.EmptyTrash
import me.proton.core.drive.trash.domain.usecase.RestoreFromTrash
import me.proton.core.drive.trash.domain.usecase.SendToTrash
import java.io.Serializable
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@ExperimentalCoroutinesApi
class TrashExtraActionProvider @Inject constructor(
    private val sendToTrash: SendToTrash,
    private val restoreFromTrash: RestoreFromTrash,
    private val deleteFromTrash: DeleteFromTrash,
    private val emptyTrash: EmptyTrash,
) : ActionProvider {

    override fun provideAction(extra: Serializable?): ActionProvider.Action? = when (extra) {
        is TrashFilesExtra -> extra.provideAction()
        is RestoreFilesExtra -> extra.provideAction()
        is DeleteFilesExtra -> extra.provideAction()
        else -> null
    }

    private fun TrashFilesExtra.provideAction(): ActionProvider.Action = if (exception == null) {
        ActionProvider.Action(I18N.string.trash_action_undo) {
            restoreFromTrash(userId, folderId.shareId, links)
        }
    } else {
        ActionProvider.Action(I18N.string.trash_action_retry) {
            sendToTrash(userId, folderId, links)
        }
    }

    private fun RestoreFilesExtra.provideAction(): ActionProvider.Action? = if (exception == null) {
        null // We don't have the folder to send them back
    } else {
        ActionProvider.Action(I18N.string.trash_action_retry) {
            restoreFromTrash(userId, shareId, links)
        }
    }

    private fun DeleteFilesExtra.provideAction(): ActionProvider.Action? = if (exception == null) {
        null // Can't undo a delete operation
    } else {
        ActionProvider.Action(I18N.string.trash_action_retry) {
            deleteFromTrash(userId, shareId, links)
        }
    }

    private fun EmptyTrashExtra.provideAction(): ActionProvider.Action? = if (exception == null) {
        null // Can't undo a delete operation
    } else {
        ActionProvider.Action(I18N.string.trash_action_retry) {
            emptyTrash(userId, shareId)
        }
    }
}
