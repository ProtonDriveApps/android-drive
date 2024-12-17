/*
 * Copyright (c) 2023-2024 Proton AG.
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
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import me.proton.core.auth.presentation.MissingScopeInitializer
import me.proton.core.crypto.validator.presentation.init.CryptoValidatorInitializer
import me.proton.core.humanverification.presentation.HumanVerificationInitializer
import me.proton.core.network.presentation.init.UnAuthSessionFetcherInitializer
import me.proton.core.paymentiap.presentation.GooglePurchaseHandlerInitializer
import me.proton.core.plan.presentation.PurchaseHandlerInitializer
import me.proton.core.plan.presentation.UnredeemedPurchaseInitializer

class MainInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // No-op needed
    }

    override fun dependencies() = listOf(
        SentryInitializer::class.java,
        LoggerInitializer::class.java,
        UncaughtExceptionHandlerInitializer::class.java,
        FeatureFlagInitializer::class.java,
        EntitlementInitializer::class.java,
        AccountStateHandlerInitializer::class.java,
        AccountRemovedHandlerInitializer::class.java,
        NotificationChannelInitializer::class.java,
        DocumentsProviderInitializer::class.java,
        CryptoValidatorInitializer::class.java,
        EventManagerInitializer::class.java,
        HumanVerificationInitializer::class.java,
        UnredeemedPurchaseInitializer::class.java,
        PurchaseHandlerInitializer::class.java,
        GooglePurchaseHandlerInitializer::class.java,
        MissingScopeInitializer::class.java,
        UnAuthSessionFetcherInitializer::class.java,
        AutoLockInitializer::class.java,
        BackupInitializer::class.java,
        ShareInitializer::class.java,
        TelemetryInitializer::class.java,
        CleanupInitializer::class.java,
        PingActiveUserInitializer::class.java,
        LogInterceptorInitializer::class.java,
        AccountReadyObserverInitializer::class.java,
        FirstAppUsageInitializer::class.java,
        UploadInitializer::class.java,
    )

    companion object {

        fun init(appContext: Context) {
            with(AppInitializer.getInstance(appContext)) {
                // WorkManager need to be initialized before any other dependant initializer.
                initializeComponent(WorkManagerInitializer::class.java)
                initializeComponent(MainInitializer::class.java)
            }
        }
    }
}
