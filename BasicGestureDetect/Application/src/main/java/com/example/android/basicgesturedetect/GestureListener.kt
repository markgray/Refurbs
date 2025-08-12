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

package com.example.android.basicgesturedetect

import android.os.Build
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import com.example.android.basicgesturedetect.GestureListener.Companion.getButtonsPressed
import com.example.android.basicgesturedetect.GestureListener.Companion.getTouchType
import com.example.android.common.logger.Log

/**
 * A [SimpleOnGestureListener] which just logs a message describing each gesture it receives
 * without consuming the events so that the underlying view will still receive it.
 */
class GestureListener : SimpleOnGestureListener() {
    /**
     * Notified when a tap occurs with the up [MotionEvent] that triggered it. We log the message
     * formed by concatenating the string "Single Tap Up" with the touch type string that our method
     * [getTouchType] generates for our [MotionEvent] parameter [e]. We then return `false` to allow
     * the event to propagate to the underlying view.
     *
     * @param e The up motion event that completed the first tap
     * @return we return false to allow the event to propagate to the underlying view.
     */
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        // Up motion completing a single tap occurred.
        Log.i(TAG, "Single Tap Up" + getTouchType(e))
        return false
    }

    /**
     * Notified when a long press occurs with the initial on down [MotionEvent] that triggered
     * it. We log the message formed by concatenating the string "Long Press" with the touch type
     * string that our method [getTouchType] generates for our [MotionEvent] parameter [e].
     *
     * @param e The initial on down motion event that started the long press.
     */
    override fun onLongPress(e: MotionEvent) {
        // Touch has been long enough to indicate a long press.
        // Does not indicate motion is complete yet (no up event necessarily)
        Log.i(TAG, "Long Press" + getTouchType(e))
    }

    /**
     * Notified when a scroll occurs with the initial on down [MotionEvent] and the current move
     * [MotionEvent]. The distance in x and y since the last call to [onScroll] is also supplied
     * for convenience. We log the message formed by concatenating the string "Scroll" with the
     * touch type string that our method [getTouchType] generates for our [MotionEvent] parameter
     * [e1]. We then return `false` to allow the event to propagate to the underlying [View].
     *
     * @param e1 The first down motion event that started the scrolling.
     * @param e2 The move motion event that triggered the current call to [onScroll].
     * @param distanceX The distance along the X axis that has been scrolled since the last call to
     * [onScroll]. This is NOT the distance between [e1] and [e2].
     * @param distanceY The distance along the Y axis that has been scrolled since the last
     * call to onScroll. This is NOT the distance between [e1] and [e2].
     * @return `true` if the event is consumed, else `false`. We return `false` to allow the event
     * to propagate to the underlying [View].
     */
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float)
    : Boolean {
        // User attempted to scroll
        Log.i(TAG, "Scroll" + getTouchType(e1!!))
        return false
    }

    /**
     * Notified of a fling event when it occurs with the initial on down [MotionEvent] and the
     * matching up [MotionEvent]. The calculated velocity along the x and y axis is supplied
     * in pixels per second. We log the message formed by concatenating the string "Fling" with the
     * touch type string that our method [getTouchType] generates for our [MotionEvent] parameter
     * [e1]. We then return `false` to allow the event to propagate to the underlying [View].
     *
     * @param e1 The first down motion event that started the fling.
     * @param e2 The move motion event that triggered the current call to [onFling].
     * @param velocityX The velocity of this fling measured in pixels per second along the x axis.
     * @param velocityY The velocity of this fling measured in pixels per second along the y axis.
     * @return `true` if the event is consumed, else `false`. We return `false` to allow the event
     * to propagate to the underlying [View].
     */
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        // Fling event occurred.  Notification of this one happens after an "up" event.
        Log.i(TAG, "Fling" + getTouchType(e1!!))
        return false
    }

    /**
     * The user has performed a down [MotionEvent] and not performed a move or up yet. This event is
     * commonly used to provide visual feedback to the user to let them know that their action has
     * been recognized i.e. highlight an element. We log the message formed by concatenating the
     * string "Show Press" with the touch type string that our method [getTouchType] generates for
     * our [MotionEvent] parameter [e].
     *
     * @param e The down motion event
     */
    override fun onShowPress(e: MotionEvent) {
        // User performed a down event, and hasn't moved yet.
        Log.i(TAG, "Show Press" + getTouchType(e))
    }

    /**
     * Notified when a tap occurs with the down [MotionEvent] that triggered it. This will be
     * triggered immediately for every down event. All other events should be preceded by this. We
     * log the message formed by concatenating the string "Down" with the touch type string that our
     * method [getTouchType] generates for our [MotionEvent] parameter [e]. We then return `false`
     * to allow the event to propagate to the underlying [View].
     *
     * @param e The down motion event.
     * @return `true` if the event is consumed, else `false`. We return `false` to allow the
     * event to propagate to the underlying [View].
     */
    override fun onDown(e: MotionEvent): Boolean {
        // "Down" event - User touched the screen.
        Log.i(TAG, "Down" + getTouchType(e))
        return false
    }

    /**
     * Notified when a double-tap occurs. We log the message formed by concatenating the string
     * "Double tap" with the touch type string that our method [getTouchType] generates for our
     * [MotionEvent] parameter [e]. We then return `false` to allow the event to propagate to the
     * underlying [View].
     *
     * @param e The down motion event of the first tap of the double-tap.
     * @return `true` if the event is consumed, else `false`. We return `false` to allow the event
     * to propagate to the underlying [View].
     */
    override fun onDoubleTap(e: MotionEvent): Boolean {
        // User tapped the screen twice.
        Log.i(TAG, "Double tap" + getTouchType(e))
        return false
    }

    /**
     * Notified when an event within a double-tap gesture occurs, including the down, move, and up
     * events. We log the message formed by concatenating the string "Event within double tap" with
     * the touch type string that our method [getTouchType] generates for our [MotionEvent] parameter
     * [e]. We then return `false` to allow the event to propagate to the underlying [View].
     *
     * @param e The motion event that occurred during the double-tap gesture.
     * @return `true` if the event is consumed, else `false`. We return `false` to allow the event
     * to propagate to the underlying [View].
     */
    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        // Since double-tap is actually several events which are considered one aggregate
        // gesture, there's a separate callback for an individual event within the double tap
        // occurring.  This occurs for down, up, and move.
        Log.i(TAG, "Event within double tap" + getTouchType(e))
        return false
    }

    /**
     * Notified when a single-tap occurs. Unlike [onSingleTapUp], this will only be called after the
     * detector is confident that the user's first tap is not followed by a second tap leading to a
     * double-tap gesture. We log the message formed by concatenating the string "Single tap confirmed"
     * with  the touch type string that our method [getTouchType] generates for our [MotionEvent]
     * parameter [e]. We then return `false` to allow the event to propagate to the underlying [View].
     *
     * @param e The down motion event of the single-tap.
     * @return `true` if the event is consumed, else `false`. We return `false` to allow the event
     * to propagate to the underlying [View].
     */
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        // A confirmed single-tap event has occurred.  Only called when the detector has
        // determined that the first tap stands alone, and is not part of a double tap.
        Log.i(TAG, "Single tap confirmed" + getTouchType(e))
        return false
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val TAG: String = "GestureListener"

        /**
         * Returns a human-readable string describing the type of touch that triggered a [MotionEvent].
         * We initialize our [String] variable `var touchTypeDescription` to the string " ", and
         * initialize our [Int] variable `val touchType` with the tool type of pointer index 0 (this
         * can be TOOL_TYPE_UNKNOWN, TOOL_TYPE_FINGER, TOOL_TYPE_STYLUS, TOOL_TYPE_ERASER or
         * TOOL_TYPE_MOUSE). We then switch on `touchType`:
         *  * `TOOL_TYPE_FINGER`: we append the string "(finger)" to `touchTypeDescription`.
         *
         *  * `TOOL_TYPE_STYLUS`: we append the string "(stylus, " to `touchTypeDescription`. We
         *  then initialize [Float] variable `val stylusPressure` with the current pressure of [e]
         *  row its first pointer index, and then append the string formed by concatenating the
         *  string "pressure: " and the string value of `stylusPressure` to `touchTypeDescription`.
         *  If we are running on a device whose SDK is greater than or equal to 21 we append the
         *  string formed by concatenating the string ", buttons pressed: " to the list of buttons
         *  pressed generated by our method [getButtonsPressed] from [e] to `touchTypeDescription`.
         *  Finally we append the string ")" to `touchTypeDescription`
         *
         *  * TOOL_TYPE_ERASER: we append the string "(eraser)" to `touchTypeDescription`.
         *
         *  * TOOL_TYPE_MOUSE: we append the string "(mouse)" to `touchTypeDescription`.
         *
         *  * default: we append the string "(unknown tool)" to `touchTypeDescription`.
         *
         * Finally we return `touchTypeDescription` to the caller.
         *
         * @param e The [MotionEvent] whose type we are to describe.
         * @return a human-readable string describing the type of touch that triggered [MotionEvent]
         * parameter [e].
         */
        private fun getTouchType(e: MotionEvent): String {
            var touchTypeDescription = " "
            val touchType: Int = e.getToolType(0)
            when (touchType) {
                MotionEvent.TOOL_TYPE_FINGER -> touchTypeDescription += "(finger)"
                MotionEvent.TOOL_TYPE_STYLUS -> {
                    touchTypeDescription += "(stylus, "
                    //Get some additional information about the stylus touch
                    val stylusPressure: Float = e.pressure
                    touchTypeDescription += "pressure: $stylusPressure"
                    if (Build.VERSION.SDK_INT >= 21) {
                        touchTypeDescription += ", buttons pressed: " + getButtonsPressed(e)
                    }
                    touchTypeDescription += ")"
                }

                MotionEvent.TOOL_TYPE_ERASER -> touchTypeDescription += "(eraser)"
                MotionEvent.TOOL_TYPE_MOUSE -> touchTypeDescription += "(mouse)"
                else -> touchTypeDescription += "(unknown tool)"
            }
            return touchTypeDescription
        }

        /**
         * Returns a human-readable string listing all the stylus buttons that were pressed when the
         * input [MotionEvent] occurred. We initialize our [String] variable `var buttons` with the
         * empty string, then if `e` has BUTTON_PRIMARY pressed we append the string " primary" to
         * `buttons`, BUTTON_SECONDARY pressed we append the string " secondary" to `buttons`,
         * BUTTON_TERTIARY pressed we append the string " tertiary" to `buttons`, BUTTON_BACK
         * pressed we append the string " back" to `buttons`, BUTTON_FORWARD pressed we append the
         * string " forward" to `buttons`. If after this `buttons` is still equal to "" we
         * set it to none. In any case we return `buttons` to the caller.
         *
         * @param e the [MotionEvent] whose pressed stylus buttons we wish to list.
         * @return a human-readable string listing all the stylus buttons that were pressed during
         * our [MotionEvent] parameter [e].
         */
        @RequiresApi(21)
        private fun getButtonsPressed(e: MotionEvent): String {
            var buttons = ""
            if (e.isButtonPressed(MotionEvent.BUTTON_PRIMARY)) {
                buttons += " primary"
            }
            if (e.isButtonPressed(MotionEvent.BUTTON_SECONDARY)) {
                buttons += " secondary"
            }
            if (e.isButtonPressed(MotionEvent.BUTTON_TERTIARY)) {
                buttons += " tertiary"
            }
            if (e.isButtonPressed(MotionEvent.BUTTON_BACK)) {
                buttons += " back"
            }
            if (e.isButtonPressed(MotionEvent.BUTTON_FORWARD)) {
                buttons += " forward"
            }
            if (buttons == "") {
                buttons = "none"
            }
            return buttons
        }
    }
}