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

data class FeatureFlagId(val userId: UserId, val id: String) {

    companion object {
        const val DRIVE_PHOTOS_UPLOAD_DISABLED = "DrivePhotosUploadDisabled"
        const val DRIVE_SHARING_DEVELOPMENT = "DriveSharingDevelopment"
        const val DRIVE_SHARING_DISABLED = "DriveSharingDisabled"
        const val DRIVE_SHARING_EDITING_DISABLED = "DriveSharingEditingDisabled"
        const val DRIVE_SHARING_EXTERNAL_INVITATIONS_DISABLED = "DriveSharingExternalInvitationsDisabled"
        const val DRIVE_DOCS_DISABLED = "DriveDocsDisabled"
        const val DRIVE_ANDROID_WHATS_NEW = "DriveAndroidWhatsNew"
        const val DRIVE_ANDROID_USER_LOG_DISABLED = "DriveAndroidUserLogDisabled"
        const val DRIVE_PUBLIC_SHARE_EDIT_MODE = "DrivePublicShareEditMode"
        const val DRIVE_PUBLIC_SHARE_EDIT_MODE_DISABLED = "DrivePublicShareEditModeDisabled"
        const val DRIVE_DYNAMIC_ENTITLEMENT_CONFIGURATION = "DriveDynamicEntitlementConfiguration"
        const val RATING_ANDROID_DRIVE = "RatingAndroidDrive"
        const val DRIVE_ALBUMS_DISABLED = "DriveAlbumsDisabled"
        const val DRIVE_THUMBNAIL_WEBP = "DriveThumbnailWebP"
        const val DRIVE_PLUS_PLAN_INTRO = "DrivePlusPlanIntro"
        const val DRIVE_ONE_DOLLAR_PLAN_UPSELL = "DriveOneDollarPlanUpsell"
        const val DRIVE_ANDROID_ALBUMS_PUBLIC_SHARE = "DriveAndroidAlbumsPublicShare"
        const val DOCS_SHEETS_ENABLED = "DocsSheetsEnabled"
        const val DOCS_CREATE_NEW_SHEET_ON_MOBILE_ENABLED = "DocsCreateNewSheetOnMobileEnabled"
        const val DOCS_SHEETS_DISABLED = "DocsSheetsDisabled"
        const val DRIVE_PHOTOS_TAGS_MIGRATION = "DrivePhotosTagsMigration"
        const val DRIVE_PHOTOS_TAGS_MIGRATION_DISABLED = "DrivePhotosTagsMigrationDisabled"
        const val DRIVE_ANDROID_DOWNLOAD_MANAGER = "DriveAndroidDownloadManager"

        internal var developments : List<String> = listOf(
        )

        fun drivePhotosUploadDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_PHOTOS_UPLOAD_DISABLED)
        fun driveSharingDevelopment(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_DEVELOPMENT)
        fun driveSharingDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_DISABLED)
        fun driveSharingEditingDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_EDITING_DISABLED)
        fun driveSharingExternalInvitationsDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_EXTERNAL_INVITATIONS_DISABLED)
        fun driveDocsDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_DOCS_DISABLED)
        fun driveAndroidWhatsNew(userId: UserId) = FeatureFlagId(userId, DRIVE_ANDROID_WHATS_NEW)
        fun driveAndroidUserLogDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_ANDROID_USER_LOG_DISABLED)
        fun drivePublicShareEditMode(userId: UserId) = FeatureFlagId(userId, DRIVE_PUBLIC_SHARE_EDIT_MODE)
        fun drivePublicShareEditModeDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_PUBLIC_SHARE_EDIT_MODE_DISABLED)
        fun driveDynamicEntitlementConfiguration(userId: UserId) = FeatureFlagId(userId, DRIVE_DYNAMIC_ENTITLEMENT_CONFIGURATION)
        fun ratingAndroidDrive(userId: UserId) = FeatureFlagId(userId, RATING_ANDROID_DRIVE)
        fun driveAlbumsDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_ALBUMS_DISABLED)
        fun driveThumbnailWebP(userId: UserId) = FeatureFlagId(userId, DRIVE_THUMBNAIL_WEBP)
        fun drivePlusPlanIntro(userId: UserId) = FeatureFlagId(userId, DRIVE_PLUS_PLAN_INTRO)
        fun driveOneDollarPlanUpsell(userId: UserId) = FeatureFlagId(userId, DRIVE_ONE_DOLLAR_PLAN_UPSELL)
        fun driveAndroidAlbumsPublicShare(userId: UserId) = FeatureFlagId(userId, DRIVE_ANDROID_ALBUMS_PUBLIC_SHARE)
        fun docsSheetsEnabled(userId: UserId) = FeatureFlagId(userId, DOCS_SHEETS_ENABLED)
        fun docsCreateNewSheetOnMobileEnabled(userId: UserId) = FeatureFlagId(userId, DOCS_CREATE_NEW_SHEET_ON_MOBILE_ENABLED)
        fun docsSheetsDisabled(userId: UserId) = FeatureFlagId(userId, DOCS_SHEETS_DISABLED)
        fun drivePhotosTagsMigration(userId: UserId) = FeatureFlagId(userId, DRIVE_PHOTOS_TAGS_MIGRATION)
        fun drivePhotosTagsMigrationDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_PHOTOS_TAGS_MIGRATION_DISABLED)
        fun driveAndroidDownloadManager(userId: UserId) = FeatureFlagId(userId, DRIVE_ANDROID_DOWNLOAD_MANAGER)
    }
}
