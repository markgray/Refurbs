/*
 * Copyright 2014, The Android Open Source Project
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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.floatingactionbuttonbasic


import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Checkable
import android.widget.FrameLayout

/**
 * A Floating Action Button is a [android.widget.Checkable] view distinguished by a circled icon
 * floating above the UI, with special motion behaviors. This constructor performs inflation from
 * XML and applies a class-specific base style from a theme attribute or style resource. Called by
 * our three other constructors. First we call our super's constructor. In our `init` block:
 * We call the [setClickable] method with `true` (kotlin sets the `isClickable` property to `true`)
 * to make our view clickable. We set the [ViewOutlineProvider] for this view to an anonymous
 * [ViewOutlineProvider] whose [ViewOutlineProvider.getOutline] override sets our [Outline] to an
 * oval fitting the height and width of our view. Finally we call the [setClipToOutline] method with
 * `true` to enable clipping to the outline, using the provider we set above.
 *
 * @param context The [Context] the view is running in, through which it can access the current
 * theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style
 * resource that supplies default values for the view. Can be 0 to not look for defaults.
 * @param defStyleRes A resource identifier of a style resource that supplies default values for the
 * view, used only if `defStyleAttr` is 0 or can not be found in the theme. Can be 0 to not look for
 * defaults.
 */
class FloatingActionButton @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context!!, attrs, defStyleAttr, defStyleRes), Checkable {
    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changes.
     */
    interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a FAB has changed.
         *
         * @param fabView The [FloatingActionButton] view whose state has changed.
         * @param isChecked The new checked state of buttonView.
         */
        fun onCheckedChanged(fabView: FloatingActionButton?, isChecked: Boolean)
    }

    /**
     * A boolean that tells if the FAB is checked or not.
     */
    private var mChecked = false

    /**
     * A listener to communicate the fact that the FAB has changed it's state
     */
    private var mOnCheckedChangeListener: OnCheckedChangeListener? = null

    init {
        isClickable = true

        // Set the outline provider for this view. The provider is given the outline which it can
        // then modify as needed. In this case we set the outline to be an oval fitting the height
        // and width.
        outlineProvider = object : ViewOutlineProvider() {
            /**
             * Called to get the provider to populate the Outline. This method will be called by a
             * View when its owned Drawables are invalidated, when the View's size changes, or if
             * [View.invalidateOutline] is called explicitly. The [Outline] parameter [outline] is
             * empty and has an alpha of `1.0f`. We set our [Outline] parameter [outline] to an
             * oval fitting the height and width of our view.
             *
             * @param view The view building the outline.
             * @param outline The empty outline to be populated.
             */
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(
                    /* left = */ 0,
                    /* top = */ 0,
                    /* right = */ width,
                    /* bottom = */ height
                )
            }
        }

        // Finally, enable clipping to the outline, using the provider we set above
        clipToOutline = true
    }

    /**
     * Sets the checked/unchecked state of the [FloatingActionButton], part of the [Checkable]
     * interface. If our [Boolean] field [mChecked] is already equal to our [Boolean] parameter
     * [checked] we return having done nothing. Otherwise we set [mChecked] to [checked] and call
     * the method [refreshDrawableState] to force our view to update its drawable state. This will
     * cause the [drawableStateChanged] method to be called on this view. If our
     * [OnCheckedChangeListener] field is not `null` we call its
     * [OnCheckedChangeListener.onCheckedChanged] method with `this` as the [FloatingActionButton]
     * view whose state has changed and [checked] as the new checked state.
     *
     * @param checked value to set our state to.
     */
    override fun setChecked(checked: Boolean) {
        // If trying to set the current state, ignore.
        if (checked == mChecked) {
            return
        }
        mChecked = checked

        // Now refresh the drawable state (so the icon changes)
        refreshDrawableState()
        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener!!.onCheckedChanged(fabView = this, isChecked = checked)
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this button changes. We just save
     * our [OnCheckedChangeListener] parameter [listener] in our field [mOnCheckedChangeListener].
     *
     * @param listener the callback to call on checked state change
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        mOnCheckedChangeListener = listener
    }

    /**
     * Returns the current checked state of the view, which is our [Boolean] field [mChecked].
     *
     * @return The current checked state of the view
     */
    override fun isChecked(): Boolean {
        return mChecked
    }

    /**
     * Change the checked state of the view to the inverse of its current state, we just call our
     * [setChecked] method with the inverse of our [Boolean] field [mChecked] (in kotlin we set our
     * `isChecked` property to the inverse of our [Boolean] field [mChecked].
     */
    override fun toggle() {
        isChecked = !mChecked
    }

    /**
     * Override `performClick()` so that we can toggle the checked state when the view is clicked, we
     * call our method [toggle] to change the checked state of the view to the inverse of its
     * current state, then return the value returned by our super's implementation of `performClick`.
     *
     * @return True there was an assigned OnClickListener that was called, false
     * otherwise is returned.
     */
    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }

    /**
     * This is called during layout when the size of this view has changed. If you were just added
     * to the view hierarchy, you're called with the old values of 0. We call our super's implementation
     * of `onSizeChanged`, then call the [invalidateOutline] method to rebuild this [View]'s Outline
     * from its [ViewOutlineProvider] so that it corresponds to our new size.
     *
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // As we have changed size, we should invalidate the outline so that is the the
        // correct size
        invalidateOutline()
    }

    /**
     * Generate the new Drawable state for this view. This is called by the view system when the
     * cached Drawable state is determined to be invalid. To retrieve the current state, you should
     * use [getDrawableState]. We initialize [IntArray] variable `val drawableState` with the array
     * returned by our super's implementation of `onCreateDrawableState` when we call it requesting
     * one more drawable state than our caller requested. If our method [isChecked] returns `true`
     * (we are currently checked) we call the [View.mergeDrawableStates] method to merge our
     * [CHECKED_STATE_SET] into `drawableState`. In any case we return `drawableState` to our
     * caller.
     *
     * @param extraSpace if non-zero, this is the number of extra entries you would like in the
     * returned array in which you can place your own states.
     * @return Returns an [IntArray] holding the current `Drawable` state of the view.
     */
    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState: IntArray = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    companion object {
        /**
         * An array of states.
         */
        private val CHECKED_STATE_SET = intArrayOf(
            android.R.attr.state_checked
        )
    }
}