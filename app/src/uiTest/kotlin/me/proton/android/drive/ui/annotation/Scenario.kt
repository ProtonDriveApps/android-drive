/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.ui.annotation

import me.proton.core.test.quark.v2.command.populate
import me.proton.core.test.quark.v2.command.quotaSetUsedSpace
import me.proton.core.test.quark.v2.command.volumeCreate
import me.proton.core.test.rule.annotation.AnnotationTestData
import me.proton.core.test.rule.annotation.TestUserData
import me.proton.core.test.rule.annotation.mapToUser
import kotlin.math.roundToInt

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Scenario(
    val forTag: String = "main",
    val value: Int = 0,
    val isPhotos: Boolean = false,
    val isDevice: Boolean = false,
    val sharedWithUserTag: String = "",
    val quota: Quota = Quota()
)

val Scenario.annotationTestData: AnnotationTestData<Scenario>
    get() = AnnotationTestData.forUserMap(
        default = this,
        implementation = { data: Scenario, usersMap: Map<String, TestUserData> ->
            usersMap[data.forTag]?.let { user ->
                if (!data.quota.isDefault()) {
                    volumeCreate(user.mapToUser())
                    if (data.quota.percentageFull in 1..100) {
                        val usedSpace =
                            (data.quota.value.toDouble() * data.quota.percentageFull / 100)
                                .roundToInt()
                        quotaSetUsedSpace(
                            user = user.mapToUser(),
                            usedSpace = "${usedSpace}${data.quota.unit}",
                            product = data.quota.product
                        )
                    }
                }

                if (value > 0) {
                    val sharingUser =
                        usersMap[data.sharedWithUserTag].takeIf { data.sharedWithUserTag.isNotEmpty() }
                    // Function populate(...) will handle case with sharingUser==null
                    populate(
                        user.mapToUser(),
                        data.value,
                        isDevice,
                        isPhotos,
                        sharingUser?.mapToUser()
                    )
                }
            } ?: error(
                "Could not populate Drive scenario. Given user \"${data.forTag}\" is not seeded."
            )
        }
    )
