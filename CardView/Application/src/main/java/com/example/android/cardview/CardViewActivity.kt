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
package com.example.android.cardview

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Launcher Activity for the CardView sample app.
 */
class CardViewActivity : FragmentActivity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`. Then we set our content view to our layout file R.layout.activity_card_view.
     * If our parameter `savedInstanceState` is null this is the first time we have been called
     * so we use the FragmentManager for interacting with fragments associated with this activity to
     * begin a new `FragmentTransaction` to which we chain a command to add a new instance of
     * `CardViewFragment` to the container view with id R.id.container, followed by a chained
     * command to commit the transaction.
     *
     * @param savedInstanceState this is null the first time we are called, and we use this fact to
     * decide if we need to create and add our fragment (when called after
     * a configuration change it is not null, and the system will have seen
     * to restoring the old fragment).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_view)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, CardViewFragment.newInstance())
                .commit()
        }
    }
}