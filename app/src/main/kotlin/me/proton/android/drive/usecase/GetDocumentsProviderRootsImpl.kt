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

package me.proton.android.drive.usecase

import kotlinx.coroutines.flow.first
import me.proton.android.drive.lock.domain.manager.AppLockManager
import me.proton.core.account.domain.entity.Account
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.drive.documentsprovider.domain.usecase.GetDocumentsProviderRoots
import javax.inject.Inject

class GetDocumentsProviderRootsImpl @Inject constructor(
    private val appLockManager: AppLockManager,
    private val accountManager: AccountManager,
) : GetDocumentsProviderRoots {

    override suspend fun invoke(): List<Account> = takeUnless { appLockManager.isEnabled() }
        ?.let { accountManager.getAccounts().first() }.orEmpty()
}
