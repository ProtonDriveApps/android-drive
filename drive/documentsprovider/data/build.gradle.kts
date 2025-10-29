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
    namespace = "me.proton.core.drive.documentsprovider.data"
}

driveModule(
    hilt = true,
    serialization = true,
    workManager = true,
    i18n = true,
) {
    api(project(":drive:documentsprovider:domain"))
    implementation(project(":drive:announce-event:domain"))
    implementation(project(":drive:base:data"))
    implementation(project(":drive:drivelink-rename:domain"))
    implementation(project(":drive:drivelink-selection:domain"))
    implementation(project(":drive:trash:domain"))
    implementation(libs.core.account)
    implementation(libs.androidx.paging.runtime.ktx)
}
