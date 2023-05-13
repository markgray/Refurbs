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

import android.animation.TypeEvaluator

/**
 * This evaluator interpolates between two PathPoint values given the value t, the
 * proportion traveled between those points. The value of the interpolation depends
 * on the operation specified by the endValue (the operation for the interval between
 * PathPoints is always specified by the end point of that interval).
 */
class PathEvaluator : TypeEvaluator<PathPoint> {
    /**
     * This function returns the result of linearly interpolating the start and end values, with
     * `t` representing the proportion between the start and end values. The calculation is a
     * simple parametric calculation: result = x0 + t * (x1 - x0), where `x0` is `startValue`,
     * `x1` is `endValue`, and `t` is the fraction from the starting to the ending values.
     *
     *
     * First we declare `float x` and `float y`, then we branch on the value of the
     * `mOperation` field of our parameter `PathPoint endValue`:
     *
     *  *
     * CURVE: we initialize `float oneMinusT` to 1 minus our parameter `t` then
     * calculate values for `x` and `y` by interpreting `endValue` as a
     * Bezier curve between the starting point contained in the `mX` and `mY`
     * fields of our parameter `PathPoint startValue` and the end point contained in
     * those fields of `endValue`.
     *
     *  *
     * LINE: we calculate values for `x` and `y` by using `t` to calculate
     * how far along we are moving from the `mX` and `mY` fields of `startValue`
     * and the end point contained in those fields of `endValue`.
     *
     *  *
     * All other operations: we just set `x` to the `mX` field of `endValue`
     * and `y` to its `mY` field.
     *
     *
     * We return the `PathPoint` object returned by the `moveTo` method of `PathPoint`
     * when given the parameters `x` and `y` (a `PathPoint` object that describes a
     * discontinuous move to the given xy location).
     *
     * @param t          The fraction from the starting to the ending values
     * @param startValue The start value.
     * @param endValue   The end value.
     * @return A linear interpolation between the start and end values, given the `t` parameter.
     */
    override fun evaluate(t: Float, startValue: PathPoint, endValue: PathPoint): PathPoint {
        val x: Float
        val y: Float
        when (endValue.mOperation) {
            PathPoint.CURVE -> {
                val oneMinusT = 1 - t
                x = oneMinusT * oneMinusT * oneMinusT * startValue.mX
                    + 3 * oneMinusT * oneMinusT * t * endValue.mControl0X
                    + 3 * oneMinusT * t * t * endValue.mControl1X + t * t * t * endValue.mX
                y = oneMinusT * oneMinusT * oneMinusT * startValue.mY
                    + 3 * oneMinusT * oneMinusT * t * endValue.mControl0Y
                    + 3 * oneMinusT * t * t * endValue.mControl1Y
                    + t * t * t * endValue.mY
            }

            PathPoint.LINE -> {
                x = startValue.mX + t * (endValue.mX - startValue.mX)
                y = startValue.mY + t * (endValue.mY - startValue.mY)
            }

            else -> {
                x = endValue.mX
                y = endValue.mY
            }
        }
        return PathPoint.moveTo(x, y)
    }
}