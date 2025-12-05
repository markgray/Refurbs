/*
 * Copyright (C) 2013 The Android Open Source Project
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
@file:Suppress("DEPRECATION", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.actionbarcompat.styled

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ContentFrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentTransaction

/**
 * This sample shows you how to use `ActionBarCompat` with a customized theme. It utilizes a split
 * action bar when running on a device with a narrow display, and shows three tabs.
 *
 * This Activity extends from [AppCompatActivity], which provides all of the function
 * necessary to display a compatible Action Bar on devices running Android v2.1+.
 *
 * The interesting bits of this sample start in the theme files
 * ('res/values/styles.xml' and 'res/values-v14').
 *
 * Many of the drawables used in this sample were generated with the
 * 'Android Action Bar Style Generator': [...](http://jgilfelt.github.io/android-actionbarstylegenerator)
 */
class MainActivity : AppCompatActivity(), ActionBar.TabListener {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.sample_main`.
     *
     * We initialize our [ContentFrameLayout] variable `rootView`
     * to the view with ID `android.R.id.content` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using `WindowInsetsCompat.Type.ime()`. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * Next we initialize our [ActionBar] variable `val ab` with a reference to this activity's
     * [ActionBar], set its navigation mode to [ActionBar.NAVIGATION_MODE_TABS], then add three
     * tabs to it with their [ActionBar.TabListener] set to `this`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_main)
        val rootView = window.decorView.findViewById<ContentFrameLayout>(android.R.id.content)
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

        // Set the Action Bar to use tabs for navigation
        val ab: ActionBar? = supportActionBar
        ab!!.navigationMode = ActionBar.NAVIGATION_MODE_TABS

        // Add three tabs to the Action Bar for display
        ab.addTab(ab.newTab().setText("Tab 1").setTabListener(this))
        ab.addTab(ab.newTab().setText("Tab 2").setTabListener(this))
        ab.addTab(ab.newTab().setText("Tab 3").setTabListener(this))
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We inflate our menu layout
     * file `R.menu.main` into our [Menu] parameter [menu] then return whatever our super's
     * implementation of `onCreateOptionsMenu` returns to our caller.
     *
     * @param menu The options [Menu] in which you place your items.
     * @return You must return true for the menu to be displayed; we return whatever our super's
     * implementation of `onCreateOptionsMenu` returns.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate menu from menu resource (res/menu/main)
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Called when a tab enters the selected state. We ignore.
     *
     * @param tab                 The tab that was selected
     * @param fragmentTransaction A [FragmentTransaction] for queuing fragment operations to
     * execute during a tab switch.
     */
    override fun onTabSelected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) {
        // This is called when a tab is selected.
    }

    /**
     * Called when a tab exits the selected state. We ignore.
     *
     * @param tab                 The tab that was unselected
     * @param fragmentTransaction A [FragmentTransaction] for queuing fragment operations to
     * execute during a tab switch.
     */
    override fun onTabUnselected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) {
        // This is called when a previously selected tab is unselected.
    }

    /**
     * Called when a tab that is already selected is chosen again by the user. We ignore.
     *
     * @param tab                 The tab that was reselected.
     * @param fragmentTransaction A [FragmentTransaction] for queuing fragment operations to
     * execute once this method returns.
     */
    override fun onTabReselected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) {
        // This is called when a previously selected tab is selected again.
    }
}
