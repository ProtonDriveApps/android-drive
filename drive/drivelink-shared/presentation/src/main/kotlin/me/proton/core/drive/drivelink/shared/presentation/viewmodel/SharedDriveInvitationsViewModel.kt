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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.coroutines.timeLimitedScope
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.onProcessing
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.usecase.GetUserEmailsFlow
import me.proton.core.drive.base.domain.usecase.IsValidEmailAddress
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.contact.domain.usecase.SearchContacts
import me.proton.core.drive.contact.presentation.component.SuggestionItem
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.shared.presentation.effect.SharedDriveInvitationsEffect
import me.proton.core.drive.drivelink.shared.presentation.viewevent.SharedDriveInvitationsViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.PermissionViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.PermissionsViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.SaveButtonViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.SharedDriveInvitationsViewState
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingDisabled
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingExternalInvitationsDisabled
import me.proton.core.drive.feature.flag.domain.extension.off
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.key.domain.extension.hasKeys
import me.proton.core.drive.key.domain.usecase.GetPublicAddressInfo
import me.proton.core.drive.label.domain.usecase.SearchLabelsWithContacts
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.CreateShareInvitationsResult
import me.proton.core.drive.share.user.domain.entity.ShareUserInvitation
import me.proton.core.drive.share.user.domain.entity.ShareUsersInvitation
import me.proton.core.drive.share.user.domain.entity.containsOnlyWarnings
import me.proton.core.drive.share.user.domain.entity.toError
import me.proton.core.drive.share.user.domain.usecase.InviteMembers
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class SharedDriveInvitationsViewModel @Inject constructor(
    getDriveLink: GetDecryptedDriveLink,
    getUserEmailsFlow: GetUserEmailsFlow,
    private val inviteMembers: InviteMembers,
    private val getPublicAddressInfo: GetPublicAddressInfo,
    private val isValidEmailAddress: IsValidEmailAddress,
    private val searchContacts: SearchContacts,
    private val searchLabelsWithContacts: SearchLabelsWithContacts,
    private val broadcastMessage: BroadcastMessages,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    private val configurationProvider: ConfigurationProvider,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId: ShareId = ShareId(userId, savedStateHandle.require(SHARE_ID))
    private val linkId: LinkId = FileId(shareId, savedStateHandle.require(LINK_ID))
    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private val driveLink = getDriveLink(linkId, failOnDecryptionError = false)
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var searchContactsJob: Job? = null
    private var searchLabelsJob: Job? = null
    private val _effect = MutableSharedFlow<SharedDriveInvitationsEffect>()
    val effect: Flow<SharedDriveInvitationsEffect> = _effect.asSharedFlow()

    private val killSwitch = getFeatureFlagFlow(driveSharingDisabled(userId))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FeatureFlag(driveSharingDisabled(userId), NOT_FOUND),
        )

    private val externalInvitationFeatureFlag =
        getFeatureFlagFlow(driveSharingExternalInvitationsDisabled(userId))
            .map { killSwitch -> killSwitch.off }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val permissions = MutableStateFlow(Permissions.editor)

    private val message = MutableStateFlow("")
    private val sendMessageAndName = MutableStateFlow(true)

    private val suggestedContacts = MutableStateFlow(emptyList<SuggestionItem>())
    private val suggestedLabels = MutableStateFlow(emptyList<SuggestionItem>())
    private val invitationEmails = MutableStateFlow(emptyList<String>())
    private val invitations = combine(
        invitationEmails,
        permissions,
    ) { invitationEmails, permissions ->
        invitationEmails.map { email ->
            ShareUserInvitation(email, permissions)
        }
    }

    private val validatedInvitations = combine(
        getUserEmailsFlow(userId),
        invitations,
        externalInvitationFeatureFlag,
    ) { userEmails, invitations, externalInvitationFeatureFlag ->
        invitations.map { invitation ->
            if (externalInvitationFeatureFlag) {
                invitation.copy(
                    isValid = invitation.email !in userEmails
                )
            } else {
                getPublicAddressInfo(userId, invitation.email)
                    .getOrNull(SHARING, "Cannot get public address")?.let { publicAddressInfo ->
                        invitation.copy(
                            isValid = invitation.email !in userEmails && publicAddressInfo.hasKeys(
                                unverified = true
                            )
                        )
                    } ?: invitation
            }
        }
    }

    private val validInvitations = validatedInvitations.map { invitations ->
        invitations.filter { invitation -> invitation.isValid == true }
    }

    val initialViewState = combine(
        permissions,
        validatedInvitations,
        validInvitations,
        message,
        sendMessageAndName,
    ) { permissions, validatedInvitations, internalInvitations, message, sendMessageAndName ->
        SharedDriveInvitationsViewState(
            linkName = "",
            isLinkNameEncrypted = true,
            showFullForm = internalInvitations.isNotEmpty(),
            invitations = validatedInvitations,
            permissionsViewState = PermissionsViewState(
                options = listOf(
                    PermissionViewState(
                        icon = CorePresentation.drawable.ic_proton_eye,
                        label = appContext.getString(I18N.string.common_permission_viewer),
                        selected = permissions == Permissions.viewer,
                        permissions = Permissions.viewer,
                    ),
                    PermissionViewState(
                        icon = CorePresentation.drawable.ic_proton_pen,
                        label = appContext.getString(I18N.string.common_permission_editor),
                        selected = permissions == Permissions.editor,
                        permissions = Permissions.editor,
                    ),
                )
            ),
            message = message,
            sendMessageAndName = sendMessageAndName,
        )
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val saveInProgress = MutableStateFlow(false)

    val initialSaveButtonViewState = SaveButtonViewState(
        label = appContext.quantityString(
            pluralRes = I18N.plurals.share_via_invitations_sharing_with,
            quantity = 0,
        ),
        isVisible = false,
        isEnabled = true,
        inProgress = false,
    )

    val saveButtonViewState =
        combine(
            validInvitations,
            saveInProgress,
            killSwitch
        ) { invitations, saveInProgress, killSwitch ->
            initialSaveButtonViewState.copy(
                label = appContext.quantityString(
                    pluralRes = I18N.plurals.share_via_invitations_sharing_with,
                    quantity = invitations.size
                ),
                isVisible = invitations.isNotEmpty(),
                isEnabled = !saveInProgress && killSwitch.off,
                inProgress = saveInProgress,
            )
        }
    val viewState: Flow<SharedDriveInvitationsViewState> = combine(
        initialViewState,
        driveLink.filterNotNull(),
        suggestedContacts,
        suggestedLabels,
    ) { initialViewState, driveLink, suggestedContacts, suggestedLabels ->
        initialViewState.copy(
            linkName = driveLink.name,
            isLinkNameEncrypted = driveLink.isNameEncrypted,
            suggestionItems = suggestedContacts + suggestedLabels,
        )
    }

    fun viewEvent(
        navigateToDiscardChanges: (LinkId) -> Unit,
        navigateBack: () -> Unit,
    ) = object : SharedDriveInvitationsViewEvent {
        override val onSearchTermChanged: (String) -> Unit =
            { searchTerm -> this@SharedDriveInvitationsViewModel.onSearchTermChanged(searchTerm) }
        override val onInviteesChanged: (List<String>) -> Unit =
            { emails -> this@SharedDriveInvitationsViewModel.onInviteesChanged(emails) }
        override val onPermissions: () -> Unit = {
            viewModelScope.launch {
                _effect.emit(SharedDriveInvitationsEffect.Permissions)
            }
        }
        override val onPermissionsChanged: (Permissions) -> Unit = { permissions ->
            this@SharedDriveInvitationsViewModel.permissions.value = permissions
        }
        override val onBackPressed: () -> Unit =
            { onBackPressed(navigateToDiscardChanges, navigateBack) }
        override val onRetry: () -> Unit = ::retry
        override val onSave: () -> Unit = { save(navigateBack) }
        override val isValidEmailAddress: (String) -> Boolean =
            { email -> this@SharedDriveInvitationsViewModel.isValidEmailAddress(email) }
        override val onMessageChanged: (String) -> Unit = { message ->
            this@SharedDriveInvitationsViewModel.message.value = message
        }
        override val onSendMessageAndName: (Boolean) -> Unit = { sendName ->
            this@SharedDriveInvitationsViewModel.sendMessageAndName.value = sendName
        }

    }


    private fun onSearchTermChanged(searchTerm: String) {

        // cancel previous search Job for this [suggestionsField] type
        searchContactsJob?.cancel()
        searchLabelsJob?.cancel()

        if (searchTerm.isNotBlank()) {
            searchContactsJob = searchContacts(userId, searchTerm).onEach { result ->
                result.onFailure { error ->
                    broadcastMessage(
                        userId = userId,
                        message = error.getDefaultMessage(
                            appContext,
                            configurationProvider.useExceptionMessage
                        ),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }.onSuccess { contacts ->
                    val alreadyInvited = invitations.first().map { invitation -> invitation.email }
                    val suggestedContacts = contacts
                        .filterNot { contact ->
                            contact.contactEmails.all { contactEmail ->
                                contactEmail.email in alreadyInvited
                            }
                        }
                        .flatMap { contact ->
                            contact.contactEmails.map { contactEmail ->
                                SuggestionItem(
                                    header = contactEmail.name.takeIfNotBlank()
                                        ?: contact.name.takeIfNotBlank()
                                        ?: contactEmail.email,
                                    subheader = contactEmail.email,
                                    value = contactEmail.email
                                )
                            }
                        }

                    this.suggestedContacts.emit(suggestedContacts)
                }
            }.launchIn(viewModelScope)
            searchLabelsJob = searchLabelsWithContacts(userId, searchTerm).onEach { result ->
                result.onFailure { error ->
                    broadcastMessage(
                        userId = userId,
                        message = error.getDefaultMessage(
                            appContext,
                            configurationProvider.useExceptionMessage
                        ),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }.onSuccess { labelWithContacts ->
                    val alreadyInvited = invitations.first().map { invitation -> invitation.email }
                    val suggestionLabels = labelWithContacts.map { label ->
                        SuggestionItem(
                            header = label.label.name,
                            appContext.quantityString(
                                I18N.plurals.share_via_invitations_group_members,
                                label.contactEmails.size
                            ).format(label.contactEmails.size),
                            value = label.contactEmails.filterNot { contactEmail ->
                                contactEmail.email in alreadyInvited
                            }.joinToString(" ") { contactEmail -> contactEmail.email }
                        )
                    }
                    this.suggestedLabels.emit(suggestionLabels)
                }
            }.launchIn(viewModelScope)
        } else {
            this.suggestedContacts.tryEmit(emptyList())
            this.suggestedLabels.tryEmit(emptyList())
        }
    }

    private fun onInviteesChanged(emails: List<String>) = viewModelScope.launch {
        invitationEmails.emit(emails.distinct())
    }

    private fun onBackPressed(
        navigateToDiscardChanges: (LinkId) -> Unit,
        navigateBack: () -> Unit,
    ) {
        viewModelScope.launch {
            if (invitations.firstOrNull().orEmpty().isNotEmpty()) {
                navigateToDiscardChanges(linkId)
            } else {
                navigateBack()
            }
        }
    }

    private fun retry() {
        viewModelScope.launch {
            retryTrigger.emit(Unit)
        }
    }

    private fun save(onComplete: () -> Unit) {
        timeLimitedScope(SHARING) {
            val invitations = validInvitations.first()
            CoreLogger.i(SHARING, "Start sending invitations (${invitations.size})")
            val sendMessageAndName = sendMessageAndName.value
            val message = message.value.takeIf { sendMessageAndName }
            val itemName = driveLink.value?.name?.takeIf { sendMessageAndName }
            val dataResult = inviteMembers(
                ShareUsersInvitation(
                    linkId = linkId,
                    members = invitations,
                    message = message,
                    itemName = itemName,
                )
            ).onEach { dataResult ->
                dataResult.onProcessing { saveInProgress.emit(true) }
            }.last()
            saveInProgress.emit(false)
            dataResult.onFailure { error ->
                error.log(SHARING, "Cannot send all invitations")
                broadcastMessage(
                    userId = userId,
                    message = error.getDefaultMessage(
                        appContext,
                        configurationProvider.useExceptionMessage
                    ),
                    type = BroadcastMessage.Type.ERROR
                )
            }.onSuccess { result ->
                result.toError("Cannot send some invitations")?.log(
                    tag = SHARING,
                    level = if (result.containsOnlyWarnings()) {
                        LoggerLevel.WARNING
                    } else {
                        LoggerLevel.ERROR
                    },
                )
                broadcastResult(result, onComplete)
            }
        }
    }

    private fun broadcastResult(
        result: CreateShareInvitationsResult,
        onComplete: () -> Unit
    ) {
        val (message, type) = when {
            result.failures.isEmpty() -> {
                appContext.quantityString(
                    I18N.plurals.share_via_invitations_person_added,
                    result.successes.size,
                ) to BroadcastMessage.Type.SUCCESS
            }

            result.successes.isEmpty() -> listOf(
                appContext.quantityString(
                    I18N.plurals.share_via_invitations_person_added_error,
                    result.failures.size,
                ),
                result.failures.values.firstOrNull()?.getDefaultMessage(
                    appContext,
                    configurationProvider.useExceptionMessage
                )
            ).joinToString("\n") to BroadcastMessage.Type.ERROR

            else -> {
                listOfNotNull(
                    appContext.quantityString(
                        I18N.plurals.share_via_invitations_person_added,
                        result.successes.size,
                    ),
                    appContext.quantityString(
                        I18N.plurals.share_via_invitations_person_added_error,
                        result.failures.size,
                    ),
                    result.failures.values.firstOrNull()?.getDefaultMessage(
                        appContext,
                        configurationProvider.useExceptionMessage
                    )
                ).joinToString("\n") to BroadcastMessage.Type.ERROR

            }
        }
        broadcastMessage(userId, message, type)
        if (type == BroadcastMessage.Type.SUCCESS) {
            onComplete()
        }
    }

    companion object {
        const val LINK_ID = "linkId"
        const val SHARE_ID = "shareId"
    }
}
