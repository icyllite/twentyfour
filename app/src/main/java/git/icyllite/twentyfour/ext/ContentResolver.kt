/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.ext

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun ContentResolver.queryFlow(
    uri: Uri,
    projection: Array<String>? = null,
    queryArgs: Bundle? = Bundle(),
) = conflatedCallbackFlow {
    // Each query will have its own cancellationSignal.
    // Before running any new query the old cancellationSignal must be cancelled
    // to ensure the currently running query gets interrupted so that we don't
    // send data across the channel if we know we received a newer set of data.
    var cancellationSignal = CancellationSignal()
    // ContentObserver.onChange can be called concurrently so make sure
    // access to the cancellationSignal is synchronized.
    val mutex = Mutex()

    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            launch(Dispatchers.IO) {
                mutex.withLock {
                    cancellationSignal.cancel()
                    cancellationSignal = CancellationSignal()
                }
                runCatching {
                    trySend(query(uri, projection, queryArgs, cancellationSignal))
                }
            }
        }
    }

    registerContentObserver(uri, true, observer)

    // The first set of values must always be generated and cannot (shouldn't) be cancelled.
    launch(Dispatchers.IO) {
        runCatching {
            trySend(
                query(uri, projection, queryArgs, null)
            )
        }.onFailure {
            Log.e("ContentResolver", "Failed to query $uri", it)
        }
    }

    awaitClose {
        // Stop receiving content changes.
        unregisterContentObserver(observer)
        // Cancel any possibly running query.
        cancellationSignal.cancel()
    }
}
