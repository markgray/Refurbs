/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION", "UnusedImport")

package com.example.android.networkconnect

import android.net.NetworkInfo
import java.io.InputStream
import java.io.InputStreamReader
import javax.net.ssl.HttpsURLConnection

/**
 * Sample interface containing bare minimum methods needed for an asynchronous task
 * to update the UI Context.
 */
interface DownloadCallback {
    /**
     * Progress codes used by our `onProgressUpdate` method.
     */
    interface Progress {
        companion object {
            /**
             * Progress code that could be used for reporting an IO error, but is not used.
             */
            const val ERROR: Int = -1
            /**
             * This Progress code is "published" after [HttpsURLConnection.connect] returns without
             * throwing an exception.
             */
            const val CONNECT_SUCCESS: Int = 0
            /**
             * This Progress code is "published" after [HttpsURLConnection.getInputStream] returns
             * an [InputStream] without throwing an exception.
             */
            const val GET_INPUT_STREAM_SUCCESS: Int = 1
            /**
             * This Progress code is "published" while the [InputStreamReader] reading the
             * [InputStream] is in progress, along with the percent of the file downloaded so far.
             */
            const val PROCESS_INPUT_STREAM_IN_PROGRESS: Int = 2
            /**
             * This Progress code is "published" after the download completes successfully.
             */
            const val PROCESS_INPUT_STREAM_SUCCESS: Int = 3
        }
    }

    /**
     * Indicates that the callback handler needs to update its appearance or information based on
     * the result of the task. Expected to be called from the main thread.
     *
     * @param result string read.
     */
    fun updateFromDownload(result: String?)

    /**
     * Get the device's active network status in the form of a NetworkInfo object.
     */
    fun getActiveNetworkInfo(): NetworkInfo

    /**
     * Indicate to callback handler any progress update.
     *
     * @param progressCode must be one of the constants defined in DownloadCallback.Progress.
     * @param percentComplete must be 0-100.
     */
    fun onProgressUpdate(progressCode: Int, percentComplete: Int)

    /**
     * Indicates that the download operation has finished. This method is called even if the
     * download hasn't completed successfully.
     */
    fun finishDownloading()
}