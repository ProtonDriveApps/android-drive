/*
 * Copyright (c) 2026 Proton AG.
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
package me.proton.core.drive.base.data.log

import me.proton.core.drive.base.domain.extension.toHex
import me.proton.core.drive.base.domain.log.ItemLifecycle
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.util.kotlin.CoreLogger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject

/**
 * Writes lifecycle lines at verbose level, which only logcat keeps: the Proton Core Sentry tree
 * drops VERBOSE (no event, no breadcrumb), the user log ignores verbose messages without a
 * throwable, and the debug file log starts at DEBUG. If a Core upgrade starts sending VERBOSE to
 * Sentry, these lines would become breadcrumbs. The backup key salt lives only in this process.
 */
class ItemLifecycleImpl internal constructor(
    private val configurationProvider: ConfigurationProvider,
    private val write: (String) -> Unit,
    private val salt: ByteArray,
) : ItemLifecycle {

    @Inject
    constructor(configurationProvider: ConfigurationProvider) : this(
        configurationProvider = configurationProvider,
        write = { line -> CoreLogger.v(TAG, line) },
        salt = ByteArray(SALT_SIZE).also { bytes -> SecureRandom().nextBytes(bytes) },
    )

    override fun accepted(flow: ItemLifecycle.Flow, id: String, operationId: String?) {
        if (!configurationProvider.itemLifecycleLogEnabled) return
        write(
            buildString {
                append("accepted flow=").append(flow.value).append(" id=").append(id)
                operationId?.let { append(" op=").append(it) }
            }
        )
    }

    override fun finished(flow: ItemLifecycle.Flow, id: String, outcome: ItemLifecycle.Outcome) {
        if (!configurationProvider.itemLifecycleLogEnabled) return
        write("finished flow=${flow.value} id=$id outcome=${outcome.value}")
    }

    override fun backupKey(uriString: String): String =
        MessageDigest.getInstance("SHA-256")
            .apply { update(salt) }
            .digest(uriString.toByteArray())
            .toHex()
            .take(KEY_LENGTH)

    internal companion object {
        const val TAG = "core.drive.lifecycle"
        private const val SALT_SIZE = 16
        private const val KEY_LENGTH = 16
    }
}
