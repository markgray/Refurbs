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

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

/**
 * A sample application that allows you to navigate a simple line graph using touch gestures.
 */
class MainActivity : Activity() {
    /**
     * Reference to the [InteractiveLineGraphView] with id R.id.chart in our layout
     */
    private lateinit var mGraphView: InteractiveLineGraphView

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.activity_main], and initialize our
     * [InteractiveLineGraphView] field [mGraphView] by finding the view with ID [R.id.chart]
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mGraphView = findViewById(R.id.chart)
    }

    /**
     * Initialize the contents of the Activity's standard options menu. You should place your menu
     * items in to [Menu] parameter [menu]. This is only called once, the first time the options
     * menu is displayed. To update the menu every time it is displayed, see [onPrepareOptionsMenu].
     * First we call our super's implementation of `onCreateOptionsMenu`, then we use a [MenuInflater]
     * with `this` context to inflate the menu layout file with ID [R.menu.main] into our [Menu]
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
     * TODO: Add kdoc
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