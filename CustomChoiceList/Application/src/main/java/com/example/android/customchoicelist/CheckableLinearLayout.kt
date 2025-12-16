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
package com.example.android.customchoicelist

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.LinearLayout

/**
 * This is a simple wrapper for [android.widget.LinearLayout] that implements the [android.widget.Checkable]
 * interface by keeping an internal 'checked' state flag.
 *
 * This can be used as the root view for a custom list item layout for
 * [android.widget.AbsListView] elements with a
 * [choiceMode][android.widget.AbsListView.setChoiceMode] set.
 */
class CheckableLinearLayout
/**
 * Our constructor. Perform inflation from XML and apply a class-specific base style from a theme
 * attribute or style resource. We just call our super's constructor.
 *
 * @param context The [Context] the view is running in, through which it can
 * access the current theme, resources, etc.
 * @param attrs   The attributes of the XML tag that is inflating the view.
 */
(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs), Checkable {
    /**
     * Our internal 'checked' state flag.
     */
    private var mChecked = false

    /**
     * Part of the [Checkable] interface, we just return the value of our [Boolean] field [mChecked]
     * internal 'checked' state flag.
     *
     * @return The current checked state of the view
     */
    override fun isChecked(): Boolean {
        return mChecked
    }

    /**
     * Change the checked state of the view. If our [Boolean] parameter [b] is not equal to our
     * [Boolean] field [mChecked] we set [mChecked] to it then call the [refreshDrawableState]
     * method to force our view to update its drawable state. This will cause [drawableStateChanged]
     * to be called on this view. Part of the [Checkable] interface.
     *
     * @param b The new checked state
     */
    override fun setChecked(b: Boolean) {
        if (b != mChecked) {
            mChecked = b
            refreshDrawableState()
        }
    }

    /**
     * Change the checked state of the view to the inverse of its current state. We call our
     * [setChecked] method with the inverse of our field [mChecked]. Part of the [Checkable]
     * interface.
     */
    override fun toggle() {
        isChecked = !mChecked
    }

    /**
     * Generate the new [android.graphics.drawable.Drawable] state for this view. This is
     * called by the view system when the cached Drawable state is determined to be invalid. To
     * retrieve the current state, you should use [getDrawableState]. We initialize our
     * [IntArray] variable `val drawableState` with the array holding the current `Drawable`
     * state of the view requesting one more entry than our [Int] parameter [extraSpace] has
     * requested using our super's [ViewGroup.onCreateDrawableState] method. If our method [isChecked]
     * returns `true` (our view is checked) we call the [View.mergeDrawableStates] method to merge our
     * additional state values in [IntArray] field [CHECKED_STATE_SET] into `drawableState`. In any
     * case we return `drawableState` to our caller.
     *
     * @param extraSpace if non-zero, this is the number of extra entries you
     * would like in the returned array in which you can place your own states.
     * @return Returns an [IntArray] holding the current `Drawable` state of
     * the view.
     */
    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState: IntArray = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(
                /* baseState = */ drawableState,
                /* additionalState = */ CHECKED_STATE_SET
            )
        }
        return drawableState
    }

    companion object {
        /**
         * State identifier indicating that the object is currently checked. See state_checkable for
         * an additional identifier that can indicate if any object may ever display a check,
         * regardless of whether state_checked is currently set. May be a boolean value, such as
         * "true" or "false".
         */
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }
}