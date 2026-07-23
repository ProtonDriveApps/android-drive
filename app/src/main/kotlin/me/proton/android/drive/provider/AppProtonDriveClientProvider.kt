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

package me.proton.android.drive.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.android.drive.sdk.DriveMetricCallback
import me.proton.android.drive.sdk.DrivePublicAddressResolver
import me.proton.android.drive.sdk.SdkMetricsNotifier
import me.proton.android.drive.usecase.GetOrCreateSdkLoggerProvider
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.DRIVE_SDK
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.usecase.GetOrCreateClientUid
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.key.domain.usecase.GetPublicAddressInfo
import me.proton.core.network.data.ApiProvider
import me.proton.core.user.domain.repository.UserAddressRepository
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.CoreUserAddressResolver
import me.proton.drive.sdk.ProtonDriveClient
import me.proton.drive.sdk.ProtonDriveSdk
import me.proton.drive.sdk.entity.ClientCreateRequest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppProtonDriveClientProvider @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val apiProvider: ApiProvider,
    private val cryptoContext: CryptoContext,
    private val userAddressRepository: UserAddressRepository,
    private val getPublicAddressInfo: GetPublicAddressInfo,
    private val getPermanentFolder: GetPermanentFolder,
    private val getOrCreateClientUid: GetOrCreateClientUid,
    private val sdkMetricsNotifier: SdkMetricsNotifier,
    private val getFeatureFlag: GetFeatureFlag,
    private val getOrCreateSdkLoggerProvider: GetOrCreateSdkLoggerProvider,
) : ProtonDriveClientProvider {

    private val mutex = Mutex()
    private val scopes = mutableMapOf<UserId?, CoroutineScope>()
    private val clients = mutableMapOf<UserId, ProtonDriveClient>()

    override suspend fun getOrCreate(userId: UserId): Result<ProtonDriveClient> = coRunCatching {
        mutex.withLock {
            clients.getOrPut(userId) {
                val scope = scopes.getOrPut(userId) {
                    CoroutineScope(Dispatchers.IO + Job())
                }
                createProtonDriveClient(
                    coroutineScope = scope,
                    userId = userId
                ).getOrThrow()
            }
        }
    }

    fun remove(userId: UserId) {
        clients.remove(userId)?.close()
        scopes.remove(userId)?.cancel("Account $userId is removed")
    }

    private suspend fun createProtonDriveClient(
        coroutineScope: CoroutineScope,
        userId: UserId
    ): Result<ProtonDriveClient> = coRunCatching {
        CoreLogger.d(
            LogTag.DRIVE_SDK,
            "Creating sdk proton drive client for ${userId.id.logId()}"
        )
        getPermanentFolder(userId).let { userDir ->
            // Remove old persistent cache
            File(userDir, "secret.db").deleteCacheFile()
            File(userDir, "entity.db").deleteCacheFile()
        }
        ProtonDriveSdk.protonDriveClientCreate(
            coroutineScope = coroutineScope,
            userId = userId,
            apiProvider = apiProvider,
            request = ClientCreateRequest(
                baseUrl = "${configurationProvider.baseUrl}/",
                loggerProvider = getOrCreateSdkLoggerProvider().getOrThrow(),
                bindingsLanguage = "kotlin",
                uid = getOrCreateClientUid().getOrNull(
                    tag = LogTag.DRIVE_SDK,
                    message = "Failed to get or create client Uid",
                )
            ),
            userAddressResolver = CoreUserAddressResolver(
                userId = userId,
                cryptoContext = cryptoContext,
                userAddressRepository = userAddressRepository,
            ),
            publicAddressResolver = DrivePublicAddressResolver(
                userId = userId,
                getPublicAddressInfo = getPublicAddressInfo,
            ),
            metricCallback = DriveMetricCallback(
                sdkMetricsNotifier = sdkMetricsNotifier,
                coroutineContext = coroutineScope.coroutineContext,
            ),
            featureEnabled = { name ->
                getFeatureFlag(FeatureFlagId.Unleash(userId, name)).on
            }
        )
    }

    private fun File.deleteCacheFile() = coRunCatching {
        if (exists() && delete()) {
            CoreLogger.d(DRIVE_SDK, "Cache file $name existed and is deleted")
        }
    }.getOrNull(DRIVE_SDK, "Cannot delete old persistent cache $name")
}
