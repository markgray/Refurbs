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

package com.example.android.curvedmotion

/**
 * A class that holds information about a location and how the path should get to that
 * location from the previous path location (if any). Any [PathPoint] holds the information for
 * its location as well as the instructions on how to traverse the preceding interval from the
 * previous location.
 */
class PathPoint {
    /**
     * The x coordinate of this [PathPoint]
     */
    var mX: Float

    /**
     * The y coordinate of this [PathPoint]
     */
    var mY: Float

    /**
     * The x coordinate of the first control point, if any, for a [PathPoint] of type [CURVE].
     */
    var mControl0X: Float = 0f

    /**
     * The y coordinate of the first control point, if any, for a [PathPoint] of type [CURVE].
     */
    var mControl0Y: Float = 0f

    /**
     * The x coordinate of the second control point, if any, for a [PathPoint] of type [CURVE].
     */
    var mControl1X: Float = 0f

    /**
     * The y coordinate of the second control point, if any, for a [PathPoint] of type [CURVE].
     */
    var mControl1Y: Float = 0f

    /**
     * The motion described by the path to get from the previous [PathPoint] in an [AnimatorPath]
     * to the location of this [PathPoint]. This can be one of [MOVE], [LINE], or [CURVE].
     */
    var mOperation: Int

    /**
     * Line/Move constructor. We just store our parameters in the corresponding fields of this
     * [PathPoint].
     *
     * @param operation the motion described by the path to get from the previous [PathPoint] in an
     * [AnimatorPath] to the location of this [PathPoint]. This can be one of [MOVE], [LINE], or
     * [CURVE].
     * @param x The x coordinate of this [PathPoint].
     * @param y The y coordinate of this [PathPoint].
     */
    private constructor(operation: Int, x: Float, y: Float) {
        mOperation = operation
        mX = x
        mY = y
    }

    /**
     * Curve constructor. We just store our parameters in the corresponding fields of this
     * [PathPoint] and set our [mOperation] field to [CURVE].
     *
     * @param c0X The x coordinate of the first control point
     * @param c0Y The y coordinate of the first control point
     * @param c1X The x coordinate of the second control point
     * @param c1Y The y coordinate of the second control point
     * @param x The x coordinate of this [PathPoint]
     * @param y The y coordinate of this [PathPoint]
     */
    private constructor(c0X: Float, c0Y: Float, c1X: Float, c1Y: Float, x: Float, y: Float) {
        mControl0X = c0X
        mControl0Y = c0Y
        mControl1X = c1X
        mControl1Y = c1Y
        mX = x
        mY = y
        mOperation = CURVE
    }

    companion object {
        /*
         * The possible path operations that describe how to move from a preceding [PathPoint] to the
         * location described by this [PathPoint].
         */

        /**
         * A discontinuous move to our xy location
         */
        const val MOVE: Int = 0

        /**
         * A continuous line from the previous [PathPoint] to our xy location
         */
        const val LINE: Int = 1

        /**
         * A Bezier curve from the previous [PathPoint] to our xy location
         */
        const val CURVE: Int = 2

        /**
         * Constructs and returns a PathPoint object that describes a line to the given xy location.
         * We just return a [PathPoint] constructed to use [LINE] as its [PathPoint.mOperation]
         * field, and our [Float] parameters [x] and [y] for its [PathPoint.mX] and [PathPoint.mY]
         * fields respectively.
         *
         * @param x The x coordinate of the [PathPoint] of the [LINE].
         * @param y The y coordinate of the [PathPoint] of the [LINE].
         */
        fun lineTo(x: Float, y: Float): PathPoint {
            return PathPoint(operation = LINE, x = x, y = y)
        }

        /**
         * Constructs and returns a [PathPoint] object that describes a Bezier curve to the given
         * [x] and [y] coordinates with the control points at ([c0X], [c0Y]) and ([c1X], [c1Y]).
         */
        fun curveTo(c0X: Float, c0Y: Float, c1X: Float, c1Y: Float, x: Float, y: Float): PathPoint {
            return PathPoint(c0X, c0Y, c1X, c1Y, x, y)
        }

        /**
         * Constructs and returns a [PathPoint] object that describes a discontinuous move to the
         * given [x] and [y] coordinates.
         */
        fun moveTo(x: Float, y: Float): PathPoint {
            return PathPoint(MOVE, x, y)
        }
    }
}