/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
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
import kotlinx.coroutines.withContext
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.asFlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.getOrNull

class AddOrRemoveFromPlaylistsViewModel(application: Application) : TwentyfourViewModel(application) {
    private val audioUri = MutableStateFlow<Uri?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val audio = audioUri
        .filterNotNull()
        .flatMapLatest {
            mediaRepository.audio(it)
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistToHasAudio = audioUri
        .filterNotNull()
        .flatMapLatest {
            mediaRepository.audioPlaylistsStatus(it)
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val providerOfAudio = audioUri
        .filterNotNull()
        .flatMapLatest { mediaRepository.providerOf(it) }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            FlowResult.Loading()
        )

    fun loadAudio(audioUri: Uri) {
        this.audioUri.value = audioUri
    }

    suspend fun addToPlaylist(playlistUri: Uri) {
        audioUri.value?.let {
            withContext(Dispatchers.IO) {
                mediaRepository.addAudioToPlaylist(playlistUri, it)
            }
        }
    }

    suspend fun removeFromPlaylist(playlistUri: Uri) {
        audioUri.value?.let {
            withContext(Dispatchers.IO) {
                mediaRepository.removeAudioFromPlaylist(playlistUri, it)
            }
        }
    }

    /**
     * Create a new playlist in the same provider as the audio.
     */
    suspend fun createPlaylist(name: String) {
        withContext(Dispatchers.IO) {
            audioUri.value?.let {
                providerOfAudio.value.getOrNull()?.let { provider ->
                    mediaRepository.createPlaylist(provider, name)
                }
            }
        }
    }
}
