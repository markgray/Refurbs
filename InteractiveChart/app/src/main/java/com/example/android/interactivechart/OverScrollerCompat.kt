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
package com.example.android.interactivechart

import android.annotation.SuppressLint
import android.os.Build
import android.widget.OverScroller
import androidx.annotation.RequiresApi

/**
 * A utility class for using [android.widget.OverScroller] in a backward-compatible fashion.
 */
object OverScrollerCompat {
    /**
     * Returns the absolute value of the current velocity of [OverScroller] parameter [overScroller]
     * if [Build.VERSION.SDK_INT] is greater than or equal to [Build.VERSION_CODES.ICE_CREAM_SANDWICH],
     * otherwise it returns 0f.
     *
     * @param overScroller the [OverScroller] instance whose current velocity we should return.
     * @return the value returned by the [OverScroller.getCurrVelocity] method (kotlin `currVelocity`
     * property) of our [OverScroller] parameter [overScroller] if the device is SDK 14 or newer, or
     * 0f for older devices.
     *
     * @see android.view.ScaleGestureDetector.getCurrentSpanY
     */
    @SuppressLint("ObsoleteSdkInt") // Left in to facilitate reuse of code.
    @RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun getCurrVelocity(overScroller: OverScroller): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            overScroller.currVelocity
        } else {
            0f
        }
    }
}