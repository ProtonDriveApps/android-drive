/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.android.drive.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal val targetPackageName: String
    get() = requireNotNull(InstrumentationRegistry.getArguments().getString("androidx.benchmark.targetPackageName")) {
        "The androidx.benchmark.targetPackageName instrumentation argument is missing"
    }

internal enum class HomeTab(val label: String) {
    FILES("Files"),
    PHOTOS("Photos"),
    COMPUTERS("Computers"),
    SHARED("Shared"),
}

internal fun MacrobenchmarkScope.startToHomeTab() {
    pressHome()
    startActivityAndWait()
    device.waitForHomeTabs()
}

internal fun MacrobenchmarkScope.waitForContent() {
    check(device.wait(Until.hasObject(By.scrollable(true)), CONTENT_TIMEOUT_MILLIS)) {
        "The start home tab showed no content: pick a default home tab with files or photos on the device"
    }
}

internal fun MacrobenchmarkScope.browseHomeTabs() {
    HomeTab.entries.forEach { tab ->
        device.homeTab(tab).click()
        device.waitForIdle()
        device.scrollContent()
    }
}

private fun UiDevice.waitForHomeTabs() {
    check(wait(Until.hasObject(By.text(HomeTab.FILES.label)), HOME_TIMEOUT_MILLIS)) {
        "The home tabs did not appear: log in to the app on the device before running this"
    }
}

private fun UiDevice.homeTab(tab: HomeTab): UiObject2 =
    findObjects(By.text(tab.label)).maxByOrNull { label -> label.visibleBounds.top }
        ?: error("Home tab ${tab.label} not found")

private fun UiDevice.scrollContent() {
    if (!wait(Until.hasObject(By.scrollable(true)), CONTENT_TIMEOUT_MILLIS)) return
    val x = displayWidth / 2
    val top = displayHeight / SCROLL_EDGE_DIVISOR
    val bottom = displayHeight - top
    swipe(x, bottom, x, top, SWIPE_STEPS)
    waitForIdle()
    swipe(x, top, x, bottom, SWIPE_STEPS)
    waitForIdle()
}

private const val HOME_TIMEOUT_MILLIS = 10_000L
private const val CONTENT_TIMEOUT_MILLIS = 5_000L
private const val SCROLL_EDGE_DIVISOR = 4
private const val SWIPE_STEPS = 20
