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
}

android {
    namespace = "me.proton.core.drive.upload.data"
}

driveModule(
    hilt = true,
    room = true,
    workManager = true,
    i18n = true,
    socialTest = true,
) {
    api(project(":drive:base:data"))
    api(project(":drive:upload:domain"))

    implementation(project(":drive:announce-event:domain"))
    implementation(project(":drive:backup:domain"))
    implementation(project(":drive:feature-flag:domain"))
    implementation(project(":drive:folder:domain"))
    implementation(project(":drive:link:data"))
    implementation(project(":drive:link:presentation"))
    implementation(project(":drive:observability:data"))
    implementation(project(":drive:share:data"))
    implementation(project(":drive:worker:data"))
    implementation(project(":verifier:data"))
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.core.crypto)
    implementation(libs.core.network)
    testImplementation(libs.bundles.test.jvm)
}
