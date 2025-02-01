/*
* Copyright 2014 The Android Open Source Project
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
package com.example.android.directoryselection

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

/**
 * Launcher Activity for the Directory Selection sample app.
 */
class DirectorySelectionActivity : FragmentActivity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file `R.layout.activity_directory_selection`.
     * If our [Bundle] parameter [savedInstanceState] is `null` this is the first time that we were
     * called so we need to construct and add our [DirectorySelectionFragment]. To do this we use
     * the [FragmentManager] for interacting with fragments associated with this activity to begin a
     * [FragmentTransaction] in which we add a new instance of [DirectorySelectionFragment]
     * to the container with id `R.id.container`, and then we commit the [FragmentTransaction].
     *
     * @param savedInstanceState used only to determine if we are being called for the first time
     * (`null`) in which case we need to create and add our fragment. On a
     * configuration change our fragment will be restored by the system.
     */
    @Suppress("ReplaceNotNullAssertionWithElvisReturn")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directory_selection)
        val rootView = findViewById<FrameLayout>(R.id.container)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top+actionBar!!.height/2
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, DirectorySelectionFragment.newInstance())
                .commit()
        }
    }
}
