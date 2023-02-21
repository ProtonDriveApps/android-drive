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

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.presentation.app.AppLifecycleObserver
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.presentation.ui.alert.ForceUpdateActivity
import javax.inject.Inject

class OnForceUpdate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLifecycleObserver: AppLifecycleObserver,
) {

    operator fun invoke(message: String) {
        if (appLifecycleObserver.state.value == AppLifecycleProvider.State.Foreground) {
            context.startActivity(ForceUpdateActivity(context, message).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        }
    }
}