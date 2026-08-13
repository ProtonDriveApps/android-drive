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
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.datastore.asFlow
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.api.ProtonApiCode.NOT_EXISTS
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.combine
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.firstCodePointAsStringOrNull
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.extension.orOwner
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
import me.proton.core.drive.drivelink.domain.usecase.GetVolumeType
import me.proton.core.drive.drivelink.shared.domain.extension.sharingDetails
import me.proton.core.drive.drivelink.shared.domain.usecase.CanManageSharing
import me.proton.core.drive.drivelink.shared.domain.usecase.GetOrCreateSharedDriveLink
import me.proton.core.drive.drivelink.shared.domain.usecase.GetSharedDriveLink
import me.proton.core.drive.drivelink.shared.presentation.effect.ManageAccessEffect
import me.proton.core.drive.drivelink.shared.presentation.extension.toViewState
import me.proton.core.drive.drivelink.shared.presentation.viewevent.ManageAccessViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.LoadingViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ManageAccessViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserType
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidAlbumsPublicShare
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.drivePublicShareEditMode
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.drivePublicShareEditModeDisabled
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingDisabled
import me.proton.core.drive.feature.flag.domain.extension.off
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.domain.usecase.ToggleEditorsCanShare
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.usecase.GetShareUsers
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.network.domain.hasProtonErrorCode
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.entity.UserAddress
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("StaticFieldLeak")
class ManageAccessViewModel @Inject constructor(
    getDriveLink: GetDecryptedDriveLink,
    getShareUsers: GetShareUsers,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    userManager: UserManager,
    private val getSharedDriveLink: GetSharedDriveLink,
    private val getOrCreateSharedDriveLink: GetOrCreateSharedDriveLink,
    private val getShare: GetShare,
    private val canManageSharing: CanManageSharing,
    private val toggleEditorsCanShare: ToggleEditorsCanShare,
    private val getUserDataStore: GetUserDataStore,
    private val savedStateHandle: SavedStateHandle,
    private val copyToClipboard: CopyToClipboard,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
    private val getVolumeType: GetVolumeType,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId: ShareId = ShareId(userId, savedStateHandle.require(SHARE_ID))
    private val linkId: LinkId = FileId(shareId, savedStateHandle.require(LINK_ID))
    private val user: StateFlow<User?> = userManager.observeUser(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val userAddresses: StateFlow<List<UserAddress>> = userManager.observeAddresses(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
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
        dataResult.onFailure { resultError ->
            resultError.cause?.let { error ->
                if (error !is NoSuchElementException && !error.hasProtonErrorCode(NOT_EXISTS)) {
                    error.log(SHARING)
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

    private val drivePublicShareEditModeFeatureFlag = getFeatureFlagFlow(drivePublicShareEditMode(userId))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeatureFlag(driveSharingDisabled(userId), NOT_FOUND)
        )

    private val drivePublicShareEditModeKillSwitch = getFeatureFlagFlow(drivePublicShareEditModeDisabled(userId))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeatureFlag(driveSharingDisabled(userId), NOT_FOUND)
        )

    private val killSwitch = getFeatureFlagFlow(driveSharingDisabled(userId))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeatureFlag(driveSharingDisabled(userId), NOT_FOUND)
        )

    private val driveAndroidAlbumsPublicShareFeatureFlag =
        getFeatureFlagFlow(driveAndroidAlbumsPublicShare(userId))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = FeatureFlag(driveAndroidAlbumsPublicShare(userId), NOT_FOUND)
            )

    private val canManageSharingFlow: StateFlow<Boolean?> = canManageSharing(linkId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val closeEffect: Flow<ManageAccessEffect> = canManageSharingFlow
        .filterNotNull()
        .distinctUntilChanged()
        .dropWhile { canManageSharing -> !canManageSharing }
        .filterNot { canManageSharing -> canManageSharing }
        .map { ManageAccessEffect.Close }

    private val _effect = MutableSharedFlow<ManageAccessEffect>()
    val effect: Flow<ManageAccessEffect> = merge(_effect.asSharedFlow(), closeEffect)

    private val share: StateFlow<Share?> = driveLink.filterNotNull().flatMapLatest { driveLink ->
        driveLink.sharingDetails?.shareId?.let { shareId ->
            getShare(shareId).filterSuccessOrError().mapSuccessValueOrNull()
        } ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val showNewEditorPermissionsFlow: Flow<Boolean> = flow {
        val userDataStore = getUserDataStore(userId)
        emitAll(
            combine(
                GetUserDataStore.Keys.newEditorPermissionsDismissed.asFlow(userDataStore, false),
                GetUserDataStore.Keys.newEditorPermissionsFirstShown.asFlow(userDataStore, NEVER_SHOWN),
            ) { dismissed, firstShown ->
                val now = TimestampMs()
                !dismissed && now < NEW_EDITOR_PERMISSIONS_LAST_DAY && (
                    firstShown == NEVER_SHOWN ||
                        now.value < firstShown + NEW_EDITOR_PERMISSIONS_DURATION.inWholeMilliseconds
                    )
            }
        )
    }

    private val volumeType: StateFlow<Volume.Type?> = flow {
        emit(getVolumeType(linkId).getOrNull())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val viewState: Flow<ManageAccessViewState> = combine(
        driveLink.filterNotNull(),
        sharedDriveLink.filterSuccessOrError().mapSuccessValueOrNull(),
        sharedLoadingViewState,
        shareUsers,
        killSwitch,
        drivePublicShareEditModeFeatureFlag,
        drivePublicShareEditModeKillSwitch,
        driveAndroidAlbumsPublicShareFeatureFlag,
        canManageSharingFlow.filterNotNull(),
        share,
        showNewEditorPermissionsFlow,
        user.filterNotNull(),
        userAddresses,
        volumeType,
    ) { driveLink, sharedDriveLink, sharedLoadingViewState, shareUsers,
        killSwitch, drivePublicShareEditModeFeatureFlag,
        drivePublicShareEditModeKillSwitch, driveAndroidAlbumsPublicShareFeatureFlag,
        canManageSharing, share, showNewEditorPermissions, user, userAddresses, volumeType ->
        val publicUrl = sharedDriveLink?.publicUrl?.value
        val isOwner = driveLink.sharePermissions.orOwner.isOwner
        val showEditorsCanShare = isOwner && share != null && volumeType != Volume.Type.PHOTO
        ManageAccessViewState(
            title = appContext.getString(I18N.string.title_manage_access),
            linkId = linkId,
            publicUrl = publicUrl,
            accessibilityDescription = if ( sharedDriveLink?.customPassword != null) {
                appContext.getString(I18N.string.manage_access_link_description_password_protected)
            } else {
                appContext.getString(I18N.string.manage_access_link_description_public)
            },
            permissionsDescription = if(sharedDriveLink?.permissions?.canWrite == true) {
                appContext.getString(I18N.string.manage_access_link_editor_permission)
            }else{
                appContext.getString(I18N.string.manage_access_link_viewer_permission)
            },
            linkName = driveLink.name,
            isLinkNameEncrypted = driveLink.isNameEncrypted,
            canEditMembers = canManageSharing && killSwitch.state != ENABLED,
            canEditLink = drivePublicShareEditModeKillSwitch.off && drivePublicShareEditModeFeatureFlag.on,
            showStopSharing = (publicUrl != null || shareUsers.isNotEmpty()) && isOwner,
            loadingViewState = sharedLoadingViewState,
            shareUsers = buildShareUserViewStates(driveLink, shareUsers, user, share, userAddresses),
            showShareWithAnyone = isOwner && if (driveLink is DriveLink.Album) {
                driveAndroidAlbumsPublicShareFeatureFlag.on
            } else {
                true
            },
            showEditorsCanShare = showEditorsCanShare,
            showNewEditorPermissions = showEditorsCanShare && showNewEditorPermissions,
            editorsCanShare = share?.editorsCanShare == true,
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
        navigateToShareLinkPermissions: (LinkId) -> Unit,
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
        override val onEditLinkPermissions: () -> Unit = { navigateToShareLinkPermissions(linkId) }
        override val onToggleEditorsCanShare: () -> Unit = ::toggleEditorsCanShare
        override val onDismissNewEditorPermissions: () -> Unit = ::dismissNewEditorPermissions
        override val onNewEditorPermissionsShown: () -> Unit = ::newEditorPermissionsShown
    }

    private fun dismissNewEditorPermissions() {
        viewModelScope.launch {
            getUserDataStore(userId).edit { preferences ->
                preferences[GetUserDataStore.Keys.newEditorPermissionsDismissed] = true
            }
        }
    }

    private fun newEditorPermissionsShown() {
        viewModelScope.launch {
            getUserDataStore(userId).edit { preferences ->
                if (preferences[GetUserDataStore.Keys.newEditorPermissionsFirstShown] == null) {
                    preferences[GetUserDataStore.Keys.newEditorPermissionsFirstShown] =
                        TimestampMs().value
                }
            }
        }
    }

    private fun toggleEditorsCanShare() {
        val share = share.value ?: return Unit.also {
            broadcastError(IllegalStateException("Cannot toggle editors can share, share is null"))
        }
        viewModelScope.launch {
            toggleEditorsCanShare(share).onFailure { error -> broadcastError(error) }
        }
    }

    private fun broadcastError(error: Throwable) {
        error.log(SHARING)
        broadcastMessages(
            userId = userId,
            message = error.getDefaultMessage(
                appContext,
                configurationProvider.useExceptionMessage,
            ),
            type = BroadcastMessage.Type.ERROR,
        )
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

    private fun buildShareUserViewStates(
        driveLink: DriveLink,
        shareUsers: List<ShareUser>,
        user: User,
        share: Share?,
        userAddresses: List<UserAddress>,
    ): List<ShareUserViewState> {
        val currentUserEmails = userAddresses.map { address -> address.email }.toSet()
        val currentUserName = user.displayName?.takeUnless(String::isBlank)
            ?: user.name?.takeUnless(String::isBlank)
            ?: user.email.orEmpty()
        val ownedBy = driveLink.link.ownedBy.email?.takeUnless(String::isBlank)
        val creator = share?.creatorEmail?.takeUnless(String::isBlank)
        val ownerEmail = ownedBy ?: creator ?: user.email ?: currentUserEmails.firstOrNull().orEmpty()
        return buildList {
            if (ownerEmail.isNotBlank()) {
                add(ownerViewState(ownerEmail, ownerEmail in currentUserEmails, currentUserName))
            }
            shareUsers
                .filterNot { shareUser -> shareUser.email == ownerEmail }
                .forEach { shareUser ->
                    add(shareUser.toViewState(appContext).markIfCurrentUser(shareUser.email in currentUserEmails))
                }
        }
    }

    private fun ownerViewState(
        email: String,
        isCurrentUser: Boolean,
        currentUserName: String,
    ): ShareUserViewState {
        val letterSource = if (isCurrentUser) currentUserName else email
        return ShareUserViewState(
            id = OWNER_ID,
            email = email,
            permissionLabel = appContext.getString(I18N.string.common_permission_owner),
            firstLetter = letterSource.firstCodePointAsStringOrNull?.uppercase() ?: "?",
            displayName = if (isCurrentUser) {
                appContext.getString(I18N.string.manage_access_current_user, currentUserName)
            } else {
                null
            },
            type = ShareUserType.MEMBER,
            showOptions = false,
        )
    }

    private fun ShareUserViewState.markIfCurrentUser(isCurrentUser: Boolean): ShareUserViewState =
        when {
            !isCurrentUser -> this
            displayName != null -> copy(
                displayName = appContext.getString(I18N.string.manage_access_current_user, displayName)
            )
            else -> copy(
                email = appContext.getString(I18N.string.manage_access_current_user, email)
            )
        }

    companion object {
        const val LINK_ID = "linkId"
        const val SHARE_ID = "shareId"
        private const val OWNER_ID = "owner"
        private const val NEVER_SHOWN = 0L
        private val NEW_EDITOR_PERMISSIONS_DURATION = 30.days
        private val NEW_EDITOR_PERMISSIONS_LAST_DAY = TimestampMs(
            LocalDateTime.of(2026, 12, 31, 23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
    }
}
