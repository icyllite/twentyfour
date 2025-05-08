/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:UseSerializers(UUIDSerializer::class)

package git.icyllite.twentyfour.datasources.jellyfin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import git.icyllite.twentyfour.datasources.jellyfin.serializers.UUIDSerializer

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class Lyrics(
    @SerialName("Lyrics") val lyrics: List<LyricLine>? = null
)
