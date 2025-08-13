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
import android.view.ScaleGestureDetector
import androidx.annotation.RequiresApi

/**
 * A utility class for using [android.view.ScaleGestureDetector] in a backward-compatible
 * fashion.
 */
object ScaleGestureDetectorCompat {
    /**
     * Returns the value returned by the [ScaleGestureDetector.getCurrentSpanX] method (kotlin
     * `currentSpanX` property) of our [ScaleGestureDetector] parameter [scaleGestureDetector] if
     * [Build.VERSION.SDK_INT] is greater than or equal to [Build.VERSION_CODES.HONEYCOMB], otherwise
     * it returns the value returned by the [ScaleGestureDetector.getCurrentSpan] method (kotlin
     * `currentSpan` property.
     *
     * @param scaleGestureDetector the [ScaleGestureDetector] we are to query for its `currentSpanX`
     * or `currentSpan` property depending on the SDK of the device.
     * @return the value returned by the [ScaleGestureDetector.getCurrentSpanX] method (kotlin
     * `currentSpanX` property) of our [ScaleGestureDetector] parameter [scaleGestureDetector] if
     * the device is using SDK 14 or newer, otherwise the value returned by its
     * [ScaleGestureDetector.getCurrentSpan] method (kotlin `currentSpan` property.
     *
     * @see android.view.ScaleGestureDetector.getCurrentSpanX
     */
    @SuppressLint("ObsoleteSdkInt") // Left in to facilitate reuse of code.
    @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
    fun getCurrentSpanX(scaleGestureDetector: ScaleGestureDetector): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            scaleGestureDetector.currentSpanX
        } else {
            scaleGestureDetector.currentSpan
        }
    }

    /**
     * Returns the value returned by the [ScaleGestureDetector.getCurrentSpanY] method (kotlin
     * `currentSpanY` property) of our [ScaleGestureDetector] parameter [scaleGestureDetector] if
     * [Build.VERSION.SDK_INT] is greater than or equal to [Build.VERSION_CODES.HONEYCOMB], otherwise
     * it returns the value returned by the [ScaleGestureDetector.getCurrentSpan] method (kotlin
     * `currentSpan` property.
     *
     * @param scaleGestureDetector the [ScaleGestureDetector] we are to query for its `currentSpanY`
     * or `currentSpan` property depending on the SDK of the device.
     * @return the value returned by the [ScaleGestureDetector.getCurrentSpanY] method (kotlin
     * `currentSpanY` property) of our [ScaleGestureDetector] parameter [scaleGestureDetector] if
     * the device is using SDK 14 or newer, otherwise the value returned by its
     * [ScaleGestureDetector.getCurrentSpan] method (kotlin `currentSpan` property.
     *
     * @see android.view.ScaleGestureDetector.getCurrentSpanY
     */
    @SuppressLint("ObsoleteSdkInt") // Left in to facilitate reuse of code.
    @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
    fun getCurrentSpanY(scaleGestureDetector: ScaleGestureDetector): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            scaleGestureDetector.currentSpanY
        } else {
            scaleGestureDetector.currentSpan
        }
    }
}