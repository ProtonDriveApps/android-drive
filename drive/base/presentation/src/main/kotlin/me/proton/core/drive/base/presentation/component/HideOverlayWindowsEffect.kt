/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.base.presentation.component

import android.os.Build
import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import java.util.WeakHashMap

// Number of effects in composition per window, only accessed on the main thread
private val hideOverlayWindowsRequests = WeakHashMap<Window, Int>()

/**
 * Hides other apps' overlay windows while this effect is in the composition, and shows them again
 * when the last such effect for the same window leaves it, so screens sharing an activity (e.g.
 * during a navigation transition) don't switch hiding off for each other.
 * Requires the `android.permission.HIDE_OVERLAY_WINDOWS` permission.
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HideOverlayWindowsEffect() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        hideOverlayWindowsRequests[window] = hideOverlayWindowsRequests.getOrDefault(window, 0) + 1
        window.setHideOverlayWindows(true)
        onDispose {
            val requests = hideOverlayWindowsRequests.getOrDefault(window, 1) - 1
            if (requests > 0) {
                hideOverlayWindowsRequests[window] = requests
            } else {
                hideOverlayWindowsRequests.remove(window)
                window.setHideOverlayWindows(false)
            }
        }
    }
}
