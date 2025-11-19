/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version2.0 (the "License");
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
package com.example.android.adaptiveicon

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * An empty [AppCompatActivity] which is launched when the user clicks the icon in the launcher whose
 * label is "Icon 1". This demonstrates the use of an adaptive icon for the application. The `onCreate`
 * override simply sets the content view to a basic layout, handles edge-to-edge display by applying
 * system bar insets as margins, and logs its creation.
 */
class Activity1 : AppCompatActivity() {
    /**
     * Called when the activity is first created. This is where you should do all of your normal
     * static set up: create views, bind data to lists, etc. This method also provides you with a
     * Bundle containing the activity's previously frozen state, if there was one.
     *
     * We first call [enableEdgeToEdge] to allow our app to draw behind the system bars. Then we call
     * our super's implementation of `onCreate` and set our content view to our layout file
     * `R.layout.empty_activity`. We then find the root content view and set an
     * [ViewCompat.setOnApplyWindowInsetsListener] on it. This listener receives the window insets
     * (like the status bar and navigation bar sizes) and applies them as margins to the actual
     * content layout, preventing our UI from being obscured by the system bars. Finally, we log
     * that the activity has been created.
     *
     * @param savedInstanceState We do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Directly set the content view using the layout resource ID
        setContentView(R.layout.empty_activity)

        // Find the root view to apply insets
        val view = findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply the insets as a margin to the view's first child,
            // which is the actual content layout.
            (v as? ViewGroup)?.getChildAt(0)?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        Log.i(TAG, "Activity1")
    }

    companion object {
        /**
         * Tag used for logging.
         */
        const val TAG: String = "AdaptiveIconSample"
    }
}
