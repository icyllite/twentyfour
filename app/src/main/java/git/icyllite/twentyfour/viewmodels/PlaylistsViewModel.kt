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
import git.icyllite.twentyfour.ext.PLAYLISTS_SORTING_REVERSE_KEY
import git.icyllite.twentyfour.ext.PLAYLISTS_SORTING_STRATEGY_KEY
import git.icyllite.twentyfour.ext.playlistsSortingRule
import git.icyllite.twentyfour.ext.preferenceFlow
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.asFlowResult
import git.icyllite.twentyfour.models.SortingRule

class PlaylistsViewModel(application: Application) : TwentyfourViewModel(application) {
    val navigationProvider = mediaRepository.navigationProvider
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null,
        )

    val sortingRule = sharedPreferences.preferenceFlow(
        PLAYLISTS_SORTING_STRATEGY_KEY,
        PLAYLISTS_SORTING_REVERSE_KEY,
        getter = SharedPreferences::playlistsSortingRule,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlists = sortingRule
        .flatMapLatest { mediaRepository.playlists(it) }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    fun setSortingRule(sortingRule: SortingRule) {
        sharedPreferences.playlistsSortingRule = sortingRule
    }
}
