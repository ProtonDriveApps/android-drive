/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.feature.flag.domain.entity

import me.proton.core.domain.entity.UserId

sealed class FeatureFlagId(open val userId: UserId, open val id: String) {

    data class Unleash(override val userId: UserId, override val id: String) : FeatureFlagId(userId, id)
    data class Legacy(override val userId: UserId, override val id: String) : FeatureFlagId(userId, id)

    @Suppress("TooManyFunctions")
    companion object {
        const val DRIVE_PHOTOS_UPLOAD_DISABLED = "DrivePhotosUploadDisabled"
        const val DRIVE_SHARING_DEVELOPMENT = "DriveSharingDevelopment"
        const val DRIVE_SHARING_DISABLED = "DriveSharingDisabled"
        const val DRIVE_SHARING_EXTERNAL_INVITATIONS_DISABLED = "DriveSharingExternalInvitationsDisabled"
        const val DRIVE_ANDROID_WHATS_NEW = "DriveAndroidWhatsNew"
        const val DRIVE_ANDROID_USER_LOG_DISABLED = "DriveAndroidUserLogDisabled"
        const val DRIVE_PUBLIC_SHARE_EDIT_MODE = "DrivePublicShareEditMode"
        const val DRIVE_PUBLIC_SHARE_EDIT_MODE_DISABLED = "DrivePublicShareEditModeDisabled"
        const val DRIVE_DYNAMIC_ENTITLEMENT_CONFIGURATION = "DriveDynamicEntitlementConfiguration"
        const val RATING_ANDROID_DRIVE = "RatingAndroidDrive"
        const val DRIVE_THUMBNAIL_WEBP = "DriveThumbnailWebP"
        const val DRIVE_PLUS_PLAN_INTRO = "DrivePlusPlanIntro"
        const val DRIVE_ONE_DOLLAR_PLAN_UPSELL = "DriveOneDollarPlanUpsell"
        const val DRIVE_ANDROID_ALBUMS_PUBLIC_SHARE = "DriveAndroidAlbumsPublicShare"
        const val DOCS_SHEETS_ENABLED = "DocsSheetsEnabled"
        const val DOCS_CREATE_NEW_SHEET_ON_MOBILE_ENABLED = "DocsCreateNewSheetOnMobileEnabled"
        const val DOCS_SHEETS_DISABLED = "DocsSheetsDisabled"
        const val DRIVE_PHOTOS_TAGS_MIGRATION_DISABLED = "DrivePhotosTagsMigrationDisabled"
        const val DRIVE_ANDROID_SDK_UPLOAD_MAIN = "DriveAndroidSDKUploadMain"
        const val DRIVE_ANDROID_SDK_UPLOAD_PHOTO = "DriveAndroidSDKUploadPhoto"
        const val DRIVE_ANDROID_SDK_DOWNLOAD_MAIN = "DriveAndroidSDKDownloadMain"
        const val DRIVE_ANDROID_SDK_DOWNLOAD_PHOTO = "DriveAndroidSDKDownloadPhoto"
        const val DRIVE_ANDROID_SDK_THUMBNAIL_MAIN = "DriveAndroidSDKThumbnailMain"
        const val DRIVE_ANDROID_SDK_THUMBNAIL_PHOTO = "DriveAndroidSDKThumbnailPhoto"
        const val DRIVE_ANDROID_SDK_NODE_OPERATION = "DriveAndroidSDKNodeOperation"
        const val DRIVE_ANDROID_SDK_TRASH = "DriveAndroidSDKTrash"
        const val DRIVE_DOWNLOAD_VERIFICATION_DISABLED = "DriveDownloadVerificationDisabled"
        const val DRIVE_UPLOAD_VERIFICATION_DISABLED = "DriveUploadVerificationDisabled"
        const val DRIVE_ANDROID_DOWNLOAD_FILE_PROGRESS_NOTIFICATION_DISABLED = "DriveAndroidDownloadFileProgressNotificationDisabled"
        const val DRIVE_ANDROID_SUMMER_SALE_2026 = "DriveAndroidSummerSale2026"

        internal var developments : List<String> = listOf(
        )

        fun drivePhotosUploadDisabled(userId: UserId) = Unleash(userId, DRIVE_PHOTOS_UPLOAD_DISABLED)
        fun driveSharingDevelopment(userId: UserId) = Unleash(userId, DRIVE_SHARING_DEVELOPMENT)
        fun driveSharingDisabled(userId: UserId) = Unleash(userId, DRIVE_SHARING_DISABLED)
        fun driveSharingExternalInvitationsDisabled(userId: UserId) = Unleash(userId, DRIVE_SHARING_EXTERNAL_INVITATIONS_DISABLED)
        fun driveAndroidWhatsNew(userId: UserId) = Unleash(userId, DRIVE_ANDROID_WHATS_NEW)
        fun driveAndroidUserLogDisabled(userId: UserId) = Unleash(userId, DRIVE_ANDROID_USER_LOG_DISABLED)
        fun drivePublicShareEditMode(userId: UserId) = Unleash(userId, DRIVE_PUBLIC_SHARE_EDIT_MODE)
        fun drivePublicShareEditModeDisabled(userId: UserId) = Unleash(userId, DRIVE_PUBLIC_SHARE_EDIT_MODE_DISABLED)
        fun driveDynamicEntitlementConfiguration(userId: UserId) = Unleash(userId, DRIVE_DYNAMIC_ENTITLEMENT_CONFIGURATION)
        fun ratingAndroidDrive(userId: UserId) = Legacy(userId, RATING_ANDROID_DRIVE)
        fun driveThumbnailWebP(userId: UserId) = Unleash(userId, DRIVE_THUMBNAIL_WEBP)
        fun drivePlusPlanIntro(userId: UserId) = Unleash(userId, DRIVE_PLUS_PLAN_INTRO)
        fun driveOneDollarPlanUpsell(userId: UserId) = Unleash(userId, DRIVE_ONE_DOLLAR_PLAN_UPSELL)
        fun driveAndroidAlbumsPublicShare(userId: UserId) = Unleash(userId, DRIVE_ANDROID_ALBUMS_PUBLIC_SHARE)
        fun docsSheetsEnabled(userId: UserId) = Unleash(userId, DOCS_SHEETS_ENABLED)
        fun docsCreateNewSheetOnMobileEnabled(userId: UserId) = Unleash(userId, DOCS_CREATE_NEW_SHEET_ON_MOBILE_ENABLED)
        fun docsSheetsDisabled(userId: UserId) = Unleash(userId, DOCS_SHEETS_DISABLED)
        fun drivePhotosTagsMigrationDisabled(userId: UserId) = Unleash(userId, DRIVE_PHOTOS_TAGS_MIGRATION_DISABLED)
        fun driveAndroidSDKUploadMain(userId: UserId) = Unleash(userId, DRIVE_ANDROID_SDK_UPLOAD_MAIN)
        fun driveAndroidSDKUploadPhoto(userId: UserId) = Unleash(userId, DRIVE_ANDROID_SDK_UPLOAD_PHOTO)
        fun driveAndroidSDKDownloadMain(userId: UserId) = Unleash(userId, DRIVE_ANDROID_SDK_DOWNLOAD_MAIN)
        fun driveAndroidSDKDownloadPhoto(userId: UserId) = Unleash(userId, DRIVE_ANDROID_SDK_DOWNLOAD_PHOTO)
        fun driveAndroidSDKThumbnailMain(userId: UserId) = Unleash(userId, DRIVE_ANDROID_SDK_THUMBNAIL_MAIN)
        fun driveAndroidSDKThumbnailPhoto(userId: UserId) = Unleash(userId, DRIVE_ANDROID_SDK_THUMBNAIL_PHOTO)
        fun driveAndroidSDKNodeOperation(userId: UserId) = Unleash(userId, DRIVE_ANDROID_SDK_NODE_OPERATION)
        fun driveAndroidSDKTrash(userId: UserId) = Unleash(userId, DRIVE_ANDROID_SDK_TRASH)
        fun driveDownloadVerificationDisabled(userId: UserId) = Unleash(userId, DRIVE_DOWNLOAD_VERIFICATION_DISABLED)
        fun driveUploadVerificationDisabled(userId: UserId) = Unleash(userId, DRIVE_UPLOAD_VERIFICATION_DISABLED)
        fun driveAndroidDownloadFileProgressNotificationDisabled(userId: UserId) = Unleash(userId, DRIVE_ANDROID_DOWNLOAD_FILE_PROGRESS_NOTIFICATION_DISABLED)
        fun driveAndroidSummerSale2026(userId: UserId) = Unleash(userId, DRIVE_ANDROID_SUMMER_SALE_2026)
    }
}
