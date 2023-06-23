/*
 * Copyright (C) 2014 The Android Open Source Project
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
@file:Suppress("MoveVariableDeclarationIntoWhen")

package com.example.android.elevationbasic

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log

/**
 * This [Fragment] does all the work of the demo, demonstrating two ways to
 * move a view in the z-axis.
 */
class ElevationBasicFragment : Fragment() {
    /**
     * Called to do initial creation of a fragment. First we call our super's implementation of
     * `onCreate`, then we call the `setHasOptionsMenu(true)` method to report that this
     * fragment would like to participate in populating the options menu by receiving a call to
     * [.onCreateOptionsMenu] and related methods.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * Called to have the fragment instantiate its user interface view. We initialize `View rootView`
     * with the view that our parameter `LayoutInflater inflater` inflates from our layout file
     * R.layout.elevation_basic using our parameter `ViewGroup container` for LayoutParams without
     * attaching to that view. We then initialize `View shape2` by finding the view in `rootView`
     * with id R.id.floating_shape_2. We set the `OnTouchListener` of `shape2` to an anonymous
     * class whose `onTouch` override moves the `View` the depth location of this view
     * relative to its `getElevation()` elevation by 120 on an ACTION_DOWN `MotionEvent`,
     * or back to 0 on an ACTION_UP `MotionEvent`.
     *
     *
     * Finally we return `rootView` to the caller.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        /*
         * Inflates an XML containing two shapes: the first has a fixed elevation
         * and the second ones raises when tapped.
         */
        val rootView = inflater.inflate(R.layout.elevation_basic, container, false)
        val shape2 = rootView.findViewById<View>(R.id.floating_shape_2)

        /*
         * Sets a {@Link View.OnTouchListener} that responds to a touch event on shape2.
         */
        shape2.setOnTouchListener(OnTouchListener { view, motionEvent ->

            /**
             * Called when a touch event is dispatched to a view. This allows listeners to
             * get a chance to respond before the target view. We initialize `int action`
             * with the masked action being performed of our parameter `MotionEvent motionEvent`
             * (pointer index information masked omitted), then switch on `action`:
             *
             *  *
             * ACTION_DOWN: We log the action, then set the depth location of our parameter
             * `View view` relative to its `getElevation()` to 120 (raises it),
             * then break.
             *
             *  *
             * ACTION_UP: We log the action, then set the depth location of our parameter
             * `View view` relative to its `getElevation()` to 0 (lowers it),
             * then break.
             *
             *  *
             * default: we return false to the caller.
             *
             *
             * We return true to the caller, consuming the event here.
             *
             * param view        The view the touch event has been dispatched to.
             * param motionEvent The MotionEvent object containing full information about
             * the event.
             * return True if the listener has consumed the event, false otherwise.
             */
            val action = motionEvent.actionMasked
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "ACTION_DOWN on view.")
                    view.translationZ = 120f
                }

                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "ACTION_UP on view.")
                    view.translationZ = 0f
                }

                else -> return@OnTouchListener false
            }
            true
        })
        return rootView
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "ElevationBasicFragment"
    }
}