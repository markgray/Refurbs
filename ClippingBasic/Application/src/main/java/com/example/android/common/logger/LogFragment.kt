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

import android.graphics.Typeface
import android.os.Build
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
 * Simple fragment which contains a [LogView] and uses is to output the log data it receives
 * through the [LogNode] interface.
 */
class LogFragment : Fragment() {
    /**
     * The [LogView] we use to output the log data we receive through the [LogNode] interface.
     */
    var logView: LogView? = null
        private set

    /**
     * The [ScrollView] which holds our [LogView] field [logView] and which is the [View] that is
     * returned by our [onCreateView] override. Our [inflateViews] method constructs and configures
     * [mScrollView] and adds a new instance of [LogView] to it (which is cached in [logView]).
     */
    private var mScrollView: ScrollView? = null

    /**
     * This method is called by our [onCreateView] override to construct, configure and return a
     * [ScrollView] (cached in [mScrollView]) which holds an instance of [LogView] (cached in
     * [logView]). First we initialize our [ScrollView] field [mScrollView] with a new instance,
     * initialize our [ViewGroup.LayoutParams] variable `val scrollParams` whose width and height
     * are both `MATCH_PARENT`, and then set the `layoutParams` property of [mScrollView] to
     * `scrollParams`. Next we initialize our [LogView] field [logView] with a new instance,
     * initialize our [ViewGroup.LayoutParams] variable `val logParams` to a clone of `scrollParams`,
     * set the `height` of `logParams` to `WRAP_CONTENT`, and then set the `layoutParams` property
     * of [logView] to `logParams`. We enable click events for `logView`, enable it to receive the
     * focus, and set its typeface and style to [Typeface.MONOSPACE] (`NORMAL` style of the default
     * monospace typeface). We want to set the padding to 16 dips so we calculate `val paddingPixels`
     * to be 16dips times the logical density of the display rounded up to [Int], and use `paddingPixels`
     * to set the padding on all sides of `logView`  as well as the size of the padding between the
     * compound drawables and the text. We then set the `gravity` of `logView` to [Gravity.BOTTOM]
     * and the text color, size, style, hint color, and highlight color to those of the system style
     * [android.R.style.TextAppearance_Holo_Medium]. Finally we add [logView] to [mScrollView] and
     * return [mScrollView] to the caller (always our [onCreateView] override in our case).
     *
     * @return the [View] which our [onCreateView] override will return for the fragment's UI.
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            logView!!.setTextAppearance(android.R.style.TextAppearance_Holo_Medium)
        } else {
            logView!!.setTextAppearance(activity, android.R.style.TextAppearance_Holo_Medium)
        }
        mScrollView!!.addView(logView)
        return mScrollView!!
    }

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onActivityCreated]. It is recommended to **only** inflate the layout in this
     * method and move logic that operates on the returned View to [onViewCreated].
     *
     * We initialize our [View] variable `val result` to the [View] returned by our [inflateViews]
     * method when it wraps a [ScrollView] around a [LogView]. [inflateViews] cached that [LogView]
     * in our field [logView] so we can use the [logView] reference to it to add a [TextWatcher] to
     * it whose [TextWatcher.afterTextChanged] override will scroll [mScrollView] down.
     *
     * Finally we return `result` to the caller.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment.
     * @param container If non-`null`, this is the parent view that the fragment's UI will be
     * attached to.  The fragment should not add the view itself, but this can be used to generate
     * the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI.
     */
    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val result = inflateViews()
        logView!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                mScrollView!!.fullScroll(ScrollView.FOCUS_DOWN)
            }
        })
        return result
    }
}