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

package me.proton.android.drive.initializer

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.drive.link.selection.domain.usecase.CleanUpSelections
import me.proton.core.presentation.app.AppLifecycleProvider

@Suppress("unused")
class SelectionInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with (
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                SelectionInitializerEntryPoint::class.java
            )
        ) {
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.STARTED)
                .onAccountReady { account ->
                    cleanUpSelections(account.userId)
                }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        AccountStateHandlerInitializer::class.java,
        LoggerInitializer::class.java,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SelectionInitializerEntryPoint {
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
        val cleanUpSelections: CleanUpSelections
    }
}
