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

package com.example.android.lunarlander;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.android.lunarlander.LunarView.LunarThread;

/**
 * This is a simple LunarLander activity that houses a single LunarView. It
 * demonstrates...
 * <ul>
 * <li>animating by calling invalidate() from draw()
 * <li>loading and drawing resources
 * <li>handling onPause() in an animation
 * </ul>
 */
public class LunarLander extends Activity {
    /**
     * Menu item id for selecting DIFFICULTY_EASY difficulty
     */
    private static final int MENU_EASY = 1;
    /**
     * Menu item id for selecting DIFFICULTY_HARD difficulty
     */
    private static final int MENU_HARD = 2;
    /**
     * Menu item id for selecting DIFFICULTY_MEDIUM difficulty
     */
    private static final int MENU_MEDIUM = 3;
    /**
     * Menu item id for pausing the game
     */
    private static final int MENU_PAUSE = 4;
    /**
     * Menu item id for resuming the game
     */
    private static final int MENU_RESUME = 5;
    /**
     * Menu item id for starting the game.
     */
    private static final int MENU_START = 6;
    /**
     * Menu item id for stopping the game
     */
    private static final int MENU_STOP = 7;

    /**
     * A handle to the thread that's actually running the animation.
     */
    private LunarThread mLunarThread;

    /**
     * A handle to the View in which the game is running.
     */
    private LunarView mLunarView;

    /**
     * Invoked during init to give the Activity a chance to set up its Menu. We add the following
     * {@code MenuItem} entries to our parameter {@code Menu menu}:
     * <ul>
     *     <li>
     *         MENU_START: "Start"
     *     </li>
     *     <li>
     *         MENU_STOP: "Stop"
     *     </li>
     *     <li>
     *         MENU_PAUSE: "Pause"
     *     </li>
     *     <li>
     *         MENU_RESUME: "Resume"
     *     </li>
     *     <li>
     *         MENU_EASY: "Easy"
     *     </li>
     *     <li>
     *         MENU_MEDIUM: "Medium"
     *     </li>
     *     <li>
     *         MENU_HARD: "Hard"
     *     </li>
     * </ul>
     * Then return true so that the menu will be displayed.
     *
     * @param menu the Menu to which entries may be added
     * @return true so that the menu will be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_START, 0, R.string.menu_start);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop);
        menu.add(0, MENU_PAUSE, 0, R.string.menu_pause);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);
        menu.add(0, MENU_EASY, 0, R.string.menu_easy);
        menu.add(0, MENU_MEDIUM, 0, R.string.menu_medium);
        menu.add(0, MENU_HARD, 0, R.string.menu_hard);

        return true;
    }

    /**
     * Invoked when the user selects an item from the Menu. We switch on the item id of our parameter
     * {@code MenuItem item}:
     * <ul>
     *     <li>
     *         MENU_START: "Start" we call the {@code doStart} method of our field {@code LunarThread mLunarThread}
     *         to start the game running, and return true to consume the event.
     *     </li>
     *     <li>
     *         MENU_STOP: "Stop" we call the {@code setState} method of our field {@code LunarThread mLunarThread}
     *         to set the game state to STATE_LOSE, and display the string "Stopped", then return true to consume
     *         the event.
     *     </li>
     *     <li>
     *         MENU_PAUSE: "Pause" we call the {@code pause} method of our field {@code LunarThread mLunarThread}
     *         to pause the game then return true to consume the event.
     *     </li>
     *     <li>
     *         MENU_RESUME: "Resume" we call the {@code unpause} method of our field {@code LunarThread mLunarThread}
     *         to unpause the game then return true to consume the event.
     *     </li>
     *     <li>
     *         MENU_EASY: "Easy" we call the {@code setDifficulty} method of our field {@code LunarThread mLunarThread}
     *         to set the difficulty to DIFFICULTY_EASY then return true to consume the event.
     *     </li>
     *     <li>
     *         MENU_MEDIUM: "Medium" we call the {@code setDifficulty} method of our field {@code LunarThread mLunarThread}
     *         to set the difficulty to DIFFICULTY_MEDIUM then return true to consume the event.
     *     </li>
     *     <li>
     *         MENU_HARD: "Hard" we call the {@code setDifficulty} method of our field {@code LunarThread mLunarThread}
     *         to set the difficulty to DIFFICULTY_HARD then return true to consume the event.
     *     </li>
     * </ul>
     * If the menu item id is not one we recognise we return false to allow normal menu processing to
     * proceed.
     *
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_START:
                mLunarThread.doStart();
                return true;
            case MENU_STOP:
                mLunarThread.setState(LunarThread.STATE_LOSE, getText(R.string.message_stopped));
                return true;
            case MENU_PAUSE:
                mLunarThread.pause();
                return true;
            case MENU_RESUME:
                mLunarThread.unpause();
                return true;
            case MENU_EASY:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_EASY);
                return true;
            case MENU_MEDIUM:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_MEDIUM);
                return true;
            case MENU_HARD:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_HARD);
                return true;
        }

        return false;
    }

    /**
     * Invoked when the Activity is created. First we call our super's implementation of {@code onCreate},
     * then we set our content view to our layout file R.layout.lunar_layout. We initialize our field
     * {@code LunarView mLunarView} by finding the view with id R.id.lunar, and initialize our field
     * {@code LunarThread mLunarThread} with the handle to the animation thread returned by the
     * {@code getThread} method of {@code mLunarView}. We then call the {@code setTextView} method of
     * {@code mLunarView} to have it set the {@code TextView} it uses for status messages to the view
     * we find with id the R.id.text. If our parameter {@code Bundle savedInstanceState} is null we
     * were just launched so we call the {@code setState} method of {@code mLunarThread} to have it
     * set the game state to STATE_READY. If {@code savedInstanceState} is not null we call the
     * {@code restoreState} method of {@code mLunarThread} to have it restore its state from the
     * data that has been saved in {@code savedInstanceState}. Finally we set the {@code OnClickListener}
     * of {@code mLunarView} to an anonymous class which simulates a keypad when the view is clicked.
     *
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.lunar_layout);

        // get handles to the LunarView from XML, and its LunarThread
        mLunarView = findViewById(R.id.lunar);
        mLunarThread = mLunarView.getThread();

        // give the LunarView a handle to the TextView used for messages
        mLunarView.setTextView((TextView) findViewById(R.id.text));

        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            mLunarThread.setState(LunarThread.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
            mLunarThread.restoreState(savedInstanceState);
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }

        mLunarView.setOnClickListener(new View.OnClickListener() {

            /**
             * Whether the rocket engine is firing or not.
             */
            private boolean firing = false;

            /**
             * Called when {@code LunarView mLunarView} is clicked. If {@code LunarThread mLunarThread}
             * is not null branch on the value of the {@code mMode} field of {@code mLunarThread}:
             * <ul>
             *     <li>
             *         not STATE_RUNNING: We call the {@code setDifficulty} method of {@code mLunarThread}
             *         to set the difficulty level to DIFFICULTY_EASY, then we call the {@code dispatchKeyEvent}
             *         method to fake an ACTION_DOWN action of the KEYCODE_DPAD_UP keycode {@code KeyEvent}
             *         followed by an ACTION_UP of the same key.
             *     </li>
             *     <li>
             *         STATE_RUNNING: We branch on the value of our field {@code boolean firing} faking an
             *         ACTION_DOWN action of the KEYCODE_DPAD_CENTER keycode if it is false (starting the
             *         rocket engine firing) and set {@code firing} to true, and if {@code firing} is already
             *         true we fake an ACTION_UP action of the KEYCODE_DPAD_CENTER keycode (stopping the
             *         rocket engine) then set {@code firing} to false.
             *     </li>
             * </ul>
             *
             * @param v The view that was clicked and held.
             */
            @Override
            public void onClick(View v) {
                if (mLunarThread != null) {
                    if (mLunarThread.mMode != LunarThread.STATE_RUNNING) {
                        mLunarThread.setDifficulty(LunarThread.DIFFICULTY_EASY);
                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP));
                        mLunarThread.doKeyDown(KeyEvent.KEYCODE_DPAD_UP, null);
                        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_UP));
                        mLunarThread.doKeyUp(KeyEvent.KEYCODE_DPAD_UP, null);
                    } else {
                        if (!firing) {
                            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER));
                            mLunarThread.doKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null);
                            firing = true;
                        } else {
                            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER));
                            mLunarThread.doKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, null);
                            firing = false;
                        }
                    }
                }
            }
        });
    }

    /**
     * Invoked when the Activity loses user focus. We fetch a handle to the animation thread of
     * {@code LunarView mLunarView} and call its {@code pause} method to pause the game, then we call
     * our super's implementation of {@code onPause}.
     */
    @Override
    protected void onPause() {
        mLunarView.getThread().pause(); // pause game when Activity pauses
        super.onPause();
    }

    /**
     * Notification that something is about to happen, to give the Activity a chance to save state.
     * First we call our super's implementation of {@code onSaveInstanceState}, then we call the
     * {@code saveState} method of {@code mLunarThread} to have it save the game state in our parameter
     * {@code Bundle outState}.
     *
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mLunarThread.saveState(outState);
        Log.w(this.getClass().getName(), "SIS called");
    }
}
