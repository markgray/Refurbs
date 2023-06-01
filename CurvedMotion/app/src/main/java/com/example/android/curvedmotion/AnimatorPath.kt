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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.example.android.curvedmotion

import java.util.ArrayList

/**
 * A simple Path object that holds information about the points along
 * a path. The API allows you to specify a move location (which essentially
 * jumps from the previous point in the path to the new one), a line location
 * (which creates a line segment from the previous location) and a curve
 * location (which creates a Bezier curve from the previous location).
 */
class AnimatorPath {
    /**
     *  The points in the path
     */
    var mPoints: ArrayList<PathPoint> = ArrayList()

    /**
     * Move from the current path point to the new one specified by [Float] parameters [x] and [y].
     * This will create a discontinuity if this point is neither the first point in the path nor the
     * same as the previous point in the path. We just call the [ArrayList.add] method of our
     * [ArrayList] of [PathPoint] field [mPoints] to add the [PathPoint] returned by the
     * [PathPoint.moveTo] method when it is called with [x] and [y] as its `x` and `y` arguments.
     *
     * @param x the x coordinate of the [PathPoint] to be added to [mPoints]
     * @param y the y coordinate of the [PathPoint] to be added to [mPoints]
     */
    fun moveTo(x: Float, y: Float) {
        mPoints.add(PathPoint.moveTo(x = x, y = y))
    }

    /**
     * Create a straight line from the current path point to the new one specified by [Float]
     * parameters [x] and [y]. We just call the [ArrayList.add] method of our [ArrayList] of
     * [PathPoint] field [mPoints] to add the [PathPoint] returned by the [PathPoint.lineTo]
     * method when it is called with [x] and [y] as its `x` and `y` arguments.
     *
     * @param x the x coordinate of the [PathPoint] to be added to [mPoints]
     * @param y the y coordinate of the [PathPoint] to be added to [mPoints]
     */
    fun lineTo(x: Float, y: Float) {
        mPoints.add(PathPoint.lineTo(x = x, y = y))
    }

    /**
     * Create a quadratic Bezier curve from the current path point to the new one specified by [x]
     * and [y]. The curve uses the current path location as the first anchor point, the control
     * points ([c0X], [c0Y]) and ([c1X], [c1Y]), and ([x], [y]) as the end anchor. We just call the
     * [ArrayList.add] method of our [ArrayList] of [PathPoint] field [mPoints] to add the [PathPoint]
     * returned by the [PathPoint.curveTo] method when it is called with our parameters.
     *
     * @param c0X the x coordinate of the first control point of the quadratic Bezier curve
     * @param c0Y the y coordinate of the first control point of the quadratic Bezier curve
     * @param c1X the x coordinate of the second control point of the quadratic Bezier curve
     * @param c1Y the y coordinate of the second control point of the quadratic Bezier curve
     * @param x the x coordinate of the end anchor.
     * @param y the y coordinate of the end anchor.
     */
    fun curveTo(c0X: Float, c0Y: Float, c1X: Float, c1Y: Float, x: Float, y: Float) {
        mPoints.add(PathPoint.curveTo(c0X, c0Y, c1X, c1Y, x, y))
    }

    /**
     * Returns a Collection of [PathPoint] objects that describe all points in the path. Since
     * [ArrayList] extends [AbstractList] which extends [AbstractCollection] which implements
     * [Collection] we can just return our [ArrayList] of [PathPoint] field [mPoints] and the
     * inheritance hierarchy does all the magic.
     */
    val points: Collection<PathPoint>
        get() = mPoints
}