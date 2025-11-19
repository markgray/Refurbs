/*
 * Copyright 2013 The Android Open Source Project
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
@file:Suppress(
    "unused",
    "DEPRECATION",
    "ReplaceNotNullAssertionWithElvisReturn",
    "ReplaceJavaStaticMethodWithKotlinAnalog"
)
// TODO: Migrate from SharedPreferences to Preferences DataStore
package com.example.android.activityinstrumentation

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import java.util.Arrays
import androidx.core.content.edit
import androidx.core.graphics.Insets

/**
 * Basic activity with a spinner. The spinner should persist its position to disk every time a
 * new selection is made.
 */
class MainActivity : AppCompatActivity() {
    /**
     * Handle to default shared preferences for this activity.
     */
    private var mPrefs: SharedPreferences? = null

    /**
     * Handle to the spinner in this Activity's layout.
     */
    private var mSpinner: Spinner? = null

    /**
     * Setup activity state.
     * TODO: Continue here.
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Inflate UI from res/layout/activity_main.xml
        setContentView(R.layout.sample_main)
        val rootView: RelativeLayout = findViewById(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top + supportActionBar!!.height
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }


        // Get handle to default shared preferences for this activity
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

        // Populate spinner with sample values from an array
        mSpinner = findViewById(R.id.spinner)
        mSpinner!!.adapter = ArrayAdapter(
            /* context = */ this,
            /* resource = */ android.R.layout.simple_list_item_1,
            /* objects = */ ArrayList(Arrays.asList(*SPINNER_VALUES))
        )

        // Read in a sample value, if it's not set.
        val selection: Int = mPrefs!!.getInt(
            /* key = */ PREF_SPINNER_POS,
            /* defValue = */ PREF_SPINNER_VALUE_ISNULL
        )
        if (selection != PREF_SPINNER_VALUE_ISNULL) {
            mSpinner!!.setSelection(selection)
        }

        // Callback to persist spinner data whenever a new value is selected. This will be the
        // focus of our sample unit test.
        mSpinner!!.onItemSelectedListener = object : OnItemSelectedListener {
            // The methods below commit the ID of the currently selected item in the spinner
            // to disk, using a SharedPreferences file.
            //
            // Note: A common mistake here is to forget to call .commit(). Try removing this
            // statement and running the tests to watch them fail.
            @SuppressLint("ApplySharedPref")
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                mPrefs!!.edit(commit = true) { putInt(PREF_SPINNER_POS, position) }
            }

            @SuppressLint("ApplySharedPref")
            override fun onNothingSelected(parent: AdapterView<*>?) {
                mPrefs!!.edit(commit = true) { remove(PREF_SPINNER_POS) }
            }
        }
    }

    companion object {
        /**
         * Shared preferences key: Holds spinner position. Must not be negative.
         */
        private const val PREF_SPINNER_POS = "spinner_pos"

        /**
         * Magic constant to indicate that no value is stored for PREF_SPINNER_POS.
         */
        private const val PREF_SPINNER_VALUE_ISNULL = -1

        /**
         * Values for display in spinner.
         */
        private val SPINNER_VALUES = arrayOf(
            "Select Weather...", "Sunny", "Partly Cloudy", "Cloudy", "Rain", "Snow", "Hurricane"
        )

        // Constants representing each of the options in SPINNER_VALUES. Declared package-private
        // so that they can be accessed from our test suite.

        /**
         * TODO: Add kdoc
         */
        const val WEATHER_NOSELECTION: Int = 0

        /**
         * TODO: Add kdoc
         */
        const val WEATHER_SUNNY: Int = 1

        /**
         * TODO: Add kdoc
         */
        const val WEATHER_PARTLY_CLOUDY: Int = 2

        /**
         * TODO: Add kdoc
         */
        const val WEATHER_CLOUDY: Int = 3

        /**
         * TODO: Add kdoc
         */
        const val WEATHER_RAIN: Int = 4

        /**
         * TODO: Add kdoc
         */
        const val WEATHER_SNOW: Int = 5

        /**
         * TODO: Add kdoc
         */
        const val WEATHER_HURRICANE: Int = 6
    }
}