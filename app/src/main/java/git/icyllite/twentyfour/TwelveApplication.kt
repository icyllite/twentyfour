/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour

import android.app.Application
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.MainScope
import git.icyllite.twentyfour.database.TwentyfourDatabase
import git.icyllite.twentyfour.repositories.MediaRepository
import git.icyllite.twentyfour.repositories.ProvidersRepository
import git.icyllite.twentyfour.repositories.ResumptionPlaylistRepository
import git.icyllite.twentyfour.ui.coil.ThumbnailMapper

@androidx.annotation.OptIn(UnstableApi::class)
class TwentyfourApplication : Application(), SingletonImageLoader.Factory {
    private val coroutineScope = MainScope()
    private val database by lazy { TwentyfourDatabase.get(applicationContext) }
    val providersRepository by lazy {
        ProvidersRepository(applicationContext, coroutineScope, database)
    }
    val mediaRepository by lazy {
        MediaRepository(applicationContext, coroutineScope, providersRepository, database)
    }
    val resumptionPlaylistRepository by lazy { ResumptionPlaylistRepository(database) }

    override fun onCreate() {
        super.onCreate()

        // Observe dynamic colors changes
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(this)
        .components {
            add(ThumbnailMapper)
        }
        .build()
}
