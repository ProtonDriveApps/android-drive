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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.usecase.EnablePhotosBackup
import me.proton.android.drive.photos.domain.usecase.GetPhotosDriveLink
import me.proton.android.drive.photos.presentation.viewevent.BackupPermissionsViewEvent
import me.proton.android.drive.photos.presentation.viewstate.BackupPermissionsEffect
import me.proton.android.drive.photos.presentation.viewstate.BackupPermissionsViewState
import me.proton.android.drive.usecase.MarkOnboardingAsShown
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.firstSuccessOrError
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.presentation.viewevent.OnboardingViewEvent
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.base.presentation.viewstate.OnboardingViewState
import me.proton.core.drive.user.domain.extension.isFree
import me.proton.core.user.domain.usecase.GetUser
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    savedStateHandle: SavedStateHandle,
    getUser: GetUser,
    private val getPhotosDriveLink: GetPhotosDriveLink,
    private val enablePhotosBackup: EnablePhotosBackup,
    private val backupPermissionsManager: BackupPermissionsManager,
    private val markOnboardingAsShown: MarkOnboardingAsShown,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val _backupPermissionsEffect = MutableSharedFlow<BackupPermissionsEffect>()
    private val backupPermissionsEffect: Flow<BackupPermissionsEffect> = _backupPermissionsEffect.asSharedFlow()
    private var dismiss: (() -> Unit)? = null
    private var nextStep: (() -> Unit)? = null
    private val isPhotoBackupVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val backupPermissionsViewState = BackupPermissionsViewState(
        permissions = backupPermissionsManager.requiredBackupPermissions,
        effect = backupPermissionsEffect,
        shouldRequestNotificationPermission = false,
    )

    val backupPermissionsViewEvent = object : BackupPermissionsViewEvent {
        override val onPermissionsChanged = backupPermissionsManager::onPermissionChanged

        override val onPermissionResult = { result: Map<String, Boolean> ->
            val allPermissionsGranted = result.values.all { it }
            if (allPermissionsGranted) {
                nextStep?.invoke()
            } else {
                dismiss?.invoke()
            }
            Unit
        }
    }

    val viewState: Flow<OnboardingViewState> = isPhotoBackupVisible.map { isPhotoBackupVisible ->
        val user = getUser(userId, false)
        OnboardingViewState(
            title = appContext.getString(I18N.string.onboarding_title),
            availableStorage = Bytes(user.maxDriveSpace ?: user.maxSpace),
            isFreeUser = user.isFree,
            skipButtonTitle = appContext.getString(I18N.string.onboarding_action_skip),
            primaryButtonTitle = appContext.getString(I18N.string.onboarding_action_primary),
            secondaryButtonTitle = appContext.getString(I18N.string.onboarding_action_secondary),
            doneButtonTitle = appContext.getString(I18N.string.onboarding_action_done),
            isPhotoBackupVisible = isPhotoBackupVisible,
        )
    }

    fun viewEvent(dismiss: () -> Unit): OnboardingViewEvent = object : OnboardingViewEvent {
        override val onSkip = { dismiss() }
        override val onPrimaryAction = { turnOnCameraUpload() }
        override val onSecondaryAction = { moreUploadOptions() }
        override val onBack = { isPhotoBackupVisible.value = false }
        override val onDone = { dismiss() }
        override val onboardingShown = { onboardingShown() }
    }.also {
        this.dismiss = dismiss
    }

    private fun turnOnCameraUpload() {
        viewModelScope.launch {
            isPhotoBackupVisible.value = false
            when (backupPermissionsManager.getBackupPermissions()) {
                is BackupPermissions.Granted -> enableBackup()
                is BackupPermissions.Denied -> {
                    _backupPermissionsEffect.emit(BackupPermissionsEffect.RequestPermission)
                    nextStep = ::enableBackup
                }
            }
        }
    }

    private fun moreUploadOptions() {
        viewModelScope.launch {
            when (backupPermissionsManager.getBackupPermissions()) {
                is BackupPermissions.Granted -> showPhotoBackup()
                is BackupPermissions.Denied -> {
                    _backupPermissionsEffect.emit(BackupPermissionsEffect.RequestPermission)
                    nextStep = ::showPhotoBackup
                }
            }
        }
    }

    private fun enableBackup() {
        viewModelScope.launch {
            getPhotosDriveLink(userId)
                .firstSuccessOrError()
                .toResult()
                .getOrNull(LogTag.BACKUP, "Getting photos drive link during onboarding failed")
                ?.id
                ?.let { photoRootId ->
                    enablePhotosBackup(photoRootId)
                        .onFailure { error ->
                            error.log(LogTag.BACKUP, "Enabling backup during onboarding failed")
                        }
                }
            dismiss?.invoke()
        }
    }

    private fun showPhotoBackup() {
        isPhotoBackupVisible.value = true
    }

    private fun onboardingShown() {
        viewModelScope.launch {
            markOnboardingAsShown()
                .getOrNull(VIEW_MODEL, "Marking onboarding as shown failed")
        }
    }
}
