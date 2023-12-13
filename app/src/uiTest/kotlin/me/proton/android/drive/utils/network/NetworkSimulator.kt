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

package me.proton.android.drive.utils.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.log.DriveLogTag
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import me.proton.core.util.kotlin.CoreLogger
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Object responsible for simulating network behavior for testing purposes.
 */
object NetworkSimulator {
    var interceptors = listOf(testNetworkInterceptor)
    var isNetworkDisabled: Boolean = false
    var isNetworkTimeout: Boolean = false
    var responseDelay: Duration = 0.milliseconds

    /**
    * An interceptor that allows simulation of various network conditions.
    */
    private val testNetworkInterceptor
        get() = Interceptor {

            /** Introduce a delay if one has been specified **/
            if (responseDelay > 0.milliseconds) {
                CoreLogger.d(
                    DriveLogTag.UI_TEST,
                    "Delaying response by ${responseDelay.inWholeMilliseconds}ms"
                )
                runBlocking {
                    delay(responseDelay)
                }
            }

            /** Simulate network timeout **/
            throwIf(isNetworkTimeout, SocketTimeoutException("Simulated network timeout"))

            /** Simulate no internet connection **/
            throwIf(isNetworkDisabled, UnknownHostException("Simulated disabled network"))

            it.proceed(it.request())
        }

    val client: OkHttpClient = OkHttpClient
        .Builder()
        .apply {
            interceptors.forEach { addInterceptor(it) }
        }
        .build()

    /** Test network manager for simulating network status **/
    val testNetworkManager = object : NetworkManager() {
        override val networkStatus: NetworkStatus =
            if (isNetworkDisabled) NetworkStatus.Disconnected else NetworkStatus.Unmetered

        override fun register() {}

        override fun unregister() {}
    }

    /** Utility function to throw a specific exception if the given condition is met. */
    private fun throwIf(check: Boolean, throwable: Throwable) =
        if (!check) true else throw throwable
}
