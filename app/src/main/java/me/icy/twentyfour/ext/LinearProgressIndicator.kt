/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package me.icy.twentyfour.ext

import com.google.android.material.progressindicator.LinearProgressIndicator
import me.icy.twentyfour.models.FlowResult

/**
 * @see LinearProgressIndicator.setProgressCompat
 */
fun <T, E> LinearProgressIndicator.setProgressCompat(status: FlowResult<T, E>) {
    when (status) {
        is FlowResult.Loading -> {
            if (!isIndeterminate) {
                hide()
                isIndeterminate = true
            }

            show()
        }

        else -> {
            hide()
        }
    }
}
