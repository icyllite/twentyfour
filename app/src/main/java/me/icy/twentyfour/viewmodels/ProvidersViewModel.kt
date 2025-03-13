/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package me.icy.twentyfour.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import me.icy.twentyfour.models.Provider

open class ProvidersViewModel(application: Application) : TwelveViewModel(application) {
    val providers = mediaRepository.allVisibleProviders

    val navigationProvider = mediaRepository.navigationProvider
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null,
        )

    fun setNavigationProvider(provider: Provider) {
        mediaRepository.setNavigationProvider(provider)
    }
}
