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

package me.proton.core.drive.backup.data.extension

import android.net.NetworkCapabilities
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager.NetworkStatusInfo.TransportType

/**
 * Transports of a network are a set, not a single value: a VPN reports [TransportType.VPN]
 * together with the transports of the network it runs on. Empty when the network has no
 * transport at all, [TransportType.OTHER] stands for a transport we have no constant for.
 */
fun NetworkCapabilities.toTransportTypes(): Set<TransportType> =
    (0..MAX_TRANSPORT)
        .filter { transport -> hasTransport(transport) }
        .mapTo(mutableSetOf()) { transport -> transport.toTransportType() }

private fun Int.toTransportType(): TransportType = when (this) {
    NetworkCapabilities.TRANSPORT_WIFI -> TransportType.WIFI
    NetworkCapabilities.TRANSPORT_CELLULAR -> TransportType.CELLULAR
    NetworkCapabilities.TRANSPORT_ETHERNET -> TransportType.ETHERNET
    NetworkCapabilities.TRANSPORT_VPN -> TransportType.VPN
    NetworkCapabilities.TRANSPORT_BLUETOOTH -> TransportType.BLUETOOTH
    NetworkCapabilities.TRANSPORT_USB -> TransportType.USB
    NetworkCapabilities.TRANSPORT_WIFI_AWARE -> TransportType.WIFI_AWARE
    NetworkCapabilities.TRANSPORT_LOWPAN -> TransportType.LOWPAN
    NetworkCapabilities.TRANSPORT_THREAD -> TransportType.THREAD
    NetworkCapabilities.TRANSPORT_SATELLITE -> TransportType.SATELLITE
    else -> TransportType.OTHER
}

/**
 * Raw transport bitmask, which unlike [toTransportTypes] also carries transports this build has
 * no constant for, so that they can still be read back from a log.
 */
fun NetworkCapabilities.toTransportTypesMask(): Long =
    (0..MAX_TRANSPORT).fold(0L) { mask, transport ->
        if (hasTransport(transport)) mask or (1L shl transport) else mask
    }

// Transports are probed one by one, the array of them is not public API. Highest bit position
// that the platform can hold, as it keeps the transports of a network in a single long.
private const val MAX_TRANSPORT = 63
