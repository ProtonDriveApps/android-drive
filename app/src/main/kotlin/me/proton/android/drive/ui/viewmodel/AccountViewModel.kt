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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.isDisabled
import me.proton.core.account.domain.entity.isReady
import me.proton.core.account.domain.entity.isStepNeeded
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountDeviceSecretNeeded
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.accountmanager.presentation.onUserAddressKeyCheckFailed
import me.proton.core.accountmanager.presentation.onUserKeyCheckFailed
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.onAddAccountResult
import me.proton.core.domain.entity.UserId
import me.proton.core.usersettings.presentation.UserSettingsOrchestrator
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val authOrchestrator: AuthOrchestrator,
    private val userSettingsOrchestrator: UserSettingsOrchestrator,
) : ViewModel() {

    private val exitApp = MutableStateFlow(false)

    val primaryAccount: StateFlow<Account?> = accountManager.getPrimaryAccount()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val state: StateFlow<State> = combine(
        exitApp,
        accountManager.getAccounts()
    ) { exitApp, accounts ->
        when {
            exitApp -> State.ExitApp
            accounts.isEmpty() || accounts.all { it.isDisabled() } -> State.PrimaryNeeded
            accounts.any { it.isReady() } -> State.AccountReady
            accounts.any { it.isStepNeeded() } -> State.StepNeeded
            else -> State.Processing
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, State.Processing)

    fun initialize(context: FragmentActivity) {
        // Account state handling.
        with(authOrchestrator) {
            register(context)
            onAddAccountResult { result ->
                if (result == null && primaryAccount.value == null) exitApp.value = true
            }
            accountManager.observe(context.lifecycle, minActiveState = Lifecycle.State.CREATED)
                .onSessionSecondFactorNeeded { startSecondFactorWorkflow(it) }
                .onAccountTwoPassModeNeeded { startTwoPassModeWorkflow(it) }
                .onAccountCreateAddressNeeded { startChooseAddressWorkflow(it) }
                .onAccountDeviceSecretNeeded { startDeviceSecretWorkflow(it) }
                .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
                .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
                .onAccountDisabled { accountManager.removeAccount(it.userId) }
                .onUserKeyCheckFailed { /* errorToast("UserKeyCheckFailed")*/ }
                .onUserAddressKeyCheckFailed { /*errorToast("UserAddressKeyCheckFailed")*/ }
        }
        userSettingsOrchestrator.register(context)
    }

    fun deInitialize() {
        authOrchestrator.unregister()
        userSettingsOrchestrator.unregister()
    }

    fun startAddAccount() {
        authOrchestrator.startAddAccountWorkflow()
    }

    fun startPasswordManagement(userId: UserId) {
        userSettingsOrchestrator.startPasswordManagementWorkflow(userId)
    }

    fun startUpdateRecoveryEmail(userId: UserId) {
        userSettingsOrchestrator.startUpdateRecoveryEmailWorkflow(userId)
    }

    fun startSecurityKeys(userId: UserId) {
        userSettingsOrchestrator.startSecurityKeysWorkflow(userId)
    }

    sealed class State {
        object PrimaryNeeded : State()
        object AccountReady : State()
        object StepNeeded : State()
        object Processing : State()
        object ExitApp : State()
    }
}
