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
package me.proton.android.drive.lock.data.provider

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED
import androidx.biometric.BiometricManager.BIOMETRIC_STATUS_UNKNOWN
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import me.proton.android.drive.lock.domain.exception.LockException
import me.proton.android.drive.lock.domain.lock.LockState
import me.proton.core.drive.base.domain.util.coRunCatching
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class BiometricPromptProviderImpl @Inject constructor(
    private val biometricManager: BiometricManager,
) : BiometricPromptProvider {
    private val listeners = ConcurrentHashMap<AuthenticationListener, Unit>()
    private val activities: MutableList<WeakReference<FragmentActivity>> = mutableListOf()
    private val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            listeners.keys().toList().forEach { listener ->
                listener.onError(LockException.BiometricAuthenticationError(errString.toString()))
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            listeners.keys().toList().forEach { listener -> listener.onSuccess(result) }
        }
    }

    interface AuthenticationListener {
        fun onSuccess(result: BiometricPrompt.AuthenticationResult)
        fun onError(error: LockException)
    }

    override fun bindToActivity(activity: FragmentActivity) {
        activities.flush().add(WeakReference(activity))
    }

    private fun buildBiometricPrompt(): BiometricPrompt {
        val activity = activities.flush().firstResumedOrNull()
        requireNotNull(activity)
        val executor = ContextCompat.getMainExecutor(activity)
        return BiometricPrompt(
            activity,
            executor,
            callback,
        )
    }

    private fun List<WeakReference<FragmentActivity>>.firstResumedOrNull(): FragmentActivity? =
        firstOrNull { weakReference ->
            val activity = weakReference.get()
            activity != null && activity.lifecycle.currentState == Lifecycle.State.RESUMED
        }?.get()

    private fun MutableList<WeakReference<FragmentActivity>>.flush(): MutableList<WeakReference<FragmentActivity>> =
        this.apply {
            removeAll { weakReference ->
                val activity = weakReference.get()
                activity == null || activity.lifecycle.currentState == Lifecycle.State.DESTROYED
            }
        }

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        cryptoObject: CryptoObject?
    ): Result<BiometricPrompt.AuthenticationResult> = coRunCatching {
        cryptoObject?.let {
            buildBiometricPrompt().authenticate(biometricPromptInfo(title, subtitle), cryptoObject)
        } ?: buildBiometricPrompt().authenticate(biometricPromptInfo(title, subtitle))

        // await result
        suspendCancellableCoroutine<Result<BiometricPrompt.AuthenticationResult>> { continuation ->
            val listener = object : AuthenticationListener {
                override fun onSuccess(result: BiometricPrompt.AuthenticationResult) {
                    listeners.remove(this)
                    continuation.resume(Result.success(result))
                }

                override fun onError(error: LockException) {
                    listeners.remove(this)
                    continuation.resume(Result.failure(error))
                }
            }
            listeners[listener] = Unit
            continuation.invokeOnCancellation {
                listeners.remove(listener)
            }
        }.getOrThrow()
    }

    override fun getLockState(): LockState =
        when (val result = biometricManager.canAuthenticate(allowedAuthenticators)) {
            BIOMETRIC_SUCCESS -> LockState.Ready
            BIOMETRIC_STATUS_UNKNOWN -> LockState.Ready
            BIOMETRIC_ERROR_UNSUPPORTED -> LockState.NotAvailable
            BIOMETRIC_ERROR_HW_UNAVAILABLE -> LockState.Ready
            BIOMETRIC_ERROR_NONE_ENROLLED -> LockState.SetupRequired
            BIOMETRIC_ERROR_NO_HARDWARE -> LockState.NotAvailable
            BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> LockState.NotAvailable
            else -> error("Unhandled BiometricManager.canAuthenticate result $result")
        }

    private fun biometricPromptInfo(title: String, subtitle: String): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()
    }

    private val allowedAuthenticators: Int get() = when (Build.VERSION.SDK_INT) {
        Build.VERSION_CODES.P, Build.VERSION_CODES.Q -> BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        else -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    }
}
