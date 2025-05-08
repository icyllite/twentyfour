/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.datasources.jellyfin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class AuthenticateUserResult(
    @SerialName("AccessToken") val accessToken: String? = null,
)
