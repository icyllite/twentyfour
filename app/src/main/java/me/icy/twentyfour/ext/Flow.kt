/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package me.icy.twentyfour.ext

import android.database.Cursor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.icy.twentyfour.models.ColumnIndexCache

fun <T> Flow<Cursor?>.mapEachRow(
    mapping: (ColumnIndexCache) -> T,
) = map { it.mapEachRow(mapping) }
