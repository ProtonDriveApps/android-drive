/*
 * Copyright (c) 2023-2024 Proton AG.
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

@file:Suppress("MatchingDeclarationName")

package me.proton.core.drive.db.test

import me.proton.core.drive.entitlement.data.db.entity.EntitlementEntity

suspend fun UserContext.entitlements(
    entitlements: Map<String, Any>,
) {
    db.entitlementDao.insertOrUpdate(*entitlements.map { (key, value) ->
        EntitlementEntity(
            userId = user.userId,
            key = key,
            value = value.toString()
        )
    }.toTypedArray())
}
