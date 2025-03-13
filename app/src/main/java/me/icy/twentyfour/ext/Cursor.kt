/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package me.icy.twentyfour.ext

import android.database.Cursor
import me.icy.twentyfour.models.ColumnIndexCache

fun <T> Cursor?.mapEachRow(
    mapping: (ColumnIndexCache) -> T,
) = this?.use { cursor ->
    if (!cursor.moveToFirst()) {
        return@use emptyList<T>()
    }

    val columnIndexCache = ColumnIndexCache(cursor)

    val data = buildList {
        do {
            add(mapping(columnIndexCache))
        } while (cursor.moveToNext())
    }

    data.toList()
} ?: emptyList()
