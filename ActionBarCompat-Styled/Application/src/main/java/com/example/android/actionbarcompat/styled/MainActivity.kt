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
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction

/**
 * This sample shows you how to use ActionBarCompat with a customized theme. It utilizes a split
 * action bar when running on a device with a narrow display, and shows three tabs.
 *
 *
 * This Activity extends from [AppCompatActivity], which provides all of the function
 * necessary to display a compatible Action Bar on devices running Android v2.1+.
 *
 *
 * The interesting bits of this sample start in the theme files
 * ('res/values/styles.xml' and 'res/values-v14').
 *
 *
 * Many of the drawables used in this sample were generated with the
 * 'Android Action Bar Style Generator': [...](http://jgilfelt.github.io/android-actionbarstylegenerator)
 */
class MainActivity : AppCompatActivity(), ActionBar.TabListener {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file R.layout.sample_main. We
     * initialize `ActionBar ab` with a reference to this activity's ActionBar, set its
     * navigation mode to NAVIGATION_MODE_TABS, then add three tabs to it with their `TabListener`
     * set to this.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_main)

        // Set the Action Bar to use tabs for navigation
        val ab = supportActionBar
        ab!!.navigationMode = ActionBar.NAVIGATION_MODE_TABS

        // Add three tabs to the Action Bar for display
        ab.addTab(ab.newTab().setText("Tab 1").setTabListener(this))
        ab.addTab(ab.newTab().setText("Tab 2").setTabListener(this))
        ab.addTab(ab.newTab().setText("Tab 3").setTabListener(this))
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We inflate our menu layout
     * file R.menu.main into our parameter `Menu menu` then return whatever our super's
     * implementation of `onCreateOptionsMenu` returns to our caller.
     *
     * @param menu The options menu in which you place your items.
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