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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.common.logger

import android.R.style.TextAppearance_Holo_Medium
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.Fragment

/**
 * Simple fraggment which contains a LogView and uses is to output log data it receives
 * through the LogNode interface.
 */
class LogFragment : Fragment() {
    /**
     * The [LogView] that this fragment uses to output log data.
     */
    var logView: LogView? = null
        private set

    /**
     * The [ScrollView] that holds the [LogView] and allows it to be scrolled.
     */
    private var mScrollView: ScrollView? = null

    /**
     * Creates a [ScrollView] and a [LogView] to display the log data. Programmatically sets
     * the properties of the views, including `Typeface`, padding, and gravity. Finally, it
     * adds the [LogView] to the [ScrollView].
     *
     * @return The [ScrollView] that contains the [LogView].
     */
    fun inflateViews(): View {
        mScrollView = ScrollView(activity)
        val scrollParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mScrollView!!.layoutParams = scrollParams
        logView = LogView(activity)
        val logParams = ViewGroup.LayoutParams(scrollParams)
        logParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        logView!!.layoutParams = logParams
        logView!!.isClickable = true
        logView!!.isFocusable = true
        logView!!.typeface = Typeface.MONOSPACE

        // Want to set padding as 16 dips, setPadding takes pixels.  Hooray math!
        val paddingDips = 16
        val scale = resources.displayMetrics.density.toDouble()
        val paddingPixels = (paddingDips * scale + .5).toInt()
        logView!!.setPadding(paddingPixels, paddingPixels, paddingPixels, paddingPixels)
        logView!!.compoundDrawablePadding = paddingPixels
        logView!!.gravity = Gravity.BOTTOM
        logView!!.setTextAppearance(TextAppearance_Holo_Medium)
        mScrollView!!.addView(logView)
        return mScrollView as ScrollView
    }

    /**
     * Called to have the fragment instantiate its user interface view. This is optional, and
     * non-graphical fragments can return `null`. This will be called between `onCreate(Bundle)`
     * and `onViewCreated(View, Bundle)`.
     *
     * It inflates the view hierarchy, and then sets up a `TextWatcher` on the `logView`
     * to automatically scroll to the bottom when new text is added.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the fragment.
     * @param container If non-`null`, this is the parent view that the fragment's UI will be attached
     * to. The fragment should not add the view itself, but this can be used to generate the
     * LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed from a previous
     * saved state as given here.
     * @return Returns the [View] for the fragment's UI, or `null`. In this case, it returns the
     * [ScrollView] containing the [LogView].
     */
    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val result = inflateViews()
        logView!!.addTextChangedListener(object : TextWatcher {
            /**
             * This method is called to notify you that, within [CharSequence] parameter [s], the
             * [Int] parameter [count] characters beginning at [Int] parameter [start] are about to
             * be replaced by new text with length [Int] parameter [after]. It is an error to attempt
             * to make changes to [s] from this callback.
             *
             * This implementation is empty as no action is needed before the text changes.
             *
             * @param s The text before the change.
             * @param start The starting index of the text to be changed.
             * @param count The number of characters to be replaced.
             * @param after The length of the new text that will replace the old text.
             */
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            /**
             * This method is called to notify you that, within [CharSequence] parameter [s], the
             * [Int] parameter [count] characters beginning at [Int] parameter [start] have just
             * replaced old text that had length [Int] parameter [before].
             * It is an error to attempt to make changes to [s] from this callback.
             *
             * This implementation is empty as no action is needed during the text change itself.
             * The scrolling action is handled in [afterTextChanged].
             *
             * @param s The text after the change.
             * @param start The starting index of the change.
             * @param before The length of the old text that was replaced.
             * @param count The length of the new text that has been inserted.
             */
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            /**
             * This method is called to notify you that, somewhere within [Editable] parameter [s],
             * the text has been changed. It is legitimate to make further changes to [s] from this
             * callback, but be careful not to get yourself into an infinite loop, because any
             * changes you make will cause this method to be called again recursively.
             *
             * This implementation automatically scrolls the [ScrollView] to the bottom to ensure the
             * latest log entry is always visible.
             *
             * @param s The [Editable] text that has been changed.
             */
            override fun afterTextChanged(s: Editable) {
                mScrollView!!.fullScroll(ScrollView.FOCUS_DOWN)
            }
        })
        return result
    }
}