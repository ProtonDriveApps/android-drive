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

plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    namespace = "me.proton.android.drive.baselineprofile"

    flavorDimensions.add("default")
    productFlavors {
        create("dev")
    }
}

baselineProfile {
    useConnectedDevices = true
}

driveTestModule(targetProjectPath = ":app") {
    add("implementation", libs.androidx.benchmark.macro.junit4)
    add("implementation", libs.androidx.test.runner)
    add("implementation", libs.androidx.test.uiautomator)
    add("implementation", libs.junit)
}
