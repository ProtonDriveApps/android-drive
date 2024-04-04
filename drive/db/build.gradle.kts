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
    namespace = "me.proton.android.drive.db"
    sourceSets {
        getByName("test").assets.srcDir("$projectDir/schemas")
    }
}

driveModule(
    hilt = true,
    room = true,
) {
    api(libs.core.account.data)
    api(libs.core.challenge.data)
    api(libs.core.contact.data)
    api(libs.core.crypto.android)
    api(libs.core.eventManager.data) {
        exclude("me.proton.core", "presentation")
        exclude("me.proton.core", "account-manager-presentation")
    }
    api(libs.core.featureFlag.data)
    api(libs.core.humanVerification.data) {
        exclude("me.proton.core", "human-verification-domain")
        exclude("me.proton.core", "util-android-dagger")
    }
    api(libs.core.key.data) {
        isTransitive = false
    }
    api(libs.core.notification.data)
    api(libs.core.observability.data)
    api(libs.core.payment.data) {
        exclude("me.proton.core", "payment-domain")
    }
    api(libs.core.push.data)
    api(libs.core.telemetry.data)
    api(libs.core.user.data) {
        exclude("me.proton.core", "key-data")
        exclude("me.proton.core", "payment-domain")
    }
    api(libs.core.userSettings.data) {
        exclude("me.proton.core", "key-data")
        exclude("me.proton.core", "payment-domain")
    }
    api(libs.core.keyTransparency.data){
        exclude("me.proton.core", "presentation")
        exclude("me.proton.core", "user-settings-domain")
        exclude("me.proton.core", "account-manager-presentation")
        exclude("me.proton.core", "auth-presentation")
    }
    // TODO: Extract from drive db
    api(project(":app-ui-settings"))
    api(project(":drive:announce-event:data"))
    api(project(":drive:backup:data"))
    api(project(":drive:device:data"))
    api(project(":drive:drivelink:data"))
    api(project(":drive:drivelink-download:data"))
    api(project(":drive:drivelink-offline:data"))
    api(project(":drive:drivelink-paged:data"))
    api(project(":drive:drivelink-photo:data"))
    api(project(":drive:drivelink-selection:data"))
    api(project(":drive:drivelink-shared:data"))
    api(project(":drive:drivelink-trash:data"))
    api(project(":drive:feature-flag:data"))
    api(project(":drive:folder:data"))
    api(project(":drive:link:data"))
    api(project(":drive:link-download:data"))
    api(project(":drive:link-node:data"))
    api(project(":drive:link-offline:data"))
    api(project(":drive:link-selection:data"))
    api(project(":drive:link-trash:data"))
    api(project(":drive:link-upload:data"))
    api(project(":drive:message-queue:data"))
    api(project(":drive:notification:data"))
    api(project(":drive:photo:data"))
    api(project(":drive:share:data"))
    api(project(":drive:share-url:base:data"))
    api(project(":drive:sorting:data"))
    api(project(":drive:stats:data"))
    api(project(":drive:user:data"))
    api(project(":drive:volume:data"))
    api(project(":drive:worker:data"))
    api(project(":photos:data"))

    testImplementation(libs.androidx.room.test)
}
