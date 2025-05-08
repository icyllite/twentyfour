/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.viewmodels

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import git.icyllite.twentyfour.ext.ALBUMS_SORTING_REVERSE_KEY
import git.icyllite.twentyfour.ext.ALBUMS_SORTING_STRATEGY_KEY
import git.icyllite.twentyfour.ext.albumsSortingRule
import git.icyllite.twentyfour.ext.preferenceFlow
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.asFlowResult
import git.icyllite.twentyfour.models.SortingRule

class AlbumsViewModel(application: Application) : TwentyfourViewModel(application) {
    val sortingRule = sharedPreferences.preferenceFlow(
        ALBUMS_SORTING_STRATEGY_KEY,
        ALBUMS_SORTING_REVERSE_KEY,
        getter = SharedPreferences::albumsSortingRule,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val albums = sortingRule
        .flatMapLatest { mediaRepository.albums(it) }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    fun setSortingRule(sortingRule: SortingRule) {
        sharedPreferences.albumsSortingRule = sortingRule
    }
}
