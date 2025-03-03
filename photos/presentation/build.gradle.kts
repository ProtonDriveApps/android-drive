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

plugins {
    id("com.android.library")
}

android {
    namespace = "me.proton.android.drive.photos.presentation"
}

driveModule(
    hilt = true,
    compose = true,
    i18n = true,
    showkase = true,
    buildConfig = true,
) {
    api(project(":photos:domain"))
    api(project(":photos:data"))
    api(project(":drive:backup:domain"))
    implementation(project(":drive:base:presentation"))
    implementation(project(":drive:files-list"))
    implementation(project(":drive:notification:presentation"))
    implementation(project(":drive:thumbnail:presentation"))
    implementation(project(":drive:user:presentation"))
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.placeholderMaterial)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.core.presentation)
    implementation(libs.core.presentation.compose)
}
