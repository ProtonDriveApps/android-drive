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

package me.proton.android.drive.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.android.drive.photos.domain.usecase.IsPhotosEnabled
import me.proton.android.drive.ui.viewevent.PhotosExportDataViewEvent
import me.proton.android.drive.ui.viewstate.PhotosExportDataViewState
import me.proton.android.drive.usecase.ExportPhotoData
import me.proton.android.drive.usecase.SendFile
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject

@Suppress("StaticFieldLeak", "LongParameterList")
@HiltViewModel
class PhotosExportDataViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    isPhotosEnabled: IsPhotosEnabled,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
    private val exportPhotoData: ExportPhotoData,
    private val sendFile: SendFile,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val exportData = MutableStateFlow(false)

    val initialViewState: PhotosExportDataViewState = PhotosExportDataViewState(
        isExportDataEnabled = false,
        isExportDataLoading = false
    )
    val viewState: Flow<PhotosExportDataViewState> =
        combine(isPhotosEnabled(userId), exportData) { enabled, exportData ->
            initialViewState.copy(
                isExportDataEnabled = enabled && configurationProvider.photoExportData,
                isExportDataLoading = exportData,
            )
        }

    fun viewEvent(): PhotosExportDataViewEvent = object : PhotosExportDataViewEvent {

        override val onExportData = { context: Context ->
            viewModelScope.launch {
                exportData.value = true
                onExportData(context)
                exportData.value = false
            }
            Unit
        }
    }

    private suspend fun onExportData(context: Context) {
        val onFailure: (exception: Throwable) -> Unit = { error ->
            error.log(BACKUP)
            broadcastMessages(
                userId = userId,
                message = error.getDefaultMessage(
                    appContext,
                    configurationProvider.useExceptionMessage
                ),
                type = BroadcastMessage.Type.ERROR,
            )
        }
        exportPhotoData(userId).onSuccess { file ->
            sendFile(context, file).onFailure(onFailure)
        }.onFailure(onFailure)
    }
}
