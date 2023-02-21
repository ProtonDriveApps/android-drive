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
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.account.domain.entity.isDisabled
import me.proton.core.account.domain.entity.isReady
import me.proton.core.account.domain.entity.isStepNeeded
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.accountmanager.presentation.onUserAddressKeyCheckFailed
import me.proton.core.accountmanager.presentation.onUserKeyCheckFailed
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.onAddAccountResult
import me.proton.core.domain.entity.Product
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val authOrchestrator: AuthOrchestrator,
) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Processing)
    private val _primaryAccount = MutableStateFlow<Account?>(null)

    private fun onAccountReady() {
        _state.value = State.AccountReady
    }

    private fun onPrimaryNeeded() {
        _state.value = State.PrimaryNeeded
    }

    private fun onStepNeeded() {
        _state.value = State.Processing
    }

    val state = _state.asStateFlow()
    val primaryAccount: StateFlow<Account?> = _primaryAccount.asStateFlow()

    fun initialize(context: FragmentActivity) {
        // Account state handling.
        with(authOrchestrator) {
            register(context)
            onAddAccountResult { result ->
                if (result == null) _state.value = State.ExitApp
            }
            accountManager.observe(context.lifecycle, minActiveState = Lifecycle.State.CREATED)
                .onSessionSecondFactorNeeded { startSecondFactorWorkflow(it) }
                .onAccountTwoPassModeNeeded { startTwoPassModeWorkflow(it) }
                .onAccountCreateAddressNeeded { startChooseAddressWorkflow(it) }
                .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
                .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
                .onAccountDisabled { accountManager.removeAccount(it.userId) }
                .onUserKeyCheckFailed { /* errorToast("UserKeyCheckFailed")*/ }
                .onUserAddressKeyCheckFailed { /*errorToast("UserAddressKeyCheckFailed")*/ }
        }

        // Check if we already have Ready account.
        accountManager.getAccounts()
            .flowWithLifecycle(context.lifecycle, Lifecycle.State.CREATED)
            .onEach { accounts ->
                when {
                    accounts.isEmpty() || accounts.all { it.isDisabled() } -> onPrimaryNeeded()
                    accounts.any { it.isReady() } -> onAccountReady()
                    accounts.any { it.isStepNeeded() } -> onStepNeeded()
                }
            }.launchIn(context.lifecycleScope)

        accountManager.getPrimaryAccount()
            .flowWithLifecycle(context.lifecycle, Lifecycle.State.CREATED)
            .onEach { account -> _primaryAccount.value = account }
            .launchIn(context.lifecycleScope)
    }

    fun deInitialize() {
        authOrchestrator.unregister()
    }

    fun addAccount() {
        authOrchestrator.startAddAccountWorkflow(
            requiredAccountType = AccountType.External,
            creatableAccountType = AccountType.Internal,
            product = Product.Drive
        )
    }

    sealed class State {
        object PrimaryNeeded : State()
        object AccountReady : State()
        object Processing : State()
        object ExitApp : State()
    }
}
