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
import git.icyllite.twentyfour.ext.ARTISTS_SORTING_REVERSE_KEY
import git.icyllite.twentyfour.ext.ARTISTS_SORTING_STRATEGY_KEY
import git.icyllite.twentyfour.ext.artistsSortingRule
import git.icyllite.twentyfour.ext.preferenceFlow
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.asFlowResult
import git.icyllite.twentyfour.models.SortingRule

class ArtistsViewModel(application: Application) : TwentyfourViewModel(application) {
    val sortingRule = sharedPreferences.preferenceFlow(
        ARTISTS_SORTING_STRATEGY_KEY,
        ARTISTS_SORTING_REVERSE_KEY,
        getter = SharedPreferences::artistsSortingRule,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val artists = sortingRule
        .flatMapLatest { mediaRepository.artists(it) }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    fun setSortingRule(sortingRule: SortingRule) {
        sharedPreferences.artistsSortingRule = sortingRule
    }
}
