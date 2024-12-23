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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.toTimestampMs
import me.proton.core.drive.base.domain.exception.requireField
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.log.LogTag.SHARE
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.shared.domain.usecase.GetSharedDriveLink
import me.proton.core.drive.drivelink.shared.presentation.component.asDayOfMonth
import me.proton.core.drive.drivelink.shared.presentation.component.asMonth
import me.proton.core.drive.drivelink.shared.presentation.component.asYear
import me.proton.core.drive.drivelink.shared.presentation.viewevent.LinkSettingsViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.LinkSettingsViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.PrivacySettingsViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.SaveButtonViewState
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.shareurl.crypto.domain.usecase.UpdateShareUrl
import me.proton.core.drive.volume.domain.entity.VolumeId
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import me.proton.core.drive.base.domain.extension.combine as baseCombine
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("StaticFieldLeak")
class LinkSettingsViewModel @Inject constructor(
    getDriveLink: GetDecryptedDriveLink,
    private val getSharedDriveLink: GetSharedDriveLink,
    private val updateShareUrl: UpdateShareUrl,
    private val broadcastMessage: BroadcastMessages,
    private val savedStateHandle: SavedStateHandle,
    private val dateTimeFormatter: DateTimeFormatter,
    private val configurationProvider: ConfigurationProvider,
    @ApplicationContext private val appContext: Context,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId: ShareId = ShareId(userId, savedStateHandle.require(SHARE_ID))
    private val linkId: LinkId = FileId(shareId, savedStateHandle.require(LINK_ID))
    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private val driveLink = getDriveLink(linkId, failOnDecryptionError = false)
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val sharedDriveLink = driveLink.filterNotNull().transformLatest { driveLink ->
        emitAll(getSharedDriveLink(driveLink))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val initialViewState = sharedDriveLink
        .filterNotNull().mapSuccessValueOrNull().filterNotNull().map { sharedDriveLink ->
            LinkSettingsViewState(
                hasUnsavedChanges = false,
                privacySettingsViewState = PrivacySettingsViewState(
                    enabled = true,
                    password = sharedDriveLink.customPassword?.value,
                    passwordChecked = sharedDriveLink.customPassword != null,
                    expirationDate = sharedDriveLink.expirationTime,
                    expirationDateChecked = sharedDriveLink.expirationTime != null,
                    minDatePickerDate = startOfFirstDay.timeInMillis,
                    maxDatePickerDate = endOfLastDay.timeInMillis,
                )
            )
        }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val passwordField = MutableStateFlow<String?>(null)
    private val passwordEnabled = MutableStateFlow(false)
    private val expirationDate = MutableStateFlow<Date?>(null)
    private val expirationDateEnabled = MutableStateFlow(false)
    private val saveInProgress = MutableStateFlow(false)
    private val _initPrivacySettings = viewModelScope.launch {
        initialViewState.take(1).collect { viewState ->
            passwordField.emit(viewState.privacySettingsViewState.password)
            passwordEnabled.emit(viewState.privacySettingsViewState.passwordChecked)
            expirationDate.emit(viewState.privacySettingsViewState.expirationDate)
            expirationDateEnabled.emit(viewState.privacySettingsViewState.expirationDateChecked)
        }
    }
    val initialSaveButtonViewState = SaveButtonViewState(
        label = "",
        isVisible = false,
        isEnabled = true,
        inProgress = false,
    )
    private val hasUnsavedPasswordChanges = combine(
        initialViewState,
        passwordField,
        passwordEnabled,
    ) { initialViewState, password, passwordEnabled ->
        when {
            initialViewState.privacySettingsViewState.passwordChecked && !passwordEnabled -> true
            !initialViewState.privacySettingsViewState.passwordChecked && passwordEnabled && !password.isNullOrEmpty() -> true
            passwordEnabled && password.orEmpty() != initialViewState.privacySettingsViewState.password.orEmpty() -> true
            else -> false
        }
    }
    private val hasUnsavedExpirationDateChanges = combine(
        initialViewState,
        expirationDate,
        expirationDateEnabled,
    ) { initialViewState, expirationDate, expirationDateEnabled ->
        when {
            initialViewState.privacySettingsViewState.expirationDateChecked && !expirationDateEnabled -> true
            expirationDateEnabled && expirationDate != initialViewState.privacySettingsViewState.expirationDate -> true
            else -> false
        }
    }
    val hasUnsavedChanges: Flow<Boolean> = combine(
        hasUnsavedPasswordChanges,
        hasUnsavedExpirationDateChanges,
    ) { hasUnsavedPasswordChanges, hasUnsavedExpirationDateChanges ->
        hasUnsavedPasswordChanges || hasUnsavedExpirationDateChanges
    }
    val saveButtonViewState = combine(
        sharedDriveLink.filterNotNull(),
        saveInProgress,
        hasUnsavedChanges,
    ) { sharedDriveLink, saveInProgress, hasUnsavedChanges ->
        initialSaveButtonViewState.copy(
            isVisible = sharedDriveLink is DataResult.Success && (hasUnsavedChanges || saveInProgress),
            isEnabled = hasUnsavedChanges && !saveInProgress,
            inProgress = saveInProgress,
        )
    }
    val viewState: Flow<LinkSettingsViewState> = baseCombine(
        initialViewState,
        passwordField,
        passwordEnabled,
        expirationDate,
        expirationDateEnabled,
        hasUnsavedChanges,
        saveInProgress,
    ) { initialViewState, password, passwordEnabled, expirationDate, expirationDateEnabled, hasUnsavedChanges, saveInProgress->
        initialViewState.copy(
            hasUnsavedChanges = hasUnsavedChanges,
            privacySettingsViewState = initialViewState.privacySettingsViewState.copy(
                enabled = !saveInProgress,
                password = password,
                passwordChecked = passwordEnabled,
                expirationDate = expirationDate,
                expirationDateChecked = expirationDateEnabled,
            )
        )
    }
    fun viewEvent(
        navigateToDiscardChanges: (LinkId) -> Unit,
        navigateBack: () -> Unit
    ) = object : LinkSettingsViewEvent {
        override val onBackPressed: () -> Unit = { onBackPressed(navigateToDiscardChanges, navigateBack) }
        override val onPasswordChanged: (String) -> Unit = { password -> onPasswordFieldChanged(password) }
        override val onPasswordEnabledChanged: (Boolean) -> Unit = { enabled -> onPasswordEnabled(enabled) }
        override val onExpirationDateChanged: (Int, Int, Int) -> Unit = { year, month, dayOfMonth ->
            onExpirationDate(year, month, dayOfMonth)
        }
        override val onExpirationDateEnabledChanged: (Boolean) -> Unit = { enabled -> onExpirationDateEnabled(enabled) }
        override val onRetry: () -> Unit = ::retry
        override val onSave: () -> Unit = ::save
    }

    private fun onPasswordFieldChanged(password: String) = viewModelScope.launch {
        passwordField.emit(password)
    }

    private fun onPasswordEnabled(enabled: Boolean) = viewModelScope.launch {
        passwordEnabled.emit(enabled)
    }

    private fun onExpirationDate(year: Int, month: Int, dayOfMonth: Int) = viewModelScope.launch {
        expirationDate.emit(
            Calendar.getInstance()
                .apply { set(year, month, dayOfMonth, 0, 0, 0) }
                .time
        )
    }

    private fun onExpirationDateEnabled(enabled: Boolean) = viewModelScope.launch {
        expirationDateEnabled.emit(enabled)
    }

    private fun onBackPressed(navigateToDiscardChanges: (LinkId) -> Unit, navigateBack: () -> Unit) = viewModelScope.launch {
        val hasUnsavedChanges = sharedDriveLink.value is DataResult.Success && hasUnsavedChanges.first()
        if (hasUnsavedChanges) {
            navigateToDiscardChanges(linkId)
        } else {
            navigateBack()
        }
    }

    private fun retry() {
        viewModelScope.launch {
            retryTrigger.emit(Unit)
        }
    }

    private fun save() {
        viewModelScope.launch {
            val hasUnsavedPasswordChanges = hasUnsavedPasswordChanges.first()
            val hasUnsavedExpirationDateChanges = hasUnsavedExpirationDateChanges.first()
            if (!hasUnsavedPasswordChanges && !hasUnsavedExpirationDateChanges) return@launch
            val privacySettings = initialViewState.first().privacySettingsViewState
            val password = if (hasUnsavedPasswordChanges) {
                password(privacySettings)
                    .onFailure { error ->
                        error.log(SHARE, "Cannot get password")
                        broadcastMessage(
                            userId = userId,
                            message = error.getDefaultMessage(appContext, configurationProvider.useExceptionMessage),
                            type = BroadcastMessage.Type.ERROR,
                        )
                        return@launch
                    }
                    .getOrNull()
            } else { null }
            val expirationDate = if (hasUnsavedExpirationDateChanges) {
                expirationDateIso8601(privacySettings)
                    .onFailure { error ->
                        error.log(SHARE, "Cannot get expiration date")
                        broadcastMessage(
                            userId = userId,
                            message = error.getDefaultMessage(appContext, configurationProvider.useExceptionMessage),
                            type = BroadcastMessage.Type.ERROR,
                        )
                        return@launch
                    }
                    .getOrNull()
            } else { "" }

            val driveLink = sharedDriveLink.filterNotNull().mapSuccessValueOrNull().filterNotNull().first()
            updateShareUrl(
                volumeId = driveLink.volumeId,
                shareUrlId = driveLink.shareUrlId,
                password = password,
                expirationDateIso8601 = expirationDate,
                hasUnsavedExpirationDateChanges = hasUnsavedExpirationDateChanges,
            )
        }
    }

    private fun password(privacySettings: PrivacySettingsViewState): Result<String?> = coRunCatching {
        when {
            privacySettings.passwordChecked && !passwordEnabled.value -> "" // remove password
            passwordEnabled.value && passwordField.value.orEmpty() != privacySettings.password.orEmpty() ->
                validatePassword(passwordField.value.orEmpty()).getOrThrow() // add or update password
            else -> null
        }
    }

    private fun expirationDateIso8601(privacySettings: PrivacySettingsViewState): Result<String?> = coRunCatching {
        when {
            privacySettings.expirationDateChecked && !expirationDateEnabled.value -> null
            else -> dateTimeFormatter.formatToIso8601String(
                validateExpirationDate(requireNotNull(expirationDate.value)).getOrThrow()
            )
        }
    }

    private fun validatePassword(password: String): Result<String> = coRunCatching {
        requireField(password.length in 1..configurationProvider.maxSharedLinkPasswordLength) {
            appContext.getString(
                I18N.string.shared_link_error_message_invalid_password_length,
                configurationProvider.maxSharedLinkPasswordLength
            )
        }
        password
    }

    private fun validateExpirationDate(expirationDate: Date): Result<Date> = coRunCatching {
        requireField(expirationDate.isNotBeforeStartOfFirstDay) {
            appContext.getString(I18N.string.shared_link_error_message_expiration_date_too_soon)
        }
        requireField(expirationDate.isNotAfterEndOfLastDay) {
            appContext.getString(
                I18N.string.shared_link_error_message_expiration_date_too_late,
                configurationProvider.maxSharedLinkExpirationDuration.inWholeDays,
            )
        }
        expirationDate
    }

    private suspend fun updateShareUrl(
        volumeId: VolumeId,
        shareUrlId: ShareUrlId,
        password: String?,
        expirationDateIso8601: String?,
        hasUnsavedExpirationDateChanges: Boolean,
    ) {
        saveInProgress.emit(true)
        updateShareUrl(
            volumeId = volumeId,
            shareUrlId = shareUrlId,
            customPassword = password,
            expirationDateIso8601 = expirationDateIso8601,
            updateExpirationDate = hasUnsavedExpirationDateChanges,
        )
            .onFailure { error ->
                error.log(SHARE, "Cannot update sharing settings")
                broadcastMessage(
                    userId = userId,
                    message = appContext.getString(
                        I18N.string.shared_link_error_message_update_share_url
                    ),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
            .onSuccess { shareUrl ->
                shareUrl.expirationTime?.let { time ->
                    expirationDate.emit(Date(time.toTimestampMs().value))
                }
                broadcastMessage(
                    userId = userId,
                    message = appContext.getString(
                        I18N.string.shared_link_message_update_share_url
                    ),
                    type = BroadcastMessage.Type.INFO,
                )
            }
            .getOrNull()
        saveInProgress.emit(false)
    }

    private val startOfFirstDay: Calendar get() =
        Calendar.getInstance()
            .apply {
                time = Date()
                add(Calendar.DATE, 1)
                set(time.asYear, time.asMonth, time.asDayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
    private val endOfLastDay: Calendar get() =
        Calendar.getInstance()
            .apply {
                time = Date(
                    System.currentTimeMillis() +
                        configurationProvider.maxSharedLinkExpirationDuration.inWholeMilliseconds
                )
                set(time.asYear, time.asMonth, time.asDayOfMonth, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }
    private val Date.isNotBeforeStartOfFirstDay: Boolean get() {
        val selected = Calendar.getInstance().apply { time = this@isNotBeforeStartOfFirstDay }
        return !selected.before(startOfFirstDay)
    }
    private val Date.isNotAfterEndOfLastDay: Boolean get() {
        val selected = Calendar.getInstance().apply { time = this@isNotAfterEndOfLastDay }
        return !selected.after(endOfLastDay)
    }

    companion object {
        const val LINK_ID = "linkId"
        const val SHARE_ID = "shareId"
    }
}
