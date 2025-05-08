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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import git.icyllite.twentyfour.ext.resources
import git.icyllite.twentyfour.models.Album
import git.icyllite.twentyfour.models.Audio
import git.icyllite.twentyfour.models.Error
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.asFlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.foldLatest
import git.icyllite.twentyfour.models.FlowResult.Companion.getOrNull
import git.icyllite.twentyfour.models.FlowResult.Companion.mapLatestData
import git.icyllite.twentyfour.models.FlowResult.Companion.mapLatestDataOrNull
import git.icyllite.twentyfour.models.MediaType
import git.icyllite.twentyfour.models.Playlist
import git.icyllite.twentyfour.models.Result
import git.icyllite.twentyfour.models.Result.Companion.map

class MediaItemViewModel(application: Application) : TwentyfourViewModel(application) {
    private val _uri = MutableStateFlow<Uri?>(null)
    val uri = _uri.asStateFlow()

    private val fromAlbum = MutableStateFlow(false)
    private val fromArtist = MutableStateFlow(false)
    private val fromGenre = MutableStateFlow(false)
    private val fromNowPlaying = MutableStateFlow(false)
    private val playlistUri = MutableStateFlow<Uri?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mediaType = uri
        .mapLatest { uri -> uri?.let { mediaRepository.mediaTypeOf(it) } }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val data = combine(
        uri.filterNotNull(),
        mediaType.filterNotNull(),
    ) { uri, mediaType ->
        when (mediaType) {
            MediaType.ALBUM -> mediaRepository.album(uri)
            MediaType.ARTIST -> mediaRepository.artist(uri).mapLatest {
                it.map { album -> album.first to listOf() }
            }

            MediaType.AUDIO -> mediaRepository.audio(uri).mapLatest {
                it.map { audio -> audio to listOf(audio) }
            }

            MediaType.GENRE -> mediaRepository.genre(uri).mapLatest {
                it.map { genre -> genre.first to listOf() }
            }

            MediaType.PLAYLIST -> mediaRepository.playlist(uri)
        }
    }
        .flatMapLatest { it }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = FlowResult.Loading(),
        )

    val mediaItem = data
        .mapLatestData { it.first }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = FlowResult.Loading(),
        )

    val tracks = data
        .foldLatest(
            onSuccess = {
                it.second
            },
            onError = { _, _ ->
                listOf()
            },
        )
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val playlist = playlistUri
        .flatMapLatest { playlistUri ->
            playlistUri?.let {
                mediaRepository.playlist(it)
            } ?: flowOf(Result.Error(Error.NOT_FOUND))
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = FlowResult.Loading(),
        )

    val showQueueButtons = combine(
        tracks,
        fromNowPlaying
    ) { tracks, fromNowPlaying -> tracks.isNotEmpty() && !fromNowPlaying }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val canToggleFavorite = mediaType
        .mapLatest { it == MediaType.AUDIO }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    val canRemoveFromPlaylist = combine(mediaType, playlist) { mediaType, playlist ->
        mediaType == MediaType.AUDIO && playlist.getOrNull()?.first?.type == Playlist.Type.PLAYLIST
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val canAddOrRemoveFromPlaylists = mediaType
        .mapLatest { it == MediaType.AUDIO }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val albumUri = mediaItem
        .mapLatestDataOrNull()
        .mapLatest {
            when (it) {
                is Audio -> it.albumUri
                else -> null
            }
        }
        .combine(fromAlbum) { uri, fromAlbum -> uri?.takeIf { !fromAlbum } }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val artistUri = mediaItem
        .mapLatestDataOrNull()
        .mapLatest {
            when (it) {
                is Audio -> it.artistUri
                is Album -> it.artistUri
                else -> null
            }
        }
        .combine(fromArtist) { uri, fromArtist -> uri?.takeIf { !fromArtist } }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val genreUri = mediaItem
        .mapLatestDataOrNull()
        .mapLatest {
            when (it) {
                is Audio -> it.genreUri
                else -> null
            }
        }
        .combine(fromGenre) { uri, fromGenre -> uri?.takeIf { !fromGenre } }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    fun setUri(uri: Uri) {
        _uri.value = uri
    }

    fun setFromAlbum(fromAlbum: Boolean) {
        this.fromAlbum.value = fromAlbum
    }

    fun setFromArtist(fromArtist: Boolean) {
        this.fromArtist.value = fromArtist
    }

    fun setFromGenre(fromGenre: Boolean) {
        this.fromGenre.value = fromGenre
    }

    fun setFromNowPlaying(fromNowPlaying: Boolean) {
        this.fromNowPlaying.value = fromNowPlaying
    }

    fun setPlaylistUri(playlistUri: Uri?) {
        this.playlistUri.value = playlistUri
    }

    fun playNow() {
        tracks.value.takeIf { it.isNotEmpty() }?.let {
            playAudio(it, 0)
        }
    }

    fun addToQueue() {
        tracks.value.takeIf { it.isNotEmpty() }?.let { audios ->
            mediaController.value?.apply {
                addMediaItems(audios.map { it.toMedia3MediaItem(resources) })

                // If the added items are the only one, play them
                if (mediaItemCount == audios.count()) {
                    play()
                }
            }
        }
    }

    fun playNext() {
        tracks.value.takeIf { it.isNotEmpty() }?.let { audios ->
            mediaController.value?.apply {
                addMediaItems(
                    currentMediaItemIndex + 1,
                    audios.map { it.toMedia3MediaItem(resources) }
                )

                // If the added items are the only one, play them
                if (mediaItemCount == audios.count()) {
                    play()
                }
            }
        }
    }

    suspend fun toggleFavorites() {
        mediaItem.value.getOrNull()?.let {
            val audio = it as? Audio ?: return@let

            withContext(Dispatchers.IO) {
                mediaRepository.setFavorite(audio.uri, !audio.isFavorite)
            }
        }
    }

    suspend fun removeAudioFromPlaylist() {
        uri.value?.takeIf { mediaType.value == MediaType.AUDIO }?.let {
            playlistUri.value?.let { playlistUri ->
                withContext(Dispatchers.IO) {
                    mediaRepository.removeAudioFromPlaylist(playlistUri, it)
                }
            }
        }
    }
}
