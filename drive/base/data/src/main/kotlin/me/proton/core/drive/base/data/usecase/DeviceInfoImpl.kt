/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.base.data.usecase

import android.os.Build
import android.os.LocaleList
import me.proton.core.drive.base.domain.provider.BuildConfigFieldsProvider
import me.proton.core.drive.base.domain.usecase.DeviceInfo
import javax.inject.Inject

class DeviceInfoImpl @Inject constructor(
    private val buildConfigFieldsProvider: BuildConfigFieldsProvider,
) : DeviceInfo {
    override fun invoke(block: (String) -> Unit) {
        block("-----------------------------------------")
        block("OS:          Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        block("DEVICE:      ${Build.MANUFACTURER} ${Build.MODEL}")
        block("FINGERPRINT: ${Build.FINGERPRINT}")
        block("ABI:         ${Build.SUPPORTED_ABIS.joinToString(",")}")
        block("LOCALE(S):   ${LocaleList.getDefault().toLanguageTags()}")
        block("APP VERSION: ${buildConfigFieldsProvider.buildConfigFields.appVersionName}")
        block("-----------------------------------------")
    }
}
