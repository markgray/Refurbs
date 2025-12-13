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
import android.view.View.OnApplyWindowInsetsListener
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
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display then we call our super's implementation of `onCreate` and set our content view
     * to our layout file `R.layout.activity_main`.
     *
     * We initialize [RelativeLayout] variable `rootView`  to the [View] with resource id
     * `R.id.root_view` and then we use [ViewCompat.setOnApplyWindowInsetsListener] to set
     * an [OnApplyWindowInsetsListener] to take over the policy for applying window insets
     * to `rootView`, with the `listener` argument a lambda that accepts the [View] passed
     * the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars]
     * as the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize our [SharedPreferences] property [mPrefs] to the value returned by the
     * [PreferenceManager.getDefaultSharedPreferences] method for `this` [MainActivity] (handle to
     * default shared preferences for this activity). We initialize our [Spinner] property [mSpinner]
     * to the view with ID `R.id.spinner`, then call its [Spinner.setAdapter] method to set its
     * adapter to an [ArrayAdapter] whose `context` argument is `this` [MainActivity], whose
     * `resource` argument is the layout file with resource ID `android.R.layout.simple_list_item_1`,
     * and whose `objects` argument is an [ArrayList] built from the contents of our [Array] of
     * [String] property [SPINNER_VALUES]. We initialize our [Int] variable `selection` to the
     * [Int] retrieved from our shared preference file by the [SharedPreferences.getInt] method of
     * [SharedPreferences] property [mPrefs] for the `key` [PREF_SPINNER_POS] (defaulting to
     * [PREF_SPINNER_VALUE_ISNULL] if none is found). Then if `selection` is not
     * [PREF_SPINNER_VALUE_ISNULL] we call the [Spinner.setSelection] method of [Spinner] property
     * [mSpinner] to set its currently selected item to `selection`. Finally we call the
     * [Spinner.setOnItemSelectedListener] method of [mSpinner] to set its [OnItemSelectedListener]
     * to an anonymous class which will store the value selected in [SharedPreferences] property
     * [mPrefs].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Inflate UI from res/layout/activity_main.xml
        setContentView(R.layout.sample_main)
        val rootView: RelativeLayout = findViewById(R.id.root_view)
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

            /**
             * Callback method to be invoked when an item in this view has been selected. This
             * implementation saves the [position] of the selected item in our [SharedPreferences]
             * file [mPrefs] under the key [PREF_SPINNER_POS]. The `commit` parameter of the [edit]
             * function is `true` so the save is performed synchronously. We suppress the
             * "ApplySharedPref" warning because we want to perform a synchronous `commit` rather
             * than an asynchronous `apply` to prevent race conditions in the tests.
             *
             * @param parent The [AdapterView] where the selection happened.
             * @param view The [View] within the [AdapterView] that was clicked.
             * @param position The position of the view in the adapter.
             * @param id The row id of the item that is selected.
             */
            @SuppressLint("ApplySharedPref")
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                mPrefs!!.edit(commit = true) { putInt(PREF_SPINNER_POS, position) }
            }

            /**
             * Callback method to be invoked when the selection disappears from this
             * view. The selection can disappear for instance when touch is activated
             * or when the adapter becomes empty. We remove the value stored under the
             * key [PREF_SPINNER_POS] from our [SharedPreferences] file [mPrefs]. The
             * `commit` parameter of the [edit] function is `true` so the removal is
             * performed synchronously. We suppress the "ApplySharedPref" warning
             * because we want to perform a synchronous `commit` rather than an
             * asynchronous `apply` to prevent race conditions in the tests.
             *
             * @param parent The [AdapterView] that now contains no selected item.
             */
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
        private val SPINNER_VALUES: Array<String> = arrayOf(
            "Select Weather...", "Sunny", "Partly Cloudy", "Cloudy", "Rain", "Snow", "Hurricane"
        )

        // Constants representing each of the options in SPINNER_VALUES. Declared package-private
        // so that they can be accessed from our test suite.

        /**
         * Position of the [Spinner] when nothing is selected.
         */
        const val WEATHER_NOSELECTION: Int = 0

        /**
         * Position of the [Spinner] when the user selects "Sunny".
         */
        const val WEATHER_SUNNY: Int = 1

        /**
         * Position of the [Spinner] when the user selects "Partly Cloudy".
         */
        const val WEATHER_PARTLY_CLOUDY: Int = 2

        /**
         * Position of the [Spinner] when the user selects "Cloudy".
         */
        const val WEATHER_CLOUDY: Int = 3

        /**
         * Position of the [Spinner] when the user selects "Rain".
         */
        const val WEATHER_RAIN: Int = 4

        /**
         * Position of the [Spinner] when the user selects "Snow".
         */
        const val WEATHER_SNOW: Int = 5

        /**
         * Position of the [Spinner] when the user selects "Hurricane".
         */
        const val WEATHER_HURRICANE: Int = 6
    }
}