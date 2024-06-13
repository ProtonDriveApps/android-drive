/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.log.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.log.domain.entity.Log
import me.proton.core.drive.log.domain.usecase.ExportLog
import me.proton.core.drive.log.domain.usecase.GetPagedLogs
import me.proton.core.drive.log.presentation.entity.LogItem
import me.proton.core.drive.log.presentation.viewevent.LogViewEvent
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class LogViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getPagedLogs: GetPagedLogs,
    private val broadcastMessages: BroadcastMessages,
    private val exportLog: ExportLog,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val logSdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val separatorSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val logs = getPagedLogs(userId)
        .map { pagingData ->
            pagingData.map { log ->
                val creationDateTime = Date(log.creationTime.value)
                LogItem.Log(
                    identifier = log.id,
                    creationDate = separatorSdf.format(creationDateTime),
                    creationTime = logSdf.format(creationDateTime),
                    message = log.message,
                    moreContent = log.moreContent,
                    isError = log.level == Log.Level.ERROR,
                )
            }
        }
        .map { pagingData ->
            pagingData.insertSeparators { before: LogItem.Log?, after: LogItem.Log? ->
                if (after == null) {
                    null
                } else if (before == null) {
                    LogItem.Separator(
                        value = after.creationDate,
                    )
                } else {
                    val beforeCalendar = Calendar.getInstance().apply {
                        timeInMillis = requireNotNull(separatorSdf.parse(before.creationDate)).time +
                                requireNotNull(logSdf.parse(before.creationTime)).time
                    }
                    val afterCalendar = Calendar.getInstance().apply {
                        timeInMillis = requireNotNull(separatorSdf.parse(after.creationDate)).time +
                                requireNotNull(logSdf.parse(after.creationTime)).time
                    }
                    if (beforeCalendar.get(Calendar.YEAR)
                        != afterCalendar.get(Calendar.YEAR) ||
                        beforeCalendar.get(Calendar.MONTH)
                        != afterCalendar.get(Calendar.MONTH) ||
                        beforeCalendar.get(Calendar.DAY_OF_MONTH)
                        != afterCalendar.get(Calendar.DAY_OF_MONTH)
                    ) {
                        LogItem.Separator(
                            value = after.creationDate,
                        )
                    } else {
                        null
                    }
                }
            }
        }

    val mimeType = configurationProvider.logZipFile.mimeType

    fun viewEvent(
        navigateToLogOptions: () -> Unit,
        showCreateLogPicker: (String, () -> Unit) -> Unit,
    ): LogViewEvent = object : LogViewEvent {
        override val onSave: () -> Unit = {
            showCreateLogPicker(configurationProvider.logZipFile.name) { handleActivityNotFound() }
        }

        override val onMoreOptions: () -> Unit = { navigateToLogOptions() }
    }

    fun onCreateLogResult(logUri: Uri) {
        viewModelScope.launch {
            exportLog(userId, logUri)
                .onFailure { error ->
                    broadcastMessages(
                        userId = userId,
                        message = error.message.orEmpty(),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
                .onSuccess {
                    broadcastMessages(
                        userId = userId,
                        message = appContext.getString(I18N.string.log_export_successfully_completed),
                        type = BroadcastMessage.Type.INFO,
                    )
                }
        }
    }

    private fun handleActivityNotFound() {
        broadcastMessages(
            userId = userId,
            message = appContext.getString(
                I18N.string.common_in_app_notification_activity_not_found,
                appContext.getString(I18N.string.operation_create_document)
            )
        )
    }
}
