/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.asFlowResult
import git.icyllite.twentyfour.models.Result

class SearchViewModel(application: Application) : TwentyfourViewModel(application) {
    private val searchQuery = MutableStateFlow("" to false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults = searchQuery
        .mapLatest {
            val (query, immediate) = it
            if (!immediate && query.isNotEmpty()) {
                delay(500)
            }
            query
        }
        .flatMapLatest { query ->
            query.trim().takeIf { it.isNotEmpty() }?.let {
                mediaRepository.search("%${it}%")
            } ?: flowOf(Result.Success(listOf()))
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    fun setSearchQuery(query: String, immediate: Boolean = false) {
        searchQuery.value = query to immediate
    }
}
