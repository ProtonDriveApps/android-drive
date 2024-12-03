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
    namespace = "me.proton.core.drive.base.data"
}

driveModule(
    hilt = true,
    workManager = true,
    serialization = true,
    i18n = true,
) {
    api(project(":drive:base:domain"))
    api(libs.core.account.data)
    api(libs.core.data)
    api(libs.core.network.data)
    api(libs.core.network.presentation)
    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.dataStore.preferences)
    implementation(libs.androidx.exif)
    implementation(libs.androidx.paging.common)
    implementation(libs.retrofit)
}
