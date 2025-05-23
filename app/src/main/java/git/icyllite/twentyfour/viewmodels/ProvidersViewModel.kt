/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import git.icyllite.twentyfour.models.Provider

open class ProvidersViewModel(application: Application) : TwentyfourViewModel(application) {
    val providers = providersRepository.allProviders

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
