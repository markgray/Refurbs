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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.basicgesturedetect

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.android.common.logger.LogFragment
import com.example.android.common.logger.LogView

/**
 * This sample detects gestures on a view and logs them. In order to try this sample out, try
 * dragging or tapping the text.
 *
 * In this sample, the gestures are detected using a custom gesture listener that extends
 * [SimpleOnGestureListener] and writes the detected [MotionEvent] into the log.
 *
 * In this example, the steps followed to set up the gesture detector are:
 *  1. Create the [GestureListener] that includes all your callbacks.
 *
 *  2. Create the [GestureDetector] that will take the [GestureListener] as an argument.
 *
 *  3. For the view where the gestures will occur, create an [View.OnTouchListener] that sends
 *  all motion events to the gesture detector.
 */
class BasicGestureDetectFragment : Fragment() {
    /**
     * Called to do initial creation of a fragment. First we call our super's implementation of
     * `onCreate`, then we call the [setHasOptionsMenu] method with `true` to report that this
     * fragment would like to participate in populating the options menu by receiving a call to
     * [onCreateOptionsMenu] and related methods.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") // TODO: Use MenuProvider
        setHasOptionsMenu(true)
    }

    /**
     * Called when the fragment's activity has been created and this fragment's view hierarchy
     * instantiated. It can be used to do final initialization once these pieces are in place,
     * such as retrieving views or restoring state.  It is also useful for fragments that use
     * [setRetainInstance] to retain their instance, as this callback tells the fragment when
     * it is fully associated with the new activity instance. This is called after [onCreateView]
     * and before [onViewStateRestored].
     *
     * First we call our super's implementation of `onActivityCreated`, then we initialize [View]
     * variable `val gestureView` by using the [FragmentActivity] this fragment is currently
     * associated with to find the view with id `R.id.sample_output`, set it to be clickable, and
     * set it to be focusable. We initialize our [GestureDetector.SimpleOnGestureListener] variable
     * `val gestureListener` with a new instance of our custom [GestureListener] class. We initialize
     * our [GestureDetector] variable `val gd` with a new instance which will use `gestureListener`
     * as its [GestureDetector.OnGestureListener]. Finally we set the [View.OnTouchListener] of
     * `gestureView` to an anonymous class whose `onTouch` override calls the `onTouchEvent` method
     * of [GestureDetector] `gd`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    @Deprecated("Deprecated in Java") // Only place we can add a GestureListener
    @SuppressLint("ClickableViewAccessibility") // I doubt the blind will use this
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION") // Only place we can add a GestureListener
        super.onActivityCreated(savedInstanceState)
        val gestureView: View = requireActivity().findViewById(R.id.sample_output)
        gestureView.isClickable = true
        gestureView.isFocusable = true

        // First create the GestureListener that will include all our callbacks.
        // Then create the GestureDetector, which takes that listener as an argument.
        val gestureListener: SimpleOnGestureListener = GestureListener()
        val gd = GestureDetector(activity, gestureListener)

        /**
         * For the view where gestures will occur, create an [View.OnTouchListener] that sends
         * all motion events to the gesture detector.  When the gesture detector actually detects
         * an event, it will use the callbacks you created in the [SimpleOnGestureListener] to
         * alert your application.
         */
        @Suppress("Unused")
        gestureView.setOnTouchListener { view: View, motionEvent: MotionEvent ->

            /**
             * Called when a touch event is dispatched to our [View] parameter [view]. This
             * allows listeners to get a chance to respond before the target [view]. We just
             * call the `onTouchEvent` method of [GestureDetector] variable `gd` with our [MotionEvent]
             * parameter [motionEvent] and return `false` so that the [view] will also receive
             * the touch event.
             *
             * @param view The [View] the touch event has been dispatched to.
             * @param motionEvent The [MotionEvent] object containing full information about the event.
             * @return `true` if the listener has consumed the event, `false` otherwise.
             */
            gd.onTouchEvent(motionEvent)
            false
        }
    }

    /**
     * This hook is called whenever an item in our options menu is selected. If the id of our
     * [MenuItem] parameter [item] is `R.id.sample_action` we call our method [clearLog], in any
     * case we return `true` to consume the event here.
     *
     * @param item The [MenuItem] that was selected.
     * @return we return `true` to consume the event here.
     */
    @Deprecated("Deprecated in Java") // TODO: Reolace with MenuProvider
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sample_action) {
            clearLog()
        }
        return true
    }

    /**
     * Clears all the logging text displayed by our [LogFragment]. We initialize [LogFragment]
     * variable `val logFragment` by using the [FragmentActivity] this fragment is currently
     * associated with to find the view with id `R.id.log_fragment`, then fetch the [LogView]
     * of `logFragment` and call its [LogView.setText] method to set its text to the empty string.
     */
    fun clearLog() {
        val logFragment = requireActivity().supportFragmentManager
            .findFragmentById(R.id.log_fragment) as LogFragment?
        logFragment!!.logView!!.text = ""
    }
}
