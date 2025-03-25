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
    namespace = "me.proton.core.drive.documentsprovider.domain"
}

driveModule(
    hilt = true,
    serialization = true,
) {
    api(project(":drive:drivelink-crypto:domain"))
    api(project(":drive:drivelink-download:domain"))
    api(project(":drive:drivelink-list:domain"))
    api(project(":drive:drivelink-selection:domain"))
    api(project(":drive:drivelink-upload:domain"))
    api(project(":drive:share:domain"))

    implementation(project(":drive:files:domain"))
    implementation(project(":drive:folder-create"))
    implementation(libs.core.accountManager.domain)
    implementation(libs.core.account.domain)
}
