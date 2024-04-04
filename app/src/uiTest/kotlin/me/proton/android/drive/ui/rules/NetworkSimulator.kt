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

package me.proton.android.drive.ui.rules

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.log.DriveLogTag
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager.Connectivity.NONE
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager.Connectivity.UNMETERED
import me.proton.core.util.kotlin.CoreLogger
import me.proton.test.fusion.FusionConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.rules.ExternalResource
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Object responsible for simulating network behavior for testing purposes.
 */
object NetworkSimulator: ExternalResource() {

    private val testNetworkInterceptor
        get() = Interceptor {
            if (responseDelay > 0.milliseconds) {
                CoreLogger.d(
                    DriveLogTag.UI_TEST,
                    "Delaying response by ${responseDelay.inWholeMilliseconds}ms"
                )
                runBlocking {
                    delay(responseDelay)
                }
            }

            throwIf(isNetworkTimeout, SocketTimeoutException("Simulated network timeout"))

            runBlocking {
                throwIf(connectivity.first() == NONE, UnknownHostException("Simulated disabled network"))
            }

            it.proceed(it.request())
        }

    private val interceptors = mutableListOf(testNetworkInterceptor)
    private var isNetworkTimeout: Boolean = false
    private var responseDelay: Duration = 0.milliseconds

    val client: OkHttpClient = OkHttpClient
        .Builder()
        .apply {
            interceptors.forEach { addInterceptor(it) }
        }
        .build()

    private var _connectivity = MutableStateFlow<BackupConnectivityManager.Connectivity?>(null)
    val connectivity: Flow<BackupConnectivityManager.Connectivity?>
        get() = _connectivity

    fun disableNetwork() = setConnectivity(NONE)

    fun enableNetwork(connectivity: BackupConnectivityManager.Connectivity = UNMETERED) =
        setConnectivity(connectivity)

    fun disableNetworkFor(duration: Duration, block : () -> Unit) {
        disableNetwork()

        block()

        val durationMillis = duration.inWholeMilliseconds
        val start = System.currentTimeMillis()

        FusionConfig.Compose.testRule.get().waitUntil((durationMillis * 1.1F).toLong()){
            System.currentTimeMillis() - start > durationMillis
        }

        enableNetwork()
    }

    fun setNetworkTimeout(isNetworkTimeout: Boolean) = apply {
        NetworkSimulator.isNetworkTimeout = isNetworkTimeout
    }

    private fun setConnectivity(connectivity: BackupConnectivityManager.Connectivity) = apply {
        _connectivity.value = connectivity
    }

    private fun throwIf(check: Boolean, throwable: Throwable) =
        if (!check) true else throw throwable
}
