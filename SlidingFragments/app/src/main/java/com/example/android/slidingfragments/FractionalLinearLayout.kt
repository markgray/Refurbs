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
package com.example.android.slidingfragments

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * In order to animate the fragment containing text on/off the screen, it is required that we know
 * the height of the device being used. However, this can only be determined at runtime, so we cannot
 * specify the required translation in an xml file. Since `FragmentTransaction`'s `setCustomAnimations`
 * method requires an ID of an animation defined via an xml file, this linear layout was built as a workaround.
 * This custom linear layout is created to specify the location of the fragment's layout as a fraction of the
 * device's height. By animating `yFraction` from 0 to 1, we can animate the fragment from the bottom
 * of the parent view to the top of the parent view, regardless of the device's specific size. The animator xml file
 * animator/slide_fragment_in.xml animates `yFraction` from 0 to slide_up_down_fraction (0.67) to
 * "slide us in" and animator/slide_fragment_out.xml animates `yFraction` from slide_up_down_fraction (0.67)
 * to 0 to "slide us out"
 */
class FractionalLinearLayout : LinearLayout {
    /**
     * Fraction of the parent view used for our top Y position (0 places us below the bottom of the
     * screen, 1 places us at the top).
     */
    private var mYFraction = 0f

    /**
     * Height of the screen (actually the height of our view).
     */
    private var mScreenHeight = 0

    /**
     * Our one argument constructor. We just call our super's constructor. unused.
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     */
    constructor(context: Context?) : super(context)

    /**
     * Perform inflation from XML (layout file layout/text_fragment.xml). We just call our super's
     * constructor.
     *
     * @param context The Context the view is running in, through which it can access the current
     * theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * This is called during layout when the size of this view has changed. If you were just added
     * to the view hierarchy, you're called with the old values of 0. First we call our super's
     * implementation of `onSizeChanged`, then we save our parameter `h` in our field
     * `int mScreenHeight`, and then call the `setY` method to set our top Y coordinate
     * to `mScreenHeight` relative to our parent (places us off the screen).
     *
     * @param w    Current width of this view.
     * @param h    Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mScreenHeight = h
        y = mScreenHeight.toFloat()
    }

    /**
     * Getter for our `float mYFraction` field. Used by the animation framework
     *
     * @return current value of our `mYFraction` field
     */
    @Suppress("unused") // Used by the animation framework
    var yFraction: Float
        get() = mYFraction
        /**
         * Setter for our `float mYFraction` field. First we save our parameter `float yFraction`
         * in our field `float mYFraction`, then we call the `setY` method to set the top Y
         * coordinate of our view to our field `mScreenHeight` minus our parameter `yFraction`
         * times `mScreenHeight` if `mScreenHeight` is greater than 0 or to 0 if it is not.
         *
         * @param yFraction value to set our field `float mYFraction` to
         */
        set(yFraction) {
            mYFraction = yFraction
            y = (if (mScreenHeight > 0) mScreenHeight - yFraction * mScreenHeight else 0f)
        }
}