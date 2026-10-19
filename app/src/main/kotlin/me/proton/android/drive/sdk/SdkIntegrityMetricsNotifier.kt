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

package me.proton.android.drive.sdk

import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag.DRIVE_SDK
import me.proton.core.drive.base.domain.usecase.ReportError
import me.proton.core.drive.observability.data.extension.toCausedBy
import me.proton.core.drive.observability.data.extension.toField
import me.proton.core.drive.observability.data.extension.toRecency
import me.proton.core.drive.observability.data.extension.toVolumeType
import me.proton.core.drive.observability.domain.constraint.CountConstraint
import me.proton.core.drive.observability.domain.constraint.MinimumIntervalConstraint
import me.proton.core.drive.observability.domain.metrics.common.Plan
import me.proton.core.drive.observability.domain.metrics.common.VolumeType
import me.proton.core.drive.observability.domain.metrics.common.YesNo
import me.proton.core.drive.observability.domain.metrics.sdk.DownloadErroringUsersTotal
import me.proton.core.drive.observability.domain.metrics.sdk.IntegrityBlockVerificationErrorsTotal
import me.proton.core.drive.observability.domain.metrics.sdk.IntegrityDecryptionErrorsTotal
import me.proton.core.drive.observability.domain.metrics.sdk.IntegrityErroringUsersTotal
import me.proton.core.drive.observability.domain.metrics.sdk.IntegrityVerificationErrorsTotal
import me.proton.core.drive.observability.domain.metrics.sdk.common.CausedBy
import me.proton.core.drive.observability.domain.metrics.sdk.common.Field
import me.proton.core.drive.observability.domain.metrics.sdk.common.Recency
import me.proton.core.drive.observability.domain.metrics.sdk.common.YesNoUnknown
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.user.domain.extension.isWithoutProtonSubscription
import me.proton.drive.sdk.telemetry.BlockVerificationErrorEvent
import me.proton.drive.sdk.telemetry.DecryptionErrorEvent
import me.proton.drive.sdk.telemetry.EncryptedField
import me.proton.drive.sdk.telemetry.VerificationErrorEvent
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class SdkIntegrityMetricsNotifier @Inject constructor(
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
    private val minimumIntervalConstraint: MinimumIntervalConstraint,
    private val countConstraint: CountConstraint,
    private val getPrimaryUser: GetPrimaryUser,
    private val reportError: ReportError,
) {

    suspend operator fun invoke(
        decryptionErrorEvent: DecryptionErrorEvent
    ) {
        val recency = decryptionErrorEvent.recency.toRecency()
        val causedBy = decryptionErrorEvent.createdBy.toCausedBy()
        decryptionErrorEvent.field.toField()?.let { field ->
            notifyIntegrityDecryptionErrorsTotalMetric(
                uid = decryptionErrorEvent.uid,
                field = field,
                recency = recency,
                causedBy = causedBy,
            )
        }
        if (recency != Recency.before_2024) {
            notifyIntegrityErroringUsersTotalMetric(
                volumeType = VolumeType.unknown,
                causedBy = causedBy,
            )
            reportError(
                tag = DRIVE_SDK,
                message = "Decryption error: $decryptionErrorEvent",
            )
        }
    }

    suspend operator fun invoke(
        verificationErrorEvent: VerificationErrorEvent
    ) {
        val recency = verificationErrorEvent.recency.toRecency()
        val causedBy = verificationErrorEvent.createdBy.toCausedBy()
        verificationErrorEvent.field.toField()?.let { field ->
            notifyIntegrityVerificationErrorsTotalMetric(
                uid = verificationErrorEvent.uid,
                field = field,
                addressMatchingDefaultShare = verificationErrorEvent.addressMatchingDefaultShare,
                recency = recency,
                causedBy = causedBy,
            )
        }
        if (recency != Recency.before_2024 && verificationErrorEvent.addressMatchingDefaultShare) {
            notifyIntegrityErroringUsersTotalMetric(
                volumeType = VolumeType.unknown,
                causedBy = causedBy,
            )
        }
    }

    suspend operator fun invoke(
        blockVerificationErrorEvent: BlockVerificationErrorEvent
    ) {
        notifyIntegrityBlockVerificationErrorsTotalMetric(
            isRetryHelped = blockVerificationErrorEvent.retryHelped
        )
        if (blockVerificationErrorEvent.retryHelped.not()) {
            notifyIntegrityErroringUsersTotalMetric(
                volumeType = blockVerificationErrorEvent.volumeType.toVolumeType(),
                causedBy = CausedBy.UNKNOWN,
            )
        }
    }

    private suspend fun notifyIntegrityDecryptionErrorsTotalMetric(
        uid: String,
        field: Field,
        recency: Recency,
        causedBy: CausedBy,
    ) {
        getPrimaryUser()?.let { user ->
            notifyIntegrityDecryptionErrorsTotalMetric(
                userId = user.userId,
                uid = uid,
                field = field,
                recency = recency,
                causedBy = causedBy,
            )
        }
    }

    private suspend fun notifyIntegrityDecryptionErrorsTotalMetric(
        userId: UserId,
        uid: String,
        field: Field,
        recency: Recency,
        causedBy: CausedBy,
    ) {
        enqueueObservabilityEvent(
            observabilityData = IntegrityDecryptionErrorsTotal(
                Labels = IntegrityDecryptionErrorsTotal.LabelsData(
                    field = field,
                    recency = recency,
                    causedBy = causedBy,
                )
            ),
            constraint = countConstraint(
                userId = userId,
                key = "decryptionErrorsTotal.$uid",
                maxCount = 1,
            )
        )
    }

    private suspend fun notifyIntegrityVerificationErrorsTotalMetric(
        uid: String,
        field: Field,
        addressMatchingDefaultShare: Boolean?,
        recency: Recency,
        causedBy: CausedBy,
    ) {
        getPrimaryUser()?.let { user ->
            notifyIntegrityVerificationErrorsTotalMetric(
                userId = user.userId,
                uid = uid,
                field = field,
                addressMatchingDefaultShare = addressMatchingDefaultShare,
                recency = recency,
                causedBy = causedBy,
            )
        }
    }

    private suspend fun notifyIntegrityVerificationErrorsTotalMetric(
        userId: UserId,
        uid: String,
        field: Field,
        addressMatchingDefaultShare: Boolean?,
        recency: Recency,
        causedBy: CausedBy,
    ) {
        enqueueObservabilityEvent(
            IntegrityVerificationErrorsTotal(
                Labels = IntegrityVerificationErrorsTotal.LabelsData(
                    field = field,
                    addressMatchingDefaultShare = when (addressMatchingDefaultShare) {
                        true -> YesNoUnknown.yes
                        false -> YesNoUnknown.no
                        null -> YesNoUnknown.unknown
                    },
                    recency = recency,
                    causedBy = causedBy,
                )
            ),
            constraint = countConstraint(
                userId = userId,
                key = "verificationErrorsTotal.$uid",
                maxCount = 1,
            )
        )
    }

    private suspend fun notifyIntegrityBlockVerificationErrorsTotalMetric(
        isRetryHelped: Boolean,
    ) {
        enqueueObservabilityEvent(
            IntegrityBlockVerificationErrorsTotal(
                Labels = IntegrityBlockVerificationErrorsTotal.LabelsData(
                    retryHelped = if (isRetryHelped) YesNo.yes else YesNo.no
                )
            )
        )
    }

    private suspend fun notifyIntegrityErroringUsersTotalMetric(
        volumeType: VolumeType,
        causedBy: CausedBy,
    ) {
        getPrimaryUser()?.let { user ->
            notifyIntegrityErroringUsersTotalMetric(
                userId = user.userId,
                volumeType = volumeType,
                causedBy = causedBy,
                isFreeUser = user.isWithoutProtonSubscription,
            )
        }
    }

    private suspend fun notifyIntegrityErroringUsersTotalMetric(
        userId: UserId,
        volumeType: VolumeType,
        causedBy: CausedBy,
        isFreeUser: Boolean,
    ) {
        enqueueObservabilityEvent(
            IntegrityErroringUsersTotal(
                Labels = IntegrityErroringUsersTotal.LabelsData(
                    volumeType = volumeType,
                    userPlan = if (isFreeUser) Plan.free else Plan.paid,
                    causedBy = causedBy,
                )
            ),
            constraint = minimumIntervalConstraint(
                userId = userId,
                schemaId = IntegrityErroringUsersTotal.SCHEMA_ID,
                interval = 5.minutes,
            )
        )
    }
}
