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
@file:Suppress(
    "UNNECESSARY_NOT_NULL_ASSERTION",
    "DEPRECATION",
    "SameParameterValue",
    "SENSELESS_COMPARISON",
    "ReplaceNotNullAssertionWithElvisReturn",
    "ReplaceJavaStaticMethodWithKotlinAnalog",
    "LiftReturnOrAssignment",
    "MemberVisibilityCanBePrivate"
)

package com.example.android.networkconnect

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Implementation of headless [Fragment] that runs an [AsyncTask] to fetch data from the network.
 */
class NetworkFragment : Fragment() {
    /**
     * [DownloadCallback] we use to report progress and post results, set from the context passed to
     * our [onAttach] override ([MainActivity] in our case).
     */
    private var mCallback: DownloadCallback? = null

    /**
     * [DownloadTask] which is currently running, created and started in our [startDownload] method,
     * canceled and set to `null` in our [cancelDownload] method.
     */
    private var mDownloadTask: DownloadTask? = null

    /**
     * URL we are to download, set from our arguments bundle (key is [URL_KEY]) in our [onCreate]
     * method.
     */
    private var mUrlString: String? = null

    /**
     * Called to do initial creation of a fragment. First we call our super's implementation of
     * `onCreate`. Then we call [setRetainInstance] with `true` to retain this [Fragment] across
     * configuration changes in the host Activity, and initialize our [String] field [mUrlString]
     * by fetching the arguments supplied when the fragment was instantiated and retrieving the
     * [String] stored under the key [URL_KEY].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retain this Fragment across configuration changes in the host Activity.
        retainInstance = true
        mUrlString = requireArguments().getString(URL_KEY)
    }

    /**
     * Called when a fragment is first attached to its context. First we call our super's
     * implementation of `onAttach`, then we cast our [Context] parameter [context] to a
     * [DownloadCallback] in order to initialize our [DownloadCallback] field [mCallback].
     *
     * @param context the [Context] of the activity we are attached to ([MainActivity]
     * in our case).
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Host Activity will handle callbacks from task.
        mCallback = context as DownloadCallback
    }

    /**
     * Called when the fragment is no longer attached to its activity.  This is called after
     * [onDestroy]. First we call our super's implementation of `onDetach`, then we set our
     * [DownloadCallback] field [mCallback] to `null`.
     */
    override fun onDetach() {
        super.onDetach()
        // Clear reference to host Activity.
        mCallback = null
    }

    /**
     * Called when the fragment is no longer in use.  This is called after [onStop] and before
     * [onDetach]. We call our method [cancelDownload] to cancel our [DownloadTask] (if it is
     * running) then call our super's implementation of `onDestroy`.
     */
    override fun onDestroy() {
        // Cancel task when Fragment is destroyed.
        cancelDownload()
        super.onDestroy()
    }

    /**
     * Start non-blocking execution of [DownloadTask]. First we call our method [cancelDownload]
     * to cancel any existing [DownloadTask] (if it is running), then we initialize our [DownloadTask]
     * field  [mDownloadTask] with a new instance and start it running to download the http url
     * contained in our [String] field [mUrlString].
     */
    fun startDownload() {
        cancelDownload()
        mDownloadTask = DownloadTask()
        mDownloadTask!!.execute(mUrlString)
    }

    /**
     * Cancel (and interrupt if necessary) any ongoing [DownloadTask] execution. If our [DownloadTask]
     * field [mDownloadTask] is not `null` we call its [DownloadTask.cancel] method to attempt to
     * cancel execution of the task passing `true` as the `mayInterruptIfRunning` flag so that it
     * will be interrupted if need be. We then set [mDownloadTask] to `null`.
     */
    fun cancelDownload() {
        if (mDownloadTask != null) {
            mDownloadTask!!.cancel(true)
            mDownloadTask = null
        }
    }

    /**
     * Implementation of AsyncTask that runs a network operation on a background thread.
     */
    @SuppressLint("StaticFieldLeak")
    private inner class DownloadTask : AsyncTask<String, Int, DownloadTask.Result?>() {
        /**
         * Wrapper class that serves as a union of a result value and an exception. When the
         * download task has completed, either the result value or exception can be a non-`null`
         * value. This allows you to pass exceptions to the UI thread that were thrown during
         * [doInBackground].
         */
        inner class Result {
            /**
             * Result value (if constructed with a string)
             */
            var mResultValue: String? = null

            /**
             * Exception thrown (if constructed with an exception).
             */
            var mException: Exception? = null

            /**
             * Constructor used to return a string result.
             *
             * @param resultValue string to return as a result in our [String] field [mResultValue]
             */
            constructor(resultValue: String?) {
                mResultValue = resultValue
            }

            /**
             * Constructor used to return an exception.
             *
             * @param exception exception to return as a result in our [Exception] field [mException]
             */
            constructor(exception: Exception?) {
                mException = exception
            }
        }

        /**
         * Cancel background network operation if we do not have network connectivity. If our
         * [DownloadCallback] field [mCallback] is not `null` we call its
         * [DownloadCallback.getActiveNetworkInfo] method to initialize [NetworkInfo] variable
         * `val networkInfo` with details about the currently active default data network. If
         * `networkInfo` is `null`, or its [NetworkInfo.isConnected] method returns `false`, or its
         * [NetworkInfo.getType] method (kotlin `type` field) does not return either
         * [ConnectivityManager.TYPE_WIFI] or [ConnectivityManager.TYPE_MOBILE] we are not connected
         * so we call the [DownloadCallback.updateFromDownload] method of [mCallback] to set its
         * result to `null`, then call our [cancel] method to cancel this download, interrupting it
         * if it is running.
         */
        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
            if (mCallback != null) {
                val networkInfo: NetworkInfo = mCallback!!.getActiveNetworkInfo()
                if (networkInfo == null || !networkInfo.isConnected ||
                    (networkInfo.type != ConnectivityManager.TYPE_WIFI
                        && networkInfo.type != ConnectivityManager.TYPE_MOBILE)
                ) {
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback!!.updateFromDownload(null)
                    cancel(true)
                }
            }
        }

        /**
         * Defines work to perform on the background thread. We initialize [Result] variable
         * `var result` to `null`, then if we have not been cancelled and our [Array] of [String]
         * parameter [urls] is not `null` and its length is greater than 0 we set [String] variable
         * `val urlString` to `urls[0]` and wrapped in a try block intended to catch any exception
         * we initialize [URL] variable `val url` with an instance constructed for the string
         * representation in `urlString`. We then set the [String] variable `val resultString` to
         * the value returned by our method [downloadUrl] after downloading [URL] `url`. If
         * `resultString` is not `null` we set `result` to an instance of [Result] constructed from
         * `resultString`, and if it is `null` we throw [IOException] "No response received." If our
         * catch block catches an exception we set `result` to an instance of `Result` constructed
         * from that exception. Finally we return `result` to the caller.
         *
         * @param urls the http url we are to download in `urls[0]`
         * @return an instance of [Result] constructed from the string that we downloaded from
         * our url, or any exception that was thrown.
         */
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg urls: String): Result? {
            var result: Result? = null
            if (!isCancelled && urls != null && urls.isNotEmpty()) {
                val urlString = urls[0]
                try {
                    val url = URL(urlString)
                    val resultString: String? = downloadUrl(url)
                    if (resultString != null) {
                        result = Result(resultString)
                    } else {
                        throw IOException("No response received.")
                    }
                } catch (e: Exception) {
                    result = Result(e)
                }
            }
            return result
        }


        /**
         * Send [DownloadCallback] a progress update. First we call our super's implementation of
         * `onProgressUpdate`, then if the length of our [Array] of [Int] parameter [values] is
         * greater than or equal to 2 we call the [DownloadCallback.onProgressUpdate] method of
         * [DownloadCallback] field [mCallback] with the parameters `values[0]` and `values[1]` so
         * it can update the UI with the current percentage of the download.
         *
         * @param values `values[0]` contains a [DownloadCallback.Progress] code, and
         * `values[1]` contains a percentage downloaded value for the progress code
         * [DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS].
         */
        @Deprecated("Deprecated in Java")
        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            if (values.size >= 2) {
                mCallback!!.onProgressUpdate(values[0]!!, values[1]!!)
            }
        }

        /**
         * Updates the [DownloadCallback] with the result. If our [Result] parameter [result] is not
         * `null` and our [DownloadCallback] field [mCallback] is not null, we branch on the value
         * of the [Result.mException] field of [result]:
         *
         *  * not `null`: (an exception was caught) We call the [DownloadCallback.updateFromDownload]
         *  method of [mCallback] with the detail message string from the [Result.mException] field
         *  of `result`.
         *
         *  * `null`: (no exception was caught) We call the [DownloadCallback.updateFromDownload]
         *  method of [mCallback] with the [Result.mResultValue] field of [result].
         *
         * In either case we call the [DownloadCallback.finishDownloading] method of [mCallback]
         *
         * @param result a [Result] instance returned from the [doInBackground] method to
         * report that the download operation has finished.
         */
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: Result?) {
            if (result != null && mCallback != null) {
                if (result.mException != null) {
                    mCallback!!.updateFromDownload(result.mException!!.message)
                } else if (result.mResultValue != null) {
                    mCallback!!.updateFromDownload(result.mResultValue)
                }
                mCallback!!.finishDownloading()
            }
        }

        /**
         * Override to add special behavior for cancelled AsyncTask. We ignore.
         *
         * @param result The result, if any, computed in `doInBackground(Object[])`, can be null
         */
        @Deprecated("Deprecated in Java")
        override fun onCancelled(result: Result?) {
        }

        /**
         * Given a URL, sets up a connection and gets the HTTP response body from the server. If the
         * network request is successful, it returns the response body in String form. Otherwise, it
         * will throw an [IOException]. We initialize our [InputStream] variable `var stream`,
         * [HttpsURLConnection] variable `var connection` and [String] variable `var result` to
         * `null`. Then in a try block without a catch block (only a finally block to clean up) we
         * set `connection` to the [HttpsURLConnection] connection returned by calling the
         * [URL.openConnection] method of [url], set its read timeout to 3000ms, its connection
         * timeout to 3000m its HTTP method to "GET", and set its [HttpsURLConnection.doInput]
         * field to `true` (we intend to use the URL connection for input). We then call the
         * [HttpsURLConnection.connect] method of `connection` to open a communications link to the
         * resource referenced by this URL. We then call the [publishProgress] method to report the
         * progress [DownloadCallback.Progress.CONNECT_SUCCESS]. We initialize our [Int] variable
         * `val responseCode` with the status code from the HTTP response message of `connection`
         * and if it is not [HttpsURLConnection.HTTP_OK] we throw an [IOException] with the message
         * "HTTP error code: " followed by the `responseCode`. We set `stream` to an input
         * stream that reads from `connection` then call the `publishProgress` method to
         * report the progress GET_INPUT_STREAM_SUCCESS. If `stream` is not null we set
         * `result` to the string returned by our method `readStream` when it reads up to
         * 500 bytes from `stream` then call the `publishProgress` method to report the
         * progress [DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS]. In the finally block if
         * `stream` is not `null` we call its [InputStream.close] method to close the input stream
         * and release any system resources associated with it, and if `connection` is not `null`
         * we call its [HttpsURLConnection.disconnect] method to indicate that other requests to the
         * server are unlikely in the near future.
         *
         * When done, successful or not, we return `result` to our caller.
         *
         * @param url http URL we are to down load
         * @return string read from the [InputStream] of the [HttpsURLConnection] we open
         * to the server at our [URL] parameter [url].
         * @throws IOException if an I/O exception occurs.
         */
        @Throws(IOException::class)
        private fun downloadUrl(url: URL): String? {
            var stream: InputStream? = null
            var connection: HttpsURLConnection? = null
            var result: String? = null
            try {
                connection = url.openConnection() as HttpsURLConnection
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                connection.readTimeout = 3000
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connection!!.connectTimeout = 3000
                // For this use case, set HTTP method to GET.
                connection.requestMethod = "GET"
                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                connection.doInput = true
                // Open communications link (network traffic occurs here).
                connection.connect()
                publishProgress(DownloadCallback.Progress.CONNECT_SUCCESS)
                val responseCode: Int = connection.responseCode
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw IOException("HTTP error code: $responseCode")
                }
                // Retrieve the response body as an InputStream.
                stream = connection.inputStream
                publishProgress(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0)
                if (stream != null) {
                    // Converts Stream to String with max length of 500.
                    result = readStream(stream, 500)
                    publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS, 0)
                }
            } finally {
                // Close Stream and disconnect HTTPS connection.
                stream?.close()
                connection?.disconnect()
            }
            return result
        }

        /**
         * Converts the contents of an [InputStream] to a [String]. First we initialize [String]
         * variable `var result` to `null`. We initialize [InputStreamReader] variable `val reader`
         * with a new instance constructed to read from our [InputStream] parameter [stream] using
         * the charset "UTF-8". We allocate [Int] parameter [maxLength] chars for [CharArray]
         * variable `val buffer`, initialize both [Int] variable `var numChars` and [Int] variable
         * `var readSize` to 0. Then while `numChars` is less than [maxLength] and `readSize` is not
         * equal to -1 we add `readSize` to `numChars` and set [Int] variable `val pct` to
         * `100*numChars/maxLength` and call the [publishProgress] method to report the progress
         * [DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS] and the value `pct`.
         * Then we call the [InputStreamReader.read] method of `reader` to read into `buffer` at an
         * offset of `numChars` and a maximum of the length of `buffer` minus the value
         * `numChars`, saving the number of `char` read in `readSize`.
         *
         * When done with the while loop, if `numChars` is not equal to -1, we set it to the
         * max of `numChars` and `maxLength`, then set `result` to an instance which is constructed
         * of the first `numChars` entries in `buffer`. Finally we return `result` to the caller.
         *
         * @param stream the [InputStream] to read from
         * @param maxLength Maximum number of bytes to read
         * @return [String] read from our [InputStream] parameter [stream]
         * @throws IOException if an I/O error occurs
         */
        @Throws(IOException::class)
        @Suppress("SameParameterValue")
        private fun readStream(stream: InputStream, maxLength: Int): String? {
            var result: String? = null
            // Read InputStream using the UTF-8 charset.
            val reader = InputStreamReader(stream, "UTF-8")
            // Create temporary buffer to hold Stream data with specified max length.
            val buffer = CharArray(maxLength)
            // Populate temporary buffer with Stream data.
            var numChars = 0
            var readSize = 0
            while (numChars < maxLength && readSize != -1) {
                numChars += readSize
                val pct = 100 * numChars / maxLength
                publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS, pct)
                readSize = reader.read(buffer, numChars, buffer.size - numChars)
            }
            if (numChars != -1) {
                // The stream was not empty.
                // Create String that is actual length of response body if actual length was less than
                // max length.
                numChars = Math.min(numChars, maxLength)
                result = String(buffer, 0, numChars)
            }
            return result
        }
    }

    companion object {
        /**
         * TAG used when we add our fragment, or search for an already existing instance.
         */
        const val TAG: String = "NetworkFragment"

        /**
         * Key in the arguments bundle for the [NetworkFragment] we create that contains the url
         * it is supposed to download.
         */
        private const val URL_KEY = "UrlKey"

        /**
         * Static initializer for [NetworkFragment] that sets the URL of the host it will be downloading
         * from. We first try to initialize [NetworkFragment] variable `var networkFragment` by using
         * our [FragmentManager] parameter [fragmentManager] to search for a fragment with the tag
         * [NetworkFragment.TAG] and if the result is `null` we initialize it with a new instance,
         * initialize [Bundle] variable `val args` with a new instance in which we store our [String]
         * parameter [url] under the key [URL_KEY] and set it as the argument bundle for
         * `networkFragment`. Then we use [fragmentManager] to begin a transaction which then adds
         * `networkFragment` using [TAG] as the tag and then we commit the transaction. Finally we
         * return `networkFragment` to the caller (whether we are reusing an existing one or the one
         * we had to add).
         *
         * @param fragmentManager [FragmentManager] for interacting with fragments associated
         * with this activity
         * @param url http url to download
         * @return a [NetworkFragment] instance, either an existing one if the
         * [FragmentManager.findFragmentByTag] method of [FragmentManager] parameter
         * [fragmentManager] finds one or a new one created to download our parameter
         * `String url`.
         */
        fun getInstance(fragmentManager: FragmentManager, url: String?): NetworkFragment {
            // Recover NetworkFragment in case we are re-creating the Activity due to a config change.
            // This is necessary because NetworkFragment might have a task that began running before
            // the config change and has not finished yet.
            // The NetworkFragment is recoverable via this method because it calls
            // setRetainInstance(true) upon creation.
            var networkFragment = fragmentManager
                .findFragmentByTag(TAG) as NetworkFragment?
            if (networkFragment == null) {
                networkFragment = NetworkFragment()
                val args = Bundle()
                args.putString(URL_KEY, url)
                networkFragment.arguments = args
                fragmentManager.beginTransaction().add(networkFragment, TAG).commit()
            }
            return networkFragment
        }
    }
}