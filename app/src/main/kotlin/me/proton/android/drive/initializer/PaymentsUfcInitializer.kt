/*
 * Copyright (c) 2026 Proton AG.
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
import me.proton.android.drive.payments.HttpCapabilityImpl
import me.proton.android.drive.payments.StoreCapabilityImpl
import me.proton.android.payment.Payments
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.presentation.app.AppLifecycleProvider

class PaymentsUfcInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with(
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                PaymentsUfcInitializerEntryPoint::class.java,
            )
        ) {
            Payments
                .registerHttp(httpCapability)
                .registerStore(storeCapability)
                .init(context.applicationContext)

            // Keep the HTTP capability's user in sync with the ready account, so it doesn't have to
            // resolve it from AccountManager on every call.
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.CREATED)
                .onAccountReady { account -> httpCapability.setUserId(account.userId) }
                .onAccountRemoved { account -> httpCapability.resetUserId(account.userId) }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PaymentsUfcInitializerEntryPoint {
        val httpCapability: HttpCapabilityImpl
        val storeCapability: StoreCapabilityImpl
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
    }
}
