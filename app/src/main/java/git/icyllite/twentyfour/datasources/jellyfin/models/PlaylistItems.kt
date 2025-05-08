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
data class PlaylistItems(
    @SerialName("ItemIds") val itemIds: List<UUID>,
)
