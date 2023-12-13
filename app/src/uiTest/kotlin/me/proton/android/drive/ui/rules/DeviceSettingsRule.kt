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

package me.proton.android.drive.ui.rules

import android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
import android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE
import android.provider.Settings.Global.WINDOW_ANIMATION_SCALE
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

class DeviceSettingsRule(
    private val animationScale: Float = 0.0F,
    private val transitionAnimationScale: Float = 0.0F,
    private val animatorDurationScale: Float = 0.0F
) : ExternalResource() {
    override fun before() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.run {
            val cmd = "settings put global"
            executeShellCommand("$cmd $WINDOW_ANIMATION_SCALE $animationScale")
            executeShellCommand("$cmd $TRANSITION_ANIMATION_SCALE $transitionAnimationScale")
            executeShellCommand("$cmd $ANIMATOR_DURATION_SCALE $animatorDurationScale")
        }
    }
}