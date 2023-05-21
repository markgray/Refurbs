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
import android.view.MenuItem

/**
 * A sample application that allows you to navigate a simple line graph using touch gestures.
 */
class MainActivity : Activity() {
    /**
     * Reference to the `InteractiveLineGraphView` with id R.id.chart in our layout
     */
    private var mGraphView: InteractiveLineGraphView? = null

    /**
     * Called when the activity is starting. First we
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mGraphView = findViewById(R.id.chart)
    }

    /**
     * TODO: Add kdoc
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
                mGraphView!!.zoomIn()
                return true
            }

            R.id.action_zoom_out -> {
                mGraphView!!.zoomOut()
                return true
            }

            R.id.action_pan_left -> {
                mGraphView!!.panLeft()
                return true
            }

            R.id.action_pan_right -> {
                mGraphView!!.panRight()
                return true
            }

            R.id.action_pan_up -> {
                mGraphView!!.panUp()
                return true
            }

            R.id.action_pan_down -> {
                mGraphView!!.panDown()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}