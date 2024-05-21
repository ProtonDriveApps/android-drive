/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.provider

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.ui.MainActivity
import javax.inject.Inject

class DelayedActivityLauncher @Inject constructor(
    @ApplicationContext appContext: Context,
) : ActivityLauncher {
    private val handler = Handler(Looper.getMainLooper())

    init {
        (appContext as Application).registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // do nothing
            }

            override fun onActivityStarted(activity: Activity) {
                // do nothing
            }

            override fun onActivityResumed(activity: Activity) {
                // do nothing
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity is MainActivity) {
                    handler.removeCallbacksAndMessages(null)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                // do nothing
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                // do nothing
            }

            override fun onActivityDestroyed(activity: Activity) {
                // do nothing
            }

        })
    }

    override fun invoke(block: () -> Unit) {
        // Avoid starting activities when MainActivity is finishing
        handler.postDelayed(block, 500)
    }
}
