/*
 * Copyright (c) 2021-2024 Proton AG.
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
}

android {
    namespace = "me.proton.core.drive.drivelink.download.data.test"
}

driveModule(
    hilt = true,
    socialTest = true,
) {
    api(project(":drive:drivelink-download:domain"))
    implementation(project(":drive:drivelink-download:data"))

    implementation(project(":drive:base:data"))
    implementation(project(":drive:drivelink:data"))
    implementation(project(":drive:file:base:domain"))
    implementation(project(":drive:link-node:domain"))
    implementation(project(":drive:worker:data"))
    implementation(libs.androidx.work.testing)
    implementation(libs.androidx.test.core.ktx)
    implementation(libs.dagger.hilt.android.testing)
}
