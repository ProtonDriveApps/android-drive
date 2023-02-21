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
package me.proton.core.drive.folder.create.presentation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.proton.core.drive.base.domain.extension.ellipsizeMiddle
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.logDefaultMessage
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.folder.create.domain.usecase.CreateFolder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.usecase.ValidateLinkName.Invalid.Empty
import me.proton.core.drive.link.domain.usecase.ValidateLinkName.Invalid.ExceedsMaxLength
import me.proton.core.drive.link.domain.usecase.ValidateLinkName.Invalid.ForbiddenCharacters
import me.proton.core.drive.link.domain.usecase.ValidateLinkName.Invalid.Periods
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

@HiltViewModel
@Suppress("StaticFieldLeak")
class CreateFolderViewModel @Inject constructor(
    private val createFolder: CreateFolder,
    private val broadcastMessages: BroadcastMessages,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId = ShareId(userId, savedStateHandle.require(KEY_SHARE_ID))
    private val parentFolderId = FolderId(shareId, savedStateHandle.require(KEY_PARENT_ID))
    private val name = MutableStateFlow(savedStateHandle.get<String>(KEY_FOLDER_NAME).orEmpty())
    private val error = MutableStateFlow<String?>(null)
    private val _effect = MutableSharedFlow<CreateFolderEffect>()
    private val inProgress = MutableStateFlow(false)
    val initialViewState = CreateFolderViewState(
        titleResId = R.string.folder_create_title,
        name = name.value,
        error = error.value,
        inProgress = inProgress.value
    )
    val viewState: Flow<CreateFolderViewState> = combine(
        name,
        error,
        inProgress,
    ) { name, error, inProgress ->
        initialViewState.copy(
            name = name,
            selection = if (error != null) name.selectAll else name.endPosition,
            error = error,
            inProgress = inProgress
        )
    }
    val viewEvent = object : CreateFolderViewEvent {
        override val onCreateFolder = { createFolder() }
        override val onNameChanged = { name: String -> onNameChanged(name) }
    }
    val createFolderEffect: Flow<CreateFolderEffect> = _effect.asSharedFlow()

    private fun onNameChanged(name: String) {
        this.name.value = name
        savedStateHandle.set(KEY_FOLDER_NAME, name)
        error.tryEmit(null)
    }

    private fun createFolder() {
        viewModelScope.launch {
            createFolder(name.value)
        }
    }

    private suspend fun createFolder(folderName: String) {
        inProgress.emit(true)
        createFolder(
            parentFolderId = parentFolderId,
            folderName = folderName,
        )
            .onFailure { error ->
                error.handle()
            }
            .onSuccess { (name, _) ->
                _effect.emit(CreateFolderEffect.Dismiss)
                broadcastMessages(
                    userId = userId,
                    message = context.getString(
                        R.string.folder_create_successful,
                        name.ellipsizeMiddle(MAX_DISPLAY_FOLDER_NAME_LENGTH)
                    )
                )
            }
        inProgress.emit(false)
    }

    private suspend fun Throwable.handle() = error.emit(
        when (this) {
            Empty -> context.getString(R.string.folder_create_error_name_is_blank)
            is ExceedsMaxLength -> context.getString(R.string.folder_create_error_name_too_long, this.maxLength)
            ForbiddenCharacters -> context.getString(R.string.folder_create_error_name_with_forbidden_characters)
            Periods -> context.getString(R.string.folder_create_error_name_periods)
            else -> logDefaultMessage(
                context = context,
                tag = VIEW_MODEL,
                unhandled = context.getString(R.string.folder_create_error_general),
            )
        }
    )

    private val String.selectAll
        get() = IntRange(0, length)

    private val String.endPosition
        get() = IntRange(length, length)

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_PARENT_ID = "parentId"
        const val KEY_FOLDER_NAME = "key.folderName"
        private const val MAX_DISPLAY_FOLDER_NAME_LENGTH = 50
    }
}
