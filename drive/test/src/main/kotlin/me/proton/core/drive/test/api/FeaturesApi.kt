/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.test.api

import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.featureflag.data.remote.resource.UnleashToggleResource
import me.proton.core.featureflag.data.remote.resource.UnleashVariantResource
import me.proton.core.featureflag.data.remote.response.GetUnleashTogglesResponse
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.coreFeatures() = routing {
    get("/core/v4/features") {
        response {
            """{"Code":1000,"Features":[]}"""
        }
    }
}

fun MockWebServer.featureFrontend(vararg ids: String) = featureFrontend(
    toggles = ids.map { id ->
        UnleashToggleResource(
            name = id,
            variant = UnleashVariantResource(
                name = "variant",
                enabled = false,
            )
        )
    }
)


fun MockWebServer.featureFrontend(toggles: List<UnleashToggleResource>) = routing {
    get("/feature/v2/frontend") {
        jsonResponse {
            GetUnleashTogglesResponse(
                code = ProtonApiCode.SUCCESS.toInt(),
                toggles = toggles
            )
        }
    }
}
