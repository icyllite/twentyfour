/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.asFlowResult

class ArtistViewModel(application: Application) : TwentyfourViewModel(application) {
    private val artistUri = MutableStateFlow<Uri?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val artist = artistUri
        .filterNotNull()
        .flatMapLatest {
            mediaRepository.artist(it)
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    fun loadAlbum(artistUri: Uri) {
        this.artistUri.value = artistUri
    }
}
