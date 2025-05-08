/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:UseSerializers(UUIDSerializer::class)

package git.icyllite.twentyfour.datasources.jellyfin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import git.icyllite.twentyfour.datasources.jellyfin.serializers.UUIDSerializer
import java.util.UUID

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class CreatePlaylist(
    @SerialName("Name") val name: String,
    @SerialName("Ids") val ids: List<UUID>,
    @SerialName("Users") val users: List<TODO>,
    @SerialName("IsPublic") val isPublic: Boolean,
)
