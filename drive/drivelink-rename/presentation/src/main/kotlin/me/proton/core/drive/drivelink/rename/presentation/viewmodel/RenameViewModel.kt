/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.drivelink.rename.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.extension.logDefaultMessage
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.rename.presentation.RenameEffect
import me.proton.core.drive.drivelink.rename.presentation.RenameViewEvent
import me.proton.core.drive.drivelink.rename.presentation.RenameViewState
import me.proton.core.drive.drivelink.rename.presentation.selection.NameWithSelection
import me.proton.core.drive.drivelink.rename.presentation.selection.Selection
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.usecase.ValidateLinkName.Invalid.Empty
import me.proton.core.drive.link.domain.usecase.ValidateLinkName.Invalid.ExceedsMaxLength
import me.proton.core.drive.link.domain.usecase.ValidateLinkName.Invalid.ForbiddenCharacters
import me.proton.core.drive.link.domain.usecase.ValidateLinkName.Invalid.Periods
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.i18n.R as I18N

@Suppress("StaticFieldLeak")
abstract class RenameViewModel(
    @ApplicationContext protected val appContext: Context,
    protected val savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    protected val shareId = ShareId(userId, savedStateHandle.require(KEY_SHARE_ID))
    protected val fileId: String? = savedStateHandle.get(KEY_FILE_ID)
    protected val linkId = if (fileId != null) {
        FileId(shareId, fileId)
    } else {
        FolderId(
            shareId = shareId,
            id = savedStateHandle.require(KEY_FOLDER_ID),
        )
    }
    protected val _renameEffect = MutableSharedFlow<RenameEffect>()
    private val isRenaming = MutableStateFlow(false)
    protected abstract val titleResId: Int
    val viewState: Flow<RenameViewState> = isRenaming.map {
        RenameViewState(
            titleResId = titleResId,
            isRenaming = isRenaming.value
        )
    }
    private val reselectionTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    protected val name: MutableSharedFlow<String> = MutableSharedFlow(1)
    val nameWithSelection: Flow<NameWithSelection> = combine(
        name,
        reselectionTrigger,
    ) { filename, _ ->
        NameWithSelection(
            name = filename,
            selection = filename.selection(linkId is FolderId)
        )
    }
    val viewEvent = object : RenameViewEvent {
        override val onRename = { name: String -> onRename(name) }
        override val onNameChanged: (String) -> Unit = { name: String -> onChanged(name) }
    }
    val renameEffect: Flow<RenameEffect> = _renameEffect.asSharedFlow()

    protected abstract suspend fun doRenameFile(name: String)

    private fun onRename(name: String) {
        clearInputError()
        renameFile(name)
    }

    private fun renameFile(name: String) {
        viewModelScope.launch {
            isRenaming.emit(true)
            doRenameFile(name)
            isRenaming.emit(false)
        }
    }

    private fun onChanged(name: String) {
        savedStateHandle.set(KEY_FILENAME, name)
        clearInputError()
    }

    private fun String.selection(isFolder: Boolean): Selection {
        val extensionIndex = lastIndexOf('.')
        return if (!isFolder && extensionIndex > 0) Selection(0, extensionIndex) else Selection(0, length)
    }

    protected suspend fun Throwable.handle() {
        with (_renameEffect) {
            emit(
                RenameEffect.ShowInputError(
                    when (this@handle) {
                        Empty -> appContext.getString(I18N.string.link_rename_error_name_is_blank)
                        is ExceedsMaxLength ->
                            appContext.getString(
                                I18N.string.link_rename_error_name_too_long,
                                this@handle.maxLength
                            )

                        ForbiddenCharacters -> appContext.getString(
                            I18N.string.link_rename_error_name_with_forbidden_characters
                        ).format("/", "\\")

                        Periods -> appContext.getString(I18N.string.link_rename_error_name_periods)
                        else -> logDefaultMessage(
                            context = appContext,
                            tag = VIEW_MODEL,
                            unhandled = appContext.getString(I18N.string.link_rename_error_general),
                        )
                    }
                )
            )
            reselectionTrigger.tryEmit(Unit)
        }
    }

    private fun clearInputError() = viewModelScope.launch {
        _renameEffect.emit(RenameEffect.ClearInputError)
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_FILE_ID = "fileId"
        const val KEY_FOLDER_ID = "folderId"
        const val KEY_FILENAME = "key.filename"
        const val MAX_DISPLAY_FILENAME_LENGTH = 50
    }
}
