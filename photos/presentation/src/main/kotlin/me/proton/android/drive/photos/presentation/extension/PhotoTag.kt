/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.presentation.extension

import android.content.Context
import me.proton.android.drive.photos.presentation.viewstate.EmptyPhotoTagState
import me.proton.android.drive.photos.presentation.viewstate.PhotosFilter
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.base.presentation.viewstate.TagViewState
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

val PhotoTag.icon get()= when (this) {
        PhotoTag.Favorites -> CorePresentation.drawable.ic_proton_heart
        PhotoTag.Screenshots -> BasePresentation.drawable.ic_screenshot
        PhotoTag.Videos -> BasePresentation.drawable.ic_video_camera
        PhotoTag.LivePhotos -> BasePresentation.drawable.ic_live
        PhotoTag.MotionPhotos -> BasePresentation.drawable.ic_live
        PhotoTag.Selfies -> CorePresentation.drawable.ic_proton_user
        PhotoTag.Portraits -> CorePresentation.drawable.ic_proton_user_circle
        PhotoTag.Bursts -> BasePresentation.drawable.ic_image_stacked
        PhotoTag.Panoramas -> BasePresentation.drawable.ic_panorama
        PhotoTag.Raw -> BasePresentation.drawable.ic_raw
    }

fun PhotoTag.toPhotosFilter(context: Context, selected: Boolean = false) = when (this) {
    PhotoTag.Favorites -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_favorite),
            icon = icon,
            selected = selected,
        )
    )

    PhotoTag.Screenshots -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_screenshots),
            icon = icon,
            selected = selected,
        )
    )

    PhotoTag.Videos -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_videos),
            icon = icon,
            selected = selected,
        )
    )

    PhotoTag.LivePhotos -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_livephotos),
            icon = icon,
            selected = selected,
        )
    )

    PhotoTag.MotionPhotos -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_motionphotos),
            icon = icon,
            selected = selected,
        )
    )

    PhotoTag.Selfies -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_selfies),
            icon = icon,
            selected = selected,
        )
    )

    PhotoTag.Portraits -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_portraits),
            icon = icon,
            selected = selected,
        )
    )

    PhotoTag.Bursts -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_bursts),
            icon = icon,
            selected = selected,
        )
    )

    PhotoTag.Panoramas -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_panoramas),
            icon = icon,
            selected = selected,
        )
    )

    PhotoTag.Raw -> PhotosFilter(
        filter = this,
        tagViewState = TagViewState(
            label = context.getString(I18N.string.photos_filter_raws),
            icon = icon,
            selected = selected,
        )
    )
}

fun PhotoTag.toEmptyPhotoTagState(): EmptyPhotoTagState = when (this) {
    PhotoTag.Favorites -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_favorite_empty_title,
            descriptionResId = I18N.string.photos_favorite_empty_description,
        )
    )

    PhotoTag.Screenshots -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_screenshots_empty_title,
            descriptionResId = I18N.string.photos_screenshots_empty_description,
        )
    )

    PhotoTag.Videos -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_videos_empty_title,
            descriptionResId = I18N.string.photos_videos_empty_description,
        )
    )

    PhotoTag.LivePhotos -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_livephotos_empty_title,
            descriptionResId = I18N.string.photos_livephotos_empty_description,
        )
    )

    PhotoTag.MotionPhotos -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_motionphotos_empty_title,
            descriptionResId = I18N.string.photos_motionphotos_empty_description,
        )
    )

    PhotoTag.Selfies -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_selfies_empty_title,
            descriptionResId = I18N.string.photos_selfies_empty_description,
        )
    )

    PhotoTag.Portraits -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_portraits_empty_title,
            descriptionResId = I18N.string.photos_portraits_empty_description,
        )
    )

    PhotoTag.Bursts -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_bursts_empty_title,
            descriptionResId = I18N.string.photos_bursts_empty_description,
        )
    )

    PhotoTag.Panoramas -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_portraits_empty_title,
            descriptionResId = I18N.string.photos_panoramas_empty_description,
        )
    )

    PhotoTag.Raw -> EmptyPhotoTagState(
        photoTag = this,
        state = ListContentState.Empty(
            imageResId = icon,
            titleId = I18N.string.photos_raws_empty_title,
            descriptionResId = I18N.string.photos_raws_empty_description,
        )
    )
}
