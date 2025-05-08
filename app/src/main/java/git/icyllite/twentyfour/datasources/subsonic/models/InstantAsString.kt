/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.datasources.subsonic.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
typealias InstantAsString = @Serializable(with = InstantSerializer::class) Instant
