/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.displayingbitmaps.ui

import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.android.displayingbitmaps.BuildConfig
import com.example.android.displayingbitmaps.util.Utils

/**
 * Simple [FragmentActivity] to hold the main [ImageGridFragment] and not much else.
 */
class ImageGridActivity : FragmentActivity() {
    /**
     * Called when the activity is starting. First we check if the gradle generated constant
     * [BuildConfig.DEBUG] is true, and if it is we call the [Utils.enableStrictMode] method to set
     * the policy for what potentially suspect actions should be detected and logged using
     * [android.os.StrictMode]. Then we call our super's implementation of `onCreate`.
     *
     * If the [FragmentManager] for interacting with fragments associated with this activity can not
     * find a fragment with the tag [TAG], we initialize [FragmentTransaction] variable `val ft` by
     * starting a series of edit operations on the Fragments associated with this [FragmentManager],
     * use it to add a new instance of [ImageGridFragment] to the container [android.R.id.content]
     * (the [ViewGroup] of the entire content area of our Activity) using the tag name [TAG].
     * We then call the [FragmentTransaction.commit] method of `ft` to schedule a commit of this
     * transaction.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            Utils.enableStrictMode()
        }
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentByTag(TAG) == null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.add(android.R.id.content, ImageGridFragment(), TAG)
            ft.commit()
        }
    }

    companion object {
        /**
         * Tag name for our `ImageGridFragment` fragment, to later retrieve the fragment with
         */
        private const val TAG = "ImageGridActivity"
    }
}