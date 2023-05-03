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
import me.proton.core.plan.presentation.PlansOrchestrator
import javax.inject.Inject

@HiltViewModel
class PlansViewModel @Inject constructor(
    accountManager: AccountManager,
    private val plansOrchestrator: PlansOrchestrator,
) : ViewModel() {
    private val primaryAccount = accountManager
        .getPrimaryAccount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun initialize(context: FragmentActivity) {
        plansOrchestrator.register(context)
    }

    fun deInitialize() {
        plansOrchestrator.unregister()
    }

    fun showCurrentPlans() {
        viewModelScope.launch {
            val account = primaryAccount.filterNotNull().first()
            plansOrchestrator.showCurrentPlanWorkflow(account.userId)
        }
    }
}
