/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.drivelink.shared.presentation.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.SHARE
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.usecase.CopyToClipboard
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.hasShareLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.shared.domain.extension.sharingDetails
import me.proton.core.drive.drivelink.shared.domain.usecase.GetOrCreateSharedDriveLink
import me.proton.core.drive.drivelink.shared.domain.usecase.GetSharedDriveLink
import me.proton.core.drive.drivelink.shared.presentation.extension.toViewState
import me.proton.core.drive.drivelink.shared.presentation.viewevent.ManageAccessViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.LoadingViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ManageAccessViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserType
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingDisabled
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.usecase.GetShareUsers
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("StaticFieldLeak")
class ManageAccessViewModel @Inject constructor(
    getDriveLink: GetDecryptedDriveLink,
    private val getSharedDriveLink: GetSharedDriveLink,
    private val getOrCreateSharedDriveLink: GetOrCreateSharedDriveLink,
    getShareUsers: GetShareUsers,
    private val savedStateHandle: SavedStateHandle,
    private val copyToClipboard: CopyToClipboard,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
    @ApplicationContext private val appContext: Context,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId: ShareId = ShareId(userId, savedStateHandle.require(SHARE_ID))
    private val linkId: LinkId = FileId(shareId, savedStateHandle.require(LINK_ID))
    private val retryTrigger = MutableSharedFlow<Unit>()
    private val driveLink = getDriveLink(linkId, failOnDecryptionError = false)
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val createSharedDriveLink = retryTrigger.transformLatest {
        emit(DataResult.Processing(ResponseSource.Local))
        emitAll(
            driveLink.filterNotNull().take(1).transformLatest { driveLink ->
                emitAll(getOrCreateSharedDriveLink(driveLink))
            }
        )
    }
    private val sharedDriveLink = driveLink.filterNotNull().transformLatest { driveLink ->
        emitAll(getSharedDriveLink(driveLink))
    }.onEach { dataResult ->
        dataResult.onFailure { error ->
            if (error.cause !is NoSuchElementException) {
                error.cause?.log(SHARING)
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        appContext,
                        configurationProvider.useExceptionMessage
                    ),
                    type = BroadcastMessage.Type.WARNING,
                )
            }
        }
    }
    private val sharedLoadingViewState = combine(
        driveLink.filterNotNull(),
        createSharedDriveLink,
    ) { driveLink, sharedDriveLink ->
        when (sharedDriveLink) {
            is DataResult.Processing -> LoadingViewState.Loading(driveLink.toLoadingMessage())
            is DataResult.Error -> {
                sharedDriveLink.cause?.log(SHARE)
                if (sharedDriveLink.cause is NoSuchElementException) {
                    LoadingViewState.Initial
                } else {
                    if (sharedDriveLink.isRetryable) {
                        LoadingViewState.Error.Retryable(
                            sharedDriveLink.getDefaultMessage(
                                appContext,
                                configurationProvider.useExceptionMessage
                            )
                        )
                    } else {
                        LoadingViewState.Error.NonRetryable(
                            sharedDriveLink.getDefaultMessage(
                                appContext,
                                configurationProvider.useExceptionMessage
                            ),
                            1.seconds,
                        )
                    }
                }
            }

            is DataResult.Success -> if (sharedDriveLink.value.isLegacy) {
                LoadingViewState.Error.NonRetryable(
                    appContext.getString(I18N.string.shared_link_error_legacy_link),
                    0.seconds,
                )
            } else {
                LoadingViewState.Available(driveLink = driveLink)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LoadingViewState.Initial)

    private val shareUsers = getShareUsers(linkId).transformLatest { dataResult ->
        dataResult.onSuccess { list ->
            emit(list)
        }.onFailure { error ->
            emit(emptyList())
            if (error.cause !is NoSuchElementException) {
                error.cause?.log(SHARING)
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        appContext,
                        configurationProvider.useExceptionMessage
                    ),
                    type = BroadcastMessage.Type.WARNING
                )
            }
        }
    }

    private val killSwitch = getFeatureFlagFlow(driveSharingDisabled(userId))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeatureFlag(driveSharingDisabled(userId), NOT_FOUND)
        )

    val viewState: Flow<ManageAccessViewState> = combine(
        driveLink.filterNotNull(),
        sharedDriveLink.filterSuccessOrError().mapSuccessValueOrNull(),
        sharedLoadingViewState,
        shareUsers,
        killSwitch,
    ) { driveLink, sharedDriveLink, sharedLoadingViewState, shareUsers, killSwitch ->
        ManageAccessViewState(
            title = appContext.getString(I18N.string.title_manage_access),
            linkId = linkId,
            publicUrl = sharedDriveLink?.publicUrl?.value,
            accessibilityDescription = if ( sharedDriveLink?.customPassword != null) {
                appContext.getString(I18N.string.manage_access_link_description_password_protected)
            } else {
                appContext.getString(I18N.string.manage_access_link_description_public)
            },
            linkName = driveLink.name,
            isLinkNameEncrypted = driveLink.isNameEncrypted,
            canEdit = (driveLink.sharePermissions == null || driveLink.sharePermissions?.isAdmin == true)
                    && killSwitch.state != ENABLED,
            loadingViewState = sharedLoadingViewState,
            shareUsers = shareUsers.toViewState()
        )
    }

    fun viewEvent(
        navigateToShareViaInvitations: (LinkId) -> Unit,
        navigateToLinkSettings: (LinkId) -> Unit,
        navigateToStopLinkSharing: (LinkId) -> Unit,
        navigateToStopAllSharing: (ShareId) -> Unit,
        navigateToInvitationOptions: (LinkId, String) -> Unit,
        navigateToExternalInvitationOptions: (LinkId, String) -> Unit,
        navigateToMemberOptions: (LinkId, String) -> Unit,
        navigateBack: () -> Unit
    ) = object : ManageAccessViewEvent {
        override val onCopyLink: (String) -> Unit = { publicUrl -> copyLink(publicUrl) }
        override val onInvite: () -> Unit = { navigateToShareViaInvitations(linkId) }
        override val onOptions: (ShareUserViewState) -> Unit = { user ->
            when (user.type) {
                ShareUserType.INVITATION -> navigateToInvitationOptions(linkId, user.id)
                ShareUserType.MEMBER -> navigateToMemberOptions(linkId, user.id)
                ShareUserType.EXTERNAL_INVITATION ->
                    navigateToExternalInvitationOptions(linkId, user.id)
            }
        }
        override val onConfigureSharing: () -> Unit = { navigateToLinkSettings(linkId) }
        override val onStartLinkSharing: () -> Unit = { startLinkSharing() }
        override val onStopLinkSharing: () -> Unit = { navigateToStopLinkSharing(linkId) }
        override val onStopAllSharing: () -> Unit = {
            driveLink.value?.sharingDetails?.shareId?.let{
                navigateToStopAllSharing(it)
            }
        }
        override val onBackPressed: () -> Unit = { navigateBack() }
        override val onRetry: () -> Unit = ::retry
    }

    private fun retry() {
        viewModelScope.launch {
            retryTrigger.emit(Unit)
        }
    }

    private fun copyLink(publicUrl: String) {
        copyToClipboard(userId, appContext.getString(I18N.string.common_link), publicUrl)
    }

    private fun startLinkSharing() {
        viewModelScope.launch {
            retryTrigger.emit(Unit)
        }
    }

    private fun DriveLink.toLoadingMessage(): String = if (hasShareLink) {
        appContext.getString(I18N.string.shared_link_getting_link)
    } else {
        val suffix = appContext.getString(
            if (id is FolderId) {
                I18N.string.shared_link_folder
            } else {
                I18N.string.shared_link_file
            }
        )
        appContext.getString(I18N.string.shared_link_loading, suffix)
    }


    private fun List<ShareUser>.toViewState(): List<ShareUserViewState> = map { user ->
        user.toViewState(appContext)
    }

    companion object {
        const val LINK_ID = "linkId"
        const val SHARE_ID = "shareId"
    }
}

