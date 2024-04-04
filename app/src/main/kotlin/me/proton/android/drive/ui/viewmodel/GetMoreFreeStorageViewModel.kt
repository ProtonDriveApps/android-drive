/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform
import me.proton.android.drive.ui.viewstate.GetMoreFreeStorageViewState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.folder.domain.usecase.HasAnyCachedFolderChildren
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShares
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
class GetMoreFreeStorageViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    configurationProvider: ConfigurationProvider,
    getShares: GetShares,
    private val hasAnyCachedFolderChildren: HasAnyCachedFolderChildren,
    private val broadcastMessages: BroadcastMessages,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val uploadAFile = GetMoreFreeStorageViewState.Action(
        iconResId = uploadAFileIconResId(false),
        titleResId = I18N.string.get_more_free_storage_action_upload_title,
        getDescription = { AnnotatedString(appContext.getString(I18N.string.get_more_free_storage_action_upload_subtitle)) },
        isDone = false,
    )

    private val createAShareLink = GetMoreFreeStorageViewState.Action(
        iconResId = createAShareLinkIconResId(false),
        titleResId = I18N.string.get_more_free_storage_action_link_title,
        getDescription = { createAShareLinkDescription },
        isDone = false,
    )

    private val setARecoveryMethod = GetMoreFreeStorageViewState.Action(
        iconResId = setARecoveryMethodIconResId(false),
        titleResId = I18N.string.get_more_free_storage_action_recovery_title,
        getDescription = { setARecoveryMethodDescription(ProtonTheme.colors.textAccent) },
        isDone = false,
        onSubtitleClick = ::onSetRecoverMethodSubtitleClick
    )

    val initialViewState = GetMoreFreeStorageViewState(
        imageResId = BasePresentation.drawable.img_free_storage,
        title = appContext.getString(
            I18N.string.get_more_free_storage_title,
            configurationProvider.maxFreeSpace.asHumanReadableString(appContext, numberOfDecimals = 0),
        ),
        descriptionResId = I18N.string.get_more_free_storage_description,
        actions = listOf(uploadAFile, createAShareLink, setARecoveryMethod),
    )

    val viewState: Flow<GetMoreFreeStorageViewState> = getShares(userId, Share.Type.STANDARD, flowOf(false))
        .distinctUntilChanged()
        .transform { result ->
            when (result) {
                is DataResult.Success -> emit(
                    initialViewState.copy(
                        actions = listOf(
                            hasAnyCachedFolderChildren(userId, filesOnly = true).let {
                                uploadAFile.copy(
                                    iconResId = uploadAFileIconResId(false),
                                    isDone = false,
                                )
                            },
                            result.value.isNotEmpty().let {
                                createAShareLink.copy(
                                    iconResId = createAShareLinkIconResId(false),
                                    isDone = false,
                                )
                            },
                            setARecoveryMethod,
                        )
                    )
                )
                else -> Unit
            }

        }

    private val isDoneIconResId = CorePresentation.drawable.ic_proton_checkmark

    @Suppress("SameParameterValue")
    private fun uploadAFileIconResId(isDone: Boolean): Int =
        if (isDone) isDoneIconResId else CorePresentation.drawable.ic_proton_arrow_up_line

    @Suppress("SameParameterValue")
    private fun createAShareLinkIconResId(isDone: Boolean): Int =
        if (isDone) isDoneIconResId else CorePresentation.drawable.ic_proton_link

    @Suppress("SameParameterValue")
    private fun setARecoveryMethodIconResId(isDone: Boolean): Int =
        if (isDone) isDoneIconResId else CorePresentation.drawable.ic_proton_key

    private val createAShareLinkDescription: AnnotatedString get() {
        val getLink = appContext.getString(I18N.string.common_get_link_action)
        val description = appContext.getString(I18N.string.get_more_free_storage_action_link_subtitle).format(getLink)
        val start = description.indexOf(getLink)
        val spanStyles = listOf(
            AnnotatedString.Range(
                SpanStyle(fontWeight = FontWeight.Bold),
                start = start,
                end = start + getLink.length
            )
        )
        return AnnotatedString(text = description, spanStyles = spanStyles)
    }

    private fun setARecoveryMethodDescription(linkColor: Color): AnnotatedString {
        val urlString = appContext.getString(I18N.string.get_more_free_storage_action_recovery_subtitle_url_string)
        val description = appContext
            .getString(I18N.string.get_more_free_storage_action_recovery_subtitle)
            .format(urlString)
        val start = description.indexOf(urlString)
        val spanStyles = listOf(
            AnnotatedString.Range(
                SpanStyle(color = linkColor),
                start = start,
                end = start + urlString.length
            )
        )
        return AnnotatedString(text = description, spanStyles = spanStyles)
    }

    private fun onSetRecoverMethodSubtitleClick(offset: Int) {
        val urlString = appContext.getString(I18N.string.get_more_free_storage_action_recovery_subtitle_url_string)
        val description = appContext
            .getString(I18N.string.get_more_free_storage_action_recovery_subtitle)
            .format(urlString)
        val start = description.indexOf(urlString)
        val linkRange = IntRange(
            start = start,
            endInclusive = start + urlString.length,
        )
        if (offset in linkRange) {
            try {
                val url = appContext
                    .getString(I18N.string.get_more_free_storage_action_recovery_subtitle_url)
                    .format(urlString)
                appContext.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (ignored: ActivityNotFoundException) {
                broadcastMessages(
                    userId = userId,
                    message = appContext.getString(I18N.string.common_error_no_browser_available),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
        }
    }
}
