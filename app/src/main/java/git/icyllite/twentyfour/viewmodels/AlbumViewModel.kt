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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import git.icyllite.twentyfour.ext.resources
import git.icyllite.twentyfour.models.Audio
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.asFlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.foldLatest
import git.icyllite.twentyfour.models.UniqueItem
import git.icyllite.twentyfour.utils.MimeUtils
import kotlin.reflect.safeCast

class AlbumViewModel(application: Application) : TwentyfourViewModel(application) {
    private val albumUri = MutableStateFlow<Uri?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val album = albumUri
        .filterNotNull()
        .flatMapLatest {
            mediaRepository.album(it)
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    val tracks = album
        .foldLatest(
            onSuccess = {
                it.second.sortedWith(
                    compareBy(
                        { audio -> audio.discNumber ?: 0 },
                        Audio::trackNumber,
                    )
                )
            },
            onError = { _, _ ->
                listOf()
            },
        )
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            listOf()
        )

    sealed interface AlbumContent : UniqueItem<AlbumContent> {
        data class DiscHeader(val discNumber: Int) : AlbumContent {
            override fun areItemsTheSame(other: AlbumContent) =
                DiscHeader::class.safeCast(other)?.let {
                    discNumber == it.discNumber
                } ?: false

            override fun areContentsTheSame(other: AlbumContent) = true
        }

        class AudioItem(val audio: Audio) : AlbumContent {
            override fun areItemsTheSame(other: AlbumContent) = AudioItem::class.safeCast(
                other
            )?.let {
                audio.areItemsTheSame(it.audio)
            } ?: false

            override fun areContentsTheSame(other: AlbumContent) = AudioItem::class.safeCast(
                other
            )?.let {
                audio.areContentsTheSame(it.audio)
            } ?: false
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val albumContent = tracks
        .mapLatest {
            val discToTracks = it.groupBy { audio ->
                audio.discNumber
            }

            val hideHeaders = with(discToTracks.keys) {
                size == 1 && firstOrNull() == 1
            }

            buildList {
                discToTracks.keys.sortedBy { disc ->
                    disc ?: 0
                }.forEach { discNumber ->
                    discNumber?.takeUnless { hideHeaders }?.let { i ->
                        add(AlbumContent.DiscHeader(i))
                    }

                    discToTracks[discNumber]?.let { tracks ->
                        addAll(
                            tracks.map { audio ->
                                AlbumContent.AudioItem(audio)
                            }
                        )
                    }
                }
            }.toList()
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            listOf()
        )

    val albumFileTypes = album
        .foldLatest(
            onSuccess = {
                it.second
                    .mapNotNull { audio -> audio.mimeType }
                    .distinct()
                    .takeIf { mimeTypes -> mimeTypes.size <= 2 }
                    ?.mapNotNull { mimeType -> MimeUtils.mimeTypeToDisplayName(mimeType) }
                    .orEmpty()
            },
            onError = { _, _ ->
                listOf()
            },
        )
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            listOf()
        )

    fun loadAlbum(albumUri: Uri) {
        this.albumUri.value = albumUri
    }

    fun playAlbum(startFrom: Audio? = null) {
        tracks.value.takeUnless { it.isEmpty() }?.let { audios ->
            playAudio(audios, startFrom?.let { audios.indexOf(it) } ?: 0)
        }
    }

    fun shufflePlayAlbum() {
        tracks.value.takeUnless { it.isEmpty() }?.let { audios ->
            playAudio(audios.shuffled(), 0)
        }
    }

    fun addToQueue() {
        tracks.value.takeUnless { it.isEmpty() }?.let { audios ->
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
        tracks.value.takeUnless { it.isEmpty() }?.let { audios ->
            mediaController.value?.apply {
                addMediaItems(
                    currentMediaItemIndex + 1,
                    audios.map { it.toMedia3MediaItem(resources) },
                )

                // If the added items are the only one, play them
                if (mediaItemCount == audios.count()) {
                    play()
                }
            }
        }
    }
}
