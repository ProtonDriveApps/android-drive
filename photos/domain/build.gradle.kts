/*
 * Copyright (c) 2023-2024 Proton AG.
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
    namespace = "me.proton.android.drive.photos.domain"
}


driveModule(
    hilt = true,
    socialTest = true,
) {
    api(project(":drive:announce-event:domain"))
    api(project(":drive:backup:domain"))
    api(project(":drive:base:domain"))
    api(project(":drive:drivelink-list:domain"))
    api(project(":drive:drivelink-photo:domain"))
    api(project(":drive:drivelink-selection:domain"))
    api(project(":drive:files:domain"))
    api(project(":drive:share-crypto:domain"))
    api(project(":drive:stats:domain"))
    testImplementation(project(":photos:data"))
}
