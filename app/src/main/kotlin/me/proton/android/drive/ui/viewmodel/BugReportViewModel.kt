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

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.usecase.GetUserEmail
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.report.presentation.ReportOrchestrator
import me.proton.core.report.presentation.entity.BugReportInput
import me.proton.core.report.presentation.entity.BugReportOutput
import javax.inject.Inject

@HiltViewModel
class BugReportViewModel @Inject constructor(
    accountManager: AccountManager,
    private val reportOrchestrator: ReportOrchestrator,
    private val broadcastMessages: BroadcastMessages,
    private val getUserEmail: GetUserEmail,
) : ViewModel() {

    private val primaryAccount = accountManager
        .getPrimaryAccount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun initialize(context: FragmentActivity) = reportOrchestrator.register(context) { output ->
        if (output is BugReportOutput.SuccessfullySent) {
            primaryAccount.value?.let { account ->
                broadcastMessages(account.userId, output.successMessage, BroadcastMessage.Type.INFO)
            }
        }
    }

    fun deInitialize() {
        reportOrchestrator.unregister()
    }

    fun sendBugReport() {
        viewModelScope.launch {
            val account = primaryAccount.filterNotNull().first()
            reportOrchestrator.startBugReport(
                input = BugReportInput(
                    email = getUserEmail(account.userId),
                    username = account.username,
                ),
            )
        }
    }
}
