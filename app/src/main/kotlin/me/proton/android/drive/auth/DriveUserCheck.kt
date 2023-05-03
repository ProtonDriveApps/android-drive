/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.android.drive.auth

import android.content.Context
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.auth.domain.usecase.PostLoginAccountSetup
import me.proton.core.auth.presentation.DefaultUserCheck
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import me.proton.core.drive.i18n.R as I18N

/**
 * Drive API requires following scopes: "full", "nondelinquent" and "drive".
 * During the sign in process we check for "full" and "drive" scope. Delinquent user is
 * checked within [DefaultUserCheck].
 */
class DriveUserCheck(
    private val context: Context,
    private val sessionManager: SessionManager,
    accountManager: AccountManager,
    userManager: UserManager,
) : DefaultUserCheck(context, accountManager, userManager) {

    override suspend fun invoke(user: User): PostLoginAccountSetup.UserCheckResult =
        if (!sessionManager.hasDriveScope(user.userId)) {
            errorNoDriveScope()
        } else {
            super.invoke(user)
        }

    private suspend fun SessionManager.hasDriveScope(userId: UserId): Boolean =
        getSessionId(userId)?.let { sessionId ->
            getSession(sessionId)?.run {
                scopes.containsAll(listOf(SCOPE_FULL, SCOPE_DRIVE))
            }
        } ?: false

    private fun errorNoDriveScope() = PostLoginAccountSetup.UserCheckResult.Error(
        localizedMessage = context.getString(I18N.string.description_no_drive_scope)
    )

    companion object {
        private const val SCOPE_DRIVE = "drive"
        private const val SCOPE_FULL = "full"
    }
}
