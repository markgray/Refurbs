/*
 * Copyright (C) 2007 The Android Open Source Project
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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.lunarlander

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.example.android.lunarlander.LunarView.LunarThread

/**
 * This is a simple LunarLander activity that houses a single LunarView. It
 * demonstrates...
 *
 *  * animating by calling invalidate() from draw()
 *  * loading and drawing resources
 *  * handling onPause() in an animation
 *
 */
class LunarLander : Activity() {
    /**
     * A handle to the thread that's actually running the animation.
     */
    private var mLunarThread: LunarThread? = null

    /**
     * A handle to the View in which the game is running.
     */
    private var mLunarView: LunarView? = null

    /**
     * Invoked during init to give the Activity a chance to set up its Menu. We add the following
     * `MenuItem` entries to our parameter `Menu menu`:
     *
     *  *
     * MENU_START: "Start"
     *
     *  *
     * MENU_STOP: "Stop"
     *
     *  *
     * MENU_PAUSE: "Pause"
     *
     *  *
     * MENU_RESUME: "Resume"
     *
     *  *
     * MENU_EASY: "Easy"
     *
     *  *
     * MENU_MEDIUM: "Medium"
     *
     *  *
     * MENU_HARD: "Hard"
     *
     *
     * Then return true so that the menu will be displayed.
     *
     * @param menu the Menu to which entries may be added
     * @return true so that the menu will be displayed.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, MENU_START, 0, R.string.menu_start)
        menu.add(0, MENU_STOP, 0, R.string.menu_stop)
        menu.add(0, MENU_PAUSE, 0, R.string.menu_pause)
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume)
        menu.add(0, MENU_EASY, 0, R.string.menu_easy)
        menu.add(0, MENU_MEDIUM, 0, R.string.menu_medium)
        menu.add(0, MENU_HARD, 0, R.string.menu_hard)
        return true
    }

    /**
     * Invoked when the user selects an item from the Menu. We switch on the item id of our parameter
     * `MenuItem item`:
     *
     *  *
     * MENU_START: "Start" we call the `doStart` method of our field `LunarThread mLunarThread`
     * to start the game running, and return true to consume the event.
     *
     *  *
     * MENU_STOP: "Stop" we call the `setState` method of our field `LunarThread mLunarThread`
     * to set the game state to STATE_LOSE, and display the string "Stopped", then return true to consume
     * the event.
     *
     *  *
     * MENU_PAUSE: "Pause" we call the `pause` method of our field `LunarThread mLunarThread`
     * to pause the game then return true to consume the event.
     *
     *  *
     * MENU_RESUME: "Resume" we call the `unpause` method of our field `LunarThread mLunarThread`
     * to unpause the game then return true to consume the event.
     *
     *  *
     * MENU_EASY: "Easy" we call the `setDifficulty` method of our field `LunarThread mLunarThread`
     * to set the difficulty to DIFFICULTY_EASY then return true to consume the event.
     *
     *  *
     * MENU_MEDIUM: "Medium" we call the `setDifficulty` method of our field `LunarThread mLunarThread`
     * to set the difficulty to DIFFICULTY_MEDIUM then return true to consume the event.
     *
     *  *
     * MENU_HARD: "Hard" we call the `setDifficulty` method of our field `LunarThread mLunarThread`
     * to set the difficulty to DIFFICULTY_HARD then return true to consume the event.
     *
     *
     * If the menu item id is not one we recognise we return false to allow normal menu processing to
     * proceed.
     *
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false otherwise
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_START -> {
                mLunarThread!!.doStart()
                return true
            }

            MENU_STOP -> {
                mLunarThread!!.setState(LunarView.STATE_LOSE, getText(R.string.message_stopped))
                return true
            }

            MENU_PAUSE -> {
                mLunarThread!!.pause()
                return true
            }

            MENU_RESUME -> {
                mLunarThread!!.unpause()
                return true
            }

            MENU_EASY -> {
                mLunarThread!!.setDifficulty(LunarView.DIFFICULTY_EASY)
                return true
            }

            MENU_MEDIUM -> {
                mLunarThread!!.setDifficulty(LunarView.DIFFICULTY_MEDIUM)
                return true
            }

            MENU_HARD -> {
                mLunarThread!!.setDifficulty(LunarView.DIFFICULTY_HARD)
                return true
            }
        }
        return false
    }

    /**
     * Invoked when the Activity is created. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file R.layout.lunar_layout. We initialize our field
     * `LunarView mLunarView` by finding the view with id R.id.lunar, and initialize our field
     * `LunarThread mLunarThread` with the handle to the animation thread returned by the
     * `getThread` method of `mLunarView`. We then call the `setTextView` method of
     * `mLunarView` to have it set the `TextView` it uses for status messages to the view
     * we find with id the R.id.text. If our parameter `Bundle savedInstanceState` is null we
     * were just launched so we call the `setState` method of `mLunarThread` to have it
     * set the game state to STATE_READY. If `savedInstanceState` is not null we call the
     * `restoreState` method of `mLunarThread` to have it restore its state from the
     * data that has been saved in `savedInstanceState`. Finally we set the `OnClickListener`
     * of `mLunarView` to an anonymous class which simulates a keypad when the view is clicked.
     *
     * @param savedInstanceState a Bundle containing state saved from a previous
     * execution, or null if this is a new execution
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.lunar_layout)

        // get handles to the LunarView from XML, and its LunarThread
        mLunarView = findViewById(R.id.lunar)
        mLunarThread = mLunarView!!.thread

        // give the LunarView a handle to the TextView used for messages
        mLunarView!!.setTextView(findViewById<View>(R.id.text) as TextView)
        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            mLunarThread!!.setState(LunarView.STATE_READY)
            Log.w(this.javaClass.name, "SIS is null")
        } else {
            // we are being restored: resume a previous game
            mLunarThread!!.restoreState(savedInstanceState)
            Log.w(this.javaClass.name, "SIS is nonnull")
        }
        mLunarView!!.setOnClickListener(object : View.OnClickListener {
            /**
             * Whether the rocket engine is firing or not.
             */
            private var firing = false

            /**
             * Called when `LunarView mLunarView` is clicked. If `LunarThread mLunarThread`
             * is not null branch on the value of the `mMode` field of `mLunarThread`:
             *
             *  *
             * not STATE_RUNNING: We call the `setDifficulty` method of `mLunarThread`
             * to set the difficulty level to DIFFICULTY_EASY, then we call the `dispatchKeyEvent`
             * method to fake an ACTION_DOWN action of the KEYCODE_DPAD_UP keycode `KeyEvent`
             * followed by an ACTION_UP of the same key.
             *
             *  *
             * STATE_RUNNING: We branch on the value of our field `boolean firing` faking an
             * ACTION_DOWN action of the KEYCODE_DPAD_CENTER keycode if it is false (starting the
             * rocket engine firing) and set `firing` to true, and if `firing` is already
             * true we fake an ACTION_UP action of the KEYCODE_DPAD_CENTER keycode (stopping the
             * rocket engine) then set `firing` to false.
             *
             *
             *
             * @param v The view that was clicked and held.
             */
            override fun onClick(v: View) {
                if (mLunarThread != null) {
                    if (mLunarThread!!.mMode != LunarView.STATE_RUNNING) {
                        mLunarThread!!.setDifficulty(LunarView.DIFFICULTY_EASY)
                        dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
                        mLunarThread!!.doKeyDown(KeyEvent.KEYCODE_DPAD_UP, null)
                        dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP))
                        mLunarThread!!.doKeyUp(KeyEvent.KEYCODE_DPAD_UP, null)
                    } else {
                        firing = if (!firing) {
                            dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
                            mLunarThread!!.doKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null)
                            true
                        } else {
                            dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
                            mLunarThread!!.doKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, null)
                            false
                        }
                    }
                }
            }
        })
    }

    /**
     * Invoked when the Activity loses user focus. We fetch a handle to the animation thread of
     * `LunarView mLunarView` and call its `pause` method to pause the game, then we call
     * our super's implementation of `onPause`.
     */
    override fun onPause() {
        mLunarView!!.thread.pause() // pause game when Activity pauses
        super.onPause()
    }

    /**
     * Notification that something is about to happen, to give the Activity a chance to save state.
     * First we call our super's implementation of `onSaveInstanceState`, then we call the
     * `saveState` method of `mLunarThread` to have it save the game state in our parameter
     * `Bundle outState`.
     *
     * @param outState a Bundle into which this Activity should save its state
     */
    override fun onSaveInstanceState(outState: Bundle) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState)
        mLunarThread!!.saveState(outState)
        Log.w(this.javaClass.name, "SIS called")
    }

    companion object {
        /**
         * Menu item id for selecting DIFFICULTY_EASY difficulty
         */
        private const val MENU_EASY = 1

        /**
         * Menu item id for selecting DIFFICULTY_HARD difficulty
         */
        private const val MENU_HARD = 2

        /**
         * Menu item id for selecting DIFFICULTY_MEDIUM difficulty
         */
        private const val MENU_MEDIUM = 3

        /**
         * Menu item id for pausing the game
         */
        private const val MENU_PAUSE = 4

        /**
         * Menu item id for resuming the game
         */
        private const val MENU_RESUME = 5

        /**
         * Menu item id for starting the game.
         */
        private const val MENU_START = 6

        /**
         * Menu item id for stopping the game
         */
        private const val MENU_STOP = 7
    }
}