/*
 * Copyright 2013 The Android Open Source Project
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
package com.example.android.interactivechart

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * A sample application that allows you to navigate a simple line graph using touch gestures.
 */
class MainActivity : ComponentActivity() {
    /**
     * Reference to the [InteractiveLineGraphView] with id R.id.chart in our layout
     */
    private lateinit var mGraphView: InteractiveLineGraphView

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge
     * to edge display, then we call our super's implementation of `onCreate`, set our
     * content view to our layout file `R.layout.activity_main`, and initialize our
     * [InteractiveLineGraphView] field [mGraphView] by finding the view with ID `R.id.chart`.
     *
     *We call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to [mGraphView], with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `insets.left`, the right margin set to
     * `insets.right`, the top margin set to `insets.top`, and the bottom margin
     * set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    @Suppress("ReplaceNotNullAssertionWithElvisReturn")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mGraphView = findViewById(R.id.chart)
        ViewCompat.setOnApplyWindowInsetsListener(mGraphView) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
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
    }

    /**
     * Initialize the contents of the Activity's standard options menu. You should place your menu
     * items in to [Menu] parameter [menu]. This is only called once, the first time the options
     * menu is displayed. To update the menu every time it is displayed, see [onPrepareOptionsMenu].
     * First we call our super's implementation of `onCreateOptionsMenu`, then we use a [MenuInflater]
     * with `this` context to inflate the menu layout file with ID `R.menu.main` into our [Menu]
     * parameter [menu] and return `true` so that the menu will be displayed.
     *
     * @param menu – The options menu in which you place your items.
     * @return You must return `true` for the menu to be displayed; if you return `false` it will
     * not be shown. We always return `true`.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We `when` branch on the
     * [MenuItem.getItemId] value (kotlin `itemId` property) of the [MenuItem] parameter [item]:
     *  - `R.id.action_zoom_in` "Demo zoom in": we call the [InteractiveLineGraphView.zoomIn] method
     *  of [InteractiveLineGraphView] field [mGraphView] and return `true` to consume the event.
     *  - `R.id.action_zoom_out` "Demo zoom out": we call the [InteractiveLineGraphView.zoomOut]
     *  method of [InteractiveLineGraphView] field [mGraphView] and return `true` to consume the
     *  event.
     *  - `R.id.action_pan_left` "Demo pan left": we call the [InteractiveLineGraphView.panLeft]
     *  method of [InteractiveLineGraphView] field [mGraphView] and return `true` to consume the
     *  event.
     *  - `R.id.action_pan_right` "Demo pan right": we call the [InteractiveLineGraphView.panRight]
     *  method of [InteractiveLineGraphView] field [mGraphView] and return `true` to consume the
     *  event.
     *  - `R.id.action_pan_up` "Demo pan up": we call the [InteractiveLineGraphView.panUp] method
     *  of [InteractiveLineGraphView] field [mGraphView] and return `true` to consume the event.
     *  - `R.id.action_pan_down` "Demo pan down": we call the [InteractiveLineGraphView.panDown]
     *  method of [InteractiveLineGraphView] field [mGraphView] and return `true` to consume the
     *  event.
     *
     * If [MenuItem] parameter [item] matches none of the resource IDs checked by our `when`
     * statement we return the value returned by our super's implementation of `onOptionsItemSelected`
     *
     * @param item – The [MenuItem] that was selected.
     * @return `false` to allow normal menu processing to proceed, `true` to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_zoom_in -> {
                mGraphView.zoomIn()
                return true
            }

            R.id.action_zoom_out -> {
                mGraphView.zoomOut()
                return true
            }

            R.id.action_pan_left -> {
                mGraphView.panLeft()
                return true
            }

            R.id.action_pan_right -> {
                mGraphView.panRight()
                return true
            }

            R.id.action_pan_up -> {
                mGraphView.panUp()
                return true
            }

            R.id.action_pan_down -> {
                mGraphView.panDown()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
