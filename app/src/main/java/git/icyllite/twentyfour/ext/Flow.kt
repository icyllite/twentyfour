/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.ext

import android.database.Cursor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import git.icyllite.twentyfour.models.ColumnIndexCache

fun <T> Flow<Cursor?>.mapEachRow(
    mapping: (ColumnIndexCache) -> T,
) = map { it.mapEachRow(mapping) }
