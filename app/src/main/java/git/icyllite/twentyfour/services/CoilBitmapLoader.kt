/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.services

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

/**
 * A [BitmapLoader] that uses Coil.
 */
@OptIn(androidx.media3.common.util.UnstableApi::class)
class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String) = true

    override fun decodeBitmap(data: ByteArray) = getImage(data)

    override fun loadBitmap(uri: Uri) = getImage(uri)

    private fun getImage(data: Any?) = scope.future(Dispatchers.IO) {
        val imageRequest = ImageRequest.Builder(context)
            .data(data)
            .allowHardware(false)
            .build()

        val imageResult = context.imageLoader.execute(imageRequest)

        val image = imageResult.image ?: error("Cannot decode the image")

        image.toBitmap()
    }
}
