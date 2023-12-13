/*
 * Copyright (c) 2021-2023 Proton AG.
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
plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "me.proton.android.drive.db.test"
    defaultConfig {
        minSdk = Config.minSdk
        compileSdk = Config.compileSdk
    }
}

driveModule(
    room = true,
) {
    api(project(":drive:db"))
    api(libs.androidx.test.core.ktx)
    api(libs.junit) {
        exclude("org.hamcrest", "hamcrest-core")
        exclude("org.hamcrest", "hamcrest-library")
    }
}
