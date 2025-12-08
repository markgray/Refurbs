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
package com.example.android.snake

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * Snake: a simple game that everyone can enjoy.
 *
 * This is an implementation of the classic Game "Snake", in which you control a serpent roaming
 * around the garden looking for apples. Be careful, though, because when you catch one, not only
 * will you become longer, but you'll move faster. Running into yourself or the walls will end the
 * game.
 */
class Snake : ComponentActivity() {
    /**
     * Reference to the [SnakeView] in our layout file.
     */
    private lateinit var mSnakeView: SnakeView

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable
     * edge to edge display, then we call our super's implementation of `onCreate`, and
     * set our content view to our layout file `R.layout.snake_layout`.
     *
     * We initialize our [FrameLayout] variable `rootView` to the view with ID `R.id.root_view`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for
     * applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize our [SnakeView] field  [mSnakeView] by finding the view with id `R.id.snake`,
     * then call its method [SnakeView.setDependentViews] to have it set its [TextView] field
     * [SnakeView.mStatusText] to the view with id `R.id.text`, its [View] field
     * [SnakeView.mArrowsView] to the view with id `R.id.arrowContainer`, and its [View] field
     * [SnakeView.mBackgroundView] to the view with id `R.id.background`. If our [Bundle] parameter
     * [savedInstanceState] is `null` we were just launched so we call the [SnakeView.setMode]
     * method of our field [mSnakeView] to have it set its mode to `READY`. If it is not `null` we
     * are being restored after a configuration change so we initialize [Bundle] variable `val map`
     * with the bundle stored in [savedInstanceState] under the key [ICICLE_KEY], and if that is not
     * `null` we call the [SnakeView.restoreState] method of [mSnakeView] to have it restore its
     * state from `map`, and if it is `null` we call the [SnakeView.setMode] method of [mSnakeView]
     * to have it set its mode to `PAUSE`. Finally we set the [View.OnTouchListener] of [mSnakeView]
     * to an anonymous class whose `onTouch` override changes the direction the snake is moving
     * based on the location of the touch if the game state of [mSnakeView] is `RUNNING`, or starts
     * the game running if it was not running by having it move the snake in the `MOVE_UP` direction.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     * down then this Bundle contains the data it most recently supplied in [onSaveInstanceState].
     */
    @SuppressLint("ClickableViewAccessibility") // I doubt the blind can play this game (but I may be wrong).
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.snake_layout)
        val rootView = findViewById<FrameLayout>(R.id.root_view)
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
        mSnakeView = findViewById(R.id.snake)
        mSnakeView.setDependentViews(
                findViewById(R.id.text),  // view to use for its field {@code TextView mStatusText}
                findViewById(R.id.arrowContainer),  // view to use for its field {@code View mArrowsView}
                findViewById(R.id.background) // view to use for its field {@code View mBackgroundView}.
        )
        if (savedInstanceState == null) {
            // We were just launched -- set up a new game
            mSnakeView.setMode(SnakeView.READY)
        } else {
            // We are being restored
            val map = savedInstanceState.getBundle(ICICLE_KEY)
            if (map != null) {
                mSnakeView.restoreState(map)
            } else {
                mSnakeView.setMode(SnakeView.PAUSE)
            }
        }
        mSnakeView.setOnTouchListener { v: View, event: MotionEvent ->

            /**
             * Called when a touch event is dispatched to a view. If the game state of our [SnakeView]
             * field [mSnakeView] is `RUNNING`, we initialize our [Float] variable `val x` to the X
             * location of the [MotionEvent] parameter `event` divided by the width of the [View]
             * parameter `v` and [Float] variable `val y` to the Y location of the of the [MotionEvent]
             * parameter `event` divided by the height of the [View] parameter `v`. Then we declare
             * [Int] variable `var direction` and set it to the quadrant of the touch from [0,1,2,3]
             * by setting the lower bit if `x` is greater than `y` and setting the upper bit if `x`
             * is greater than 1 minus `y`. We then call the [SnakeView.moveSnake] method of [mSnakeView]
             * to set the direction it is moving in to `direction` (ie. the direction is same as the
             * quadrant which was touched). If the game state of [mSnakeView] was not already `RUNNING`,
             * we start it running by calling its [SnakeView.moveSnake] method to have it start moving
             * in the `MOVE_UP` direction.
             *
             * @param v The [View] the touch event has been dispatched to.
             * @param event The [MotionEvent] object containing full information about the event.
             * @return `true` if the listener has consumed the event, `false` otherwise.
             */
            if (mSnakeView.gameState == SnakeView.RUNNING) {
                // Normalize x,y between 0 and 1
                val x = event.x / v.width
                val y = event.y / v.height

                // Direction will be [0,1,2,3] depending on quadrant
                var direction: Int = if (x > y) 1 else 0
                direction = direction or if (x > 1 - y) 2 else 0

                // Direction is same as the quadrant which was clicked
                mSnakeView.moveSnake(direction)
            } else {
                // If the game is not running then on touching any part of the screen
                // we start the game by sending MOVE_UP signal to SnakeView
                mSnakeView.moveSnake(MOVE_UP)
            }
            false
        }
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into the background, but
     * has not (yet) been killed. First we call our super's implementation of `onPause`, then
     * we call the [SnakeView.setMode] method of our field [mSnakeView] to have it set its game mode
     * to `PAUSE`.
     */
    override fun onPause() {
        super.onPause()
        // Pause the game along with the activity
        mSnakeView.setMode(SnakeView.PAUSE)
    }

    /**
     * Called to retrieve per-instance state from an activity before being killed so that the state
     * can be restored in [onCreate] or [onRestoreInstanceState] (the [Bundle] populated by this
     * method will be passed to both). We just store the [Bundle] returned by the [SnakeView.saveState]
     * method of our field [mSnakeView] in our [Bundle] parameter [outState] under the key [ICICLE_KEY].
     *
     * @param outState [Bundle] in which to place your saved state.
     */
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Store the game state
        outState.putBundle(ICICLE_KEY, mSnakeView.saveState())
    }

    /**
     * Called when a key was pressed down and not handled by any of the views inside of the activity.
     * Handles key events in the game by updating the direction our snake is traveling based on the
     * `DPAD` key pressed. We switch on the value of our [Int] parameter [keyCode]:
     *  * [KeyEvent.KEYCODE_DPAD_UP]: we call the [SnakeView.moveSnake] method of our field
     *  [mSnakeView] to have it start to move in the [MOVE_UP] direction then break.
     *
     *  * [KeyEvent.KEYCODE_DPAD_RIGHT]: we call the [SnakeView.moveSnake] method of our field
     *  [mSnakeView] to have it start to move in the [MOVE_RIGHT] direction then break.
     *
     *  * [KeyEvent.KEYCODE_DPAD_DOWN]: we call the [SnakeView.moveSnake] method of our field
     *  [mSnakeView] to have it start to move in the [MOVE_DOWN] direction then break.
     *
     *  * [KeyEvent.KEYCODE_DPAD_LEFT]: we call the [SnakeView.moveSnake] method of our field
     *  [mSnakeView] to have it start to move in the  [MOVE_LEFT] direction then break.
     *
     * Finally we return the value returned by our super's implementation of `onKeyDown` to the caller.
     *
     * @param keyCode The key code of the key event received.
     * @param msg     The [KeyEvent] received.
     * @return Return `true` to consume the event here, or `false` to let it propagate.
     */
    override fun onKeyDown(keyCode: Int, msg: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> mSnakeView.moveSnake(MOVE_UP)
            KeyEvent.KEYCODE_DPAD_RIGHT -> mSnakeView.moveSnake(MOVE_RIGHT)
            KeyEvent.KEYCODE_DPAD_DOWN -> mSnakeView.moveSnake(MOVE_DOWN)
            KeyEvent.KEYCODE_DPAD_LEFT -> mSnakeView.moveSnake(MOVE_LEFT)
        }
        return super.onKeyDown(keyCode, msg)
    }

    companion object {
        // Constants for desired direction of moving the snake
        /**
         * Turn the snake to move left
         */
        @JvmField
        var MOVE_LEFT: Int = 0

        /**
         * Turn the snake to move up
         */
        @JvmField
        var MOVE_UP: Int = 1

        /**
         * Turn the snake to move down
         */
        @JvmField
        var MOVE_DOWN: Int = 2

        /**
         * Turn the snake to move right
         */
        @JvmField
        var MOVE_RIGHT: Int = 3

        /**
         * Key under which we store the `Bundle` containing the state of the game in the `Bundle`
         * that we are passed in our `onSaveInstanceState` override and later restore from the
         * `Bundle` passed our `onCreate` override when we are recreated.
         */
        private const val ICICLE_KEY = "snake-view"
    }
}
