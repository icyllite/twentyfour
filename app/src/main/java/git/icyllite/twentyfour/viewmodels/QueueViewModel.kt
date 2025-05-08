/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import git.icyllite.twentyfour.ext.queueFlow

class QueueViewModel(application: Application) : TwentyfourViewModel(application) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val queue = mediaControllerFlow
        .flatMapLatest { it.queueFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            listOf()
        )

    fun moveItem(from: Int, to: Int) {
        mediaController.value?.moveMediaItem(from, to)
    }

    fun removeItem(index: Int) {
        mediaController.value?.removeMediaItem(index)
    }

    fun playItem(index: Int) {
        mediaController.value?.apply {
            seekTo(index, 0)
            play()
        }
    }
}
