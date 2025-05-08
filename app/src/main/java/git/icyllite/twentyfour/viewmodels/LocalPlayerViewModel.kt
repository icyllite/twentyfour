/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import git.icyllite.twentyfour.ext.applicationContext
import git.icyllite.twentyfour.ext.availableCommandsFlow
import git.icyllite.twentyfour.ext.eventsFlow
import git.icyllite.twentyfour.ext.isPlayingFlow
import git.icyllite.twentyfour.ext.mediaMetadataFlow
import git.icyllite.twentyfour.ext.next
import git.icyllite.twentyfour.ext.playbackParametersFlow
import git.icyllite.twentyfour.ext.playbackProgressFlow
import git.icyllite.twentyfour.ext.playbackStateFlow
import git.icyllite.twentyfour.ext.repeatModeFlow
import git.icyllite.twentyfour.ext.shuffleModeEnabled
import git.icyllite.twentyfour.ext.shuffleModeFlow
import git.icyllite.twentyfour.ext.toThumbnail
import git.icyllite.twentyfour.ext.typedRepeatMode
import git.icyllite.twentyfour.models.Error
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.PlaybackProgress
import git.icyllite.twentyfour.models.PlaybackState
import git.icyllite.twentyfour.models.RepeatMode
import git.icyllite.twentyfour.models.Thumbnail

/**
 * A view model useful to playback stuff locally (not in the playback service).
 */
class LocalPlayerViewModel(application: Application) : AndroidViewModel(application) {
    enum class PlaybackSpeed(val value: Float) {
        ONE(1f),
        ONE_POINT_FIVE(1.5f),
        TWO(2f),
        ZERO_POINT_FIVE(0.5f);

        companion object {
            fun fromValue(value: Float) = entries.firstOrNull {
                it.value == value
            }
        }
    }

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(application)
    }

    // ExoPlayer
    private val exoPlayer = ExoPlayer.Builder(applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val eventsFlow = exoPlayer.eventsFlow()
        .flowOn(Dispatchers.Main)
        .shareIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            replay = 1
        )

    private val playbackState = exoPlayer.playbackStateFlow(eventsFlow)
        .flowOn(Dispatchers.Main)

    val mediaMetadata = exoPlayer.mediaMetadataFlow(eventsFlow)
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = MediaMetadata.EMPTY
        )

    val isPlaying = exoPlayer.isPlayingFlow(eventsFlow)
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    val shuffleMode = exoPlayer.shuffleModeFlow(eventsFlow)
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    val repeatMode = exoPlayer.repeatModeFlow(eventsFlow)
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = RepeatMode.NONE
        )

    val mediaArtwork = combine(
        mediaMetadata,
        playbackState,
    ) { mediaMetadata, playbackState ->
        when (playbackState) {
            PlaybackState.BUFFERING -> FlowResult.Loading()
            else -> mediaMetadata.toThumbnail(applicationContext)?.let {
                FlowResult.Success<Thumbnail, Error>(it)
            } ?: FlowResult.Error(Error.NOT_FOUND)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = FlowResult.Loading()
        )

    val playbackProgress = exoPlayer.playbackProgressFlow(eventsFlow)
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = PlaybackProgress.EMPTY
        )

    val playbackParameters = exoPlayer.playbackParametersFlow(eventsFlow)
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = PlaybackParameters.DEFAULT
        )

    val availableCommands = exoPlayer.availableCommandsFlow(eventsFlow)
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = Player.Commands.EMPTY
        )

    override fun onCleared() {
        exoPlayer.release()

        super.onCleared()
    }

    fun setMediaUris(uris: Iterable<Uri>) {
        exoPlayer.apply {
            // Initialize shuffle and repeat modes
            typedRepeatMode = sharedPreferences.typedRepeatMode
            shuffleModeEnabled = sharedPreferences.shuffleModeEnabled

            setMediaItems(
                uris.map {
                    MediaItem.fromUri(it)
                }
            )
            prepare()
            play()
        }
    }

    fun togglePlayPause() {
        exoPlayer.apply {
            if (playbackState == ExoPlayer.STATE_ENDED) {
                seekTo(0)
            } else if (playWhenReady) {
                pause()
            } else {
                play()
            }
        }
    }

    fun shufflePlaybackSpeed() {
        val playbackSpeed = PlaybackSpeed.fromValue(
            exoPlayer.playbackParameters.speed
        ) ?: PlaybackSpeed.ONE

        exoPlayer.setPlaybackSpeed(playbackSpeed.next().value)
    }

    fun seekToPosition(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun toggleShuffleMode() {
        exoPlayer.shuffleModeEnabled = exoPlayer.shuffleModeEnabled.not()
        sharedPreferences.shuffleModeEnabled = exoPlayer.shuffleModeEnabled
    }

    fun toggleRepeatMode() {
        exoPlayer.typedRepeatMode = exoPlayer.typedRepeatMode.next()
        sharedPreferences.typedRepeatMode = exoPlayer.typedRepeatMode
    }

    fun seekToPrevious() {
        exoPlayer.apply {
            val currentMediaItemIndex = currentMediaItemIndex
            seekToPrevious()
            if (this.currentMediaItemIndex < currentMediaItemIndex) {
                play()
            }
        }
    }

    fun seekToNext() {
        exoPlayer.apply {
            seekToNext()
            play()
        }
    }
}
