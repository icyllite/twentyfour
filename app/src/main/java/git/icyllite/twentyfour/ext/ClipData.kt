/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.ext

import android.content.ClipData

fun ClipData.asArray() = buildList {
    for (i in 0 until itemCount) {
        add(getItemAt(i))
    }
}
