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

package me.proton.android.drive.initializer

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.lifecycle.coroutineScope
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.extension.log
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.hasConnectivity
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.TRACKING
import me.proton.core.drive.linkupload.domain.manager.UploadSpeedManager
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinksCount
import me.proton.core.drive.upload.data.exception.UploadWorkerException
import me.proton.core.drive.upload.domain.handler.UploadErrorHandler
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.util.kotlin.CoreLogger

class UploadInitializer : Initializer<Unit> {

    private val scopes = mutableMapOf<UserId, CoroutineScope>()

    override fun create(context: Context) {
        with(
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                UploadInitializerEntryPoint::class.java,
            )
        ) {

            uploadErrorManager.errors
                .onEach { uploadError -> handleUploadError(uploadError) }
                .launchIn(appLifecycleProvider.lifecycle.coroutineScope)

            accountManager.observe(
                appLifecycleProvider.lifecycle,
                androidx.lifecycle.Lifecycle.State.CREATED
            )
                .onAccountReady { account ->
                    val userId = account.userId
                    val scope = scopes.getOrPut(userId) {
                        CoroutineScope(Dispatchers.IO + Job())
                    }
                    uploadErrorManager.errors
                        .map { uploadError -> context.hasNotConnectivity(uploadError) }
                        .onEach { noConnectivity ->
                            if (noConnectivity && uploadSpeedManager.isRunning()) {
                                CoreLogger.v(TRACKING, "Pausing, no network to upload")
                                uploadSpeedManager.pause(userId)
                            }
                        }
                        .launchIn(scope)
                    getUploadFileLinksCount(userId).distinctUntilChanged().map { count ->
                        count.total > 0L
                    }.distinctUntilChanged().onEach { uploading ->
                        if (uploading) {
                            CoreLogger.v(TRACKING, "Resuming, new uploads are scheduled")
                            uploadSpeedManager.resume()
                        } else {
                            CoreLogger.v(TRACKING, "Pausing, all uploads are done")
                            uploadSpeedManager.pause(userId)
                        }
                    }.launchIn(scope)
                }
                .onAccountRemoved { account ->
                    scopes.remove(account.userId)?.cancel()
                }
        }
    }

    private fun Context.hasNotConnectivity(
        uploadError: UploadErrorManager.Error
    ): Boolean {
        val throwable = uploadError.throwable
        return if (throwable is ApiException) {
            val error = throwable.error
            error is ApiResult.Error.Connection && !error.isConnectedToNetwork
        } else if (throwable is UploadWorkerException) {
            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                throwable.hasNoConnectivity()
            } else {
                !hasConnectivity()
            }
        } else {
            false
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        WorkManagerInitializer::class.java,
    )

    private suspend fun UploadInitializerEntryPoint.handleUploadError(uploadError: UploadErrorManager.Error) {
        uploadErrorHandlers.forEach { uploadErrorHandler ->
            runCatching {
                uploadErrorHandler.onError(uploadError)
            }.onFailure { error ->
                error.log(LogTag.UPLOAD, "Failed to handle upload error")
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UploadInitializerEntryPoint {
        val appLifecycleProvider: AppLifecycleProvider
        val uploadErrorManager: UploadErrorManager
        val uploadErrorHandlers: @JvmSuppressWildcards Set<UploadErrorHandler>
        val getUploadFileLinksCount: GetUploadFileLinksCount
        val uploadSpeedManager: UploadSpeedManager
        val accountManager: AccountManager
    }
}
