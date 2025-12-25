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
@file:Suppress("DEPRECATION", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.networkconnect

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment

/**
 * Sample Activity demonstrating how to connect to the network and fetch raw HTML. It uses a
 * [Fragment] that encapsulates the network operations in an AsyncTask. This sample uses a
 * [TextView] to display output.
 */
class MainActivity : AppCompatActivity(), DownloadCallback {
    /**
     * Reference to the [TextView] showing fetched data, so we can clear it with a button
     * as necessary.
     */
    private var mDataText: TextView? = null

    /**
     * Keep a reference to the [NetworkFragment] which owns the AsyncTask object that is used to
     * execute network ops.
     */
    private var mNetworkFragment: NetworkFragment? = null

    /**
     * Boolean telling us whether a download is in progress, so we don't trigger overlapping
     * downloads with consecutive button clicks.
     */
    private var mDownloading = false

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and set our content
     * view to our layout file `R.layout.sample_main`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to `rootView`, with the `listener` argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars] as
     * the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize our [TextView] field  [mDataText] by finding the view with id
     * `R.id.data_text`, and initialize our [NetworkFragment] field [mNetworkFragment]
     * with a handle to an instance constructed to retrieve the url
     * "[Google](https://www.google.com)" (creating it if need be, or using the
     * fragment manager to fetch a handle to an already running instance).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_main)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        mDataText = findViewById(R.id.data_text)
        mNetworkFragment = NetworkFragment.getInstance(
            supportFragmentManager,
            "https://www.google.com"
        )
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We fetch a [MenuInflater]
     * for this context and use it to inflate our menu layout file `R.menu.main` into our [Menu]
     * parameter [menu], then return `true` to the caller so the menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return `true` for the menu to be displayed;
     * if you return `false` it will not be shown.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We switch on the
     * [MenuItem.getItemId] of our [MenuItem] parameter [item]:
     *
     *  * `R.id.fetch_action`: ("FETCH") we call our method [startDownload] to start a background
     *  download and return `true` to the caller to consume the event here.
     *
     *  * `R.id.clear_action`: ("CLEAR") we call our method [finishDownloading] to cancel any
     *  ongoing download, set the text of [TextView] field [mDataText] to the empty string, and
     *  return `true` to the caller to consume the event here.
     *
     * If it is not an item we know about, we return `false` to the caller to allow normal menu
     * processing to proceed.
     *
     * @param item The menu item that was selected.
     * @return boolean Return `false` to allow normal menu processing to
     * proceed, `true` to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.fetch_action -> {
                startDownload()
                return true
            }

            R.id.clear_action -> {
                finishDownloading()
                mDataText!!.text = ""
                return true
            }
        }
        return false
    }

    /**
     * Starts the background downloading. If our [Boolean] field [mDownloading] is `false` (we are
     * not already downloading) and our [NetworkFragment] field [mNetworkFragment] is not `null` we
     * call the [NetworkFragment.startDownload] method of [mNetworkFragment] to start the
     * non-blocking execution of its `DownloadTask`. We then set [mDownloading] to `true`.
     */
    private fun startDownload() {
        if (!mDownloading && mNetworkFragment != null) {
            // Execute the async download.
            mNetworkFragment!!.startDownload()
            mDownloading = true
        }
    }

    /**
     * Method from the [DownloadCallback] interface used by [NetworkFragment] to send us the data it
     * downloaded. If our [String] parameter [result] is not `null` we set the text of our [TextView]
     * field [mDataText] to it, otherwise we set it to the string with resource ID
     * `R.string.connection_error` ("Connection error.").
     *
     * @param result string containing the data downloaded.
     */
    override fun updateFromDownload(result: String?) {
        if (result != null) {
            mDataText!!.text = result
        } else {
            mDataText!!.text = getString(R.string.connection_error)
        }
    }

    /**
     * Method from the [DownloadCallback] interface used by [NetworkFragment] to retrieve the
     * device's active network status in the form of a [NetworkInfo] object. We initialize the
     * [ConnectivityManager] variable `val connectivityManager` with a handle to the system level
     * service [Context.CONNECTIVITY_SERVICE], then use it to return a [NetworkInfo] object for the
     * current default network or `null` if no default network is currently active.
     *
     * @return a [NetworkInfo] object for the current default network or `null` if no default
     * network is currently active
     */
    @Suppress("DEPRECATION")
    override fun getActiveNetworkInfo(): NetworkInfo {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.activeNetworkInfo!!
    }

    /**
     * Indicates that the download operation has finished. This method is called even if the
     * download hasn't completed successfully, or the user has clicked the "CLEAR" button.
     * First we set our [Boolean] field [mDownloading] to `false`, then if our [NetworkFragment]
     * field [mNetworkFragment] is not `null` we call its [NetworkFragment.cancelDownload] method
     * to cancel (and interrupt if necessary) any ongoing `DownloadTask` execution.
     */
    override fun finishDownloading() {
        mDownloading = false
        if (mNetworkFragment != null) {
            mNetworkFragment!!.cancelDownload()
        }
    }

    /**
     * Called by the `DownloadTask` AsyncTask of [NetworkFragment] to report progress.
     * We switch on the value of our [Int] parameter [progressCode]:
     *
     *  * [DownloadCallback.Progress.ERROR]: we ignore
     *  * [DownloadCallback.Progress.CONNECT_SUCCESS]: we ignore
     *  * [DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS]: we ignore
     *  * [DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS]: We set the text of [TextView]
     *  field [mDataText] to the percent contained in our [Int] parameter [percentComplete]
     *  * [DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS]: we ignore
     *
     * @param progressCode must be one of the constants defined in [DownloadCallback.Progress].
     * @param percentComplete must be 0-100.
     */
    @SuppressLint("SetTextI18n")
    override fun onProgressUpdate(progressCode: Int, percentComplete: Int) {
        when (progressCode) {
            DownloadCallback.Progress.ERROR -> {}
            DownloadCallback.Progress.CONNECT_SUCCESS -> {}
            DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS -> {}
            DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS -> mDataText!!.text = "$percentComplete%"
            DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS -> {}
        }
    }
}
