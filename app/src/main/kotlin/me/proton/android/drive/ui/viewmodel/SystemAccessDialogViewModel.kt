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

import android.annotation.TargetApi
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import me.proton.android.drive.ui.viewevent.SystemAccessDialogViewEvent
import javax.inject.Inject

class SystemAccessDialogViewModel @Inject constructor(

) : ViewModel() {
    fun viewEvent(
        context: Context,
        dismiss: () -> Unit,
    ) = object : SystemAccessDialogViewEvent {
        override val onSettings = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.showBiometricSettings()
            } else {
                context.showSetNewPasswordSettings()
            }
            dismiss()
        }
    }

    private fun Context.showSetNewPasswordSettings() {
        startActivity(Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD))
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun Context.showBiometricSettings() {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Settings.ACTION_BIOMETRIC_ENROLL
        } else {
            @Suppress("DEPRECATION")
            Settings.ACTION_FINGERPRINT_ENROLL
        }
        startActivity(Intent(action))
    }
}
