/*
 * Copyright 2014 The Android Open Source Project
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
@file:Suppress("ReplaceJavaStaticMethodWithKotlinAnalog", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.clippingbasic

import android.graphics.Outline
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log

/**
 * This sample shows how to clip a [View] using an [Outline].
 */
class ClippingBasicFragment : Fragment() {
    /**
     * Store the click count so that we can show a different text on every click.
     */
    private var mClickCount = 0

    /**
     * The [Outline] used to clip the image with.
     */
    private var mOutlineProvider: ViewOutlineProvider? = null

    /**
     * An array of texts, loaded from the string-array with resource id `R.array.sample_texts`, and
     * displayed in round robin order every time the [changeText] method is called.
     */
    private lateinit var mSampleTexts: Array<String>

    /**
     * A reference to the [TextView] that shows different text strings when it is clicked.
     */
    private var mTextView: TextView? = null

    /**
     * Called to do initial creation of a fragment. First we call our super's implementation of
     * `onCreate` then we call [setHasOptionsMenu] with `true` to report that this fragment would
     * like to participate in populating the options menu by receiving a call to [onCreateOptionsMenu]
     * and related methods. We initialize our [ViewOutlineProvider] field [mOutlineProvider] with a
     * new instance of [ClipOutlineProvider], and initialize [Array] of [String] field [mSampleTexts]
     * by retrieving the string-array with resource id `R.array.sample_texts` from our activities
     * resources.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     * this is the state. We do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") // TODO: Replace with MenuProvider
        setHasOptionsMenu(true)
        mOutlineProvider = ClipOutlineProvider()
        mSampleTexts = resources.getStringArray(R.array.sample_texts)
    }

    /**
     * Called to have the fragment instantiate its user interface view. We return the [View]
     * constructed  by using our [LayoutInflater] parameter [inflater] to inflate our layout file
     * `R.layout.clipping_basic_fragment` using our [ViewGroup] parameter [container] to generate
     * the `LayoutParams` without attaching to it.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment.
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            /* resource = */ R.layout.clipping_basic_fragment,
            /* root = */ container,
            /* attachToRoot = */ false
        )
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. This gives subclasses a chance to initialize themselves once they
     * know their view hierarchy has been completely created. The fragment's view hierarchy is not
     * however attached to its parent at this point.
     *
     * First we call our super's implementation of `onViewCreated`, then we initialize our
     * [TextView] field [mTextView] by finding the view in our [View] parameter [view] with the
     * id `R.id.text_view` and call our [changeText] method to set its text to the first string in
     * our [Array] of strings field [mSampleTexts]. We initialize [View] variable `val clippedView`
     * by finding the view in [view] with id `R.id.frame`, then set its [ViewOutlineProvider] to our
     * [ViewOutlineProvider] field [mOutlineProvider] (which generates the [Outline] that defines the
     * shape of the shadow the view casts, and enables outline clipping). We find the view with id
     * `R.id.button` and set its [OnClickListener] to an anonymous class which toggles whether the
     * [View]'s Outline should be used to clip the contents of the View. We find the view with id
     * `R.id.text_view` then set its [OnClickListener] to an anonymous class which increments our
     * field [mClickCount], calls our method [changeText] to change the next to the next one in
     * order, and then calls the [View.invalidateOutline] method of [View] variable `clippedView` to
     * invalidate the outline just in case the [TextView] changed size.
     *
     * @param view The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Set the initial text for the TextView. */
        mTextView = view.findViewById(R.id.text_view)
        changeText()
        val clippedView: View = view.findViewById(R.id.frame)

        /* Sets the OutlineProvider for the View. */
        clippedView.outlineProvider = mOutlineProvider

        /* When the button is clicked, the text is clipped or un-clipped. */
        view.findViewById<View>(R.id.button).setOnClickListener { bt: View ->
            /**
             * Called when the button with id `R.id.button` is clicked. We branch on the value
             * returned by the [View.getClipToOutline] method of `clippedView`:
             *
             *  * `true`: (the Outline should be used to clip the contents of the View) we call the
             *  [View.setClipToOutline] method of `clippedView` with `false` to disable the clipping
             *  to outline, log this action, then set the text of the [Button] to the string with
             *  resource id `R.string.clip_button` ("Enable outline clipping")
             *
             *  * `false`: (the Outline should not be used to clip the contents of the View) we call
             *  the [View.setClipToOutline] method of `clippedView` with `true` to enable the
             *  clipping to outline, log this action, then set the text of the [Button] to the
             *  string with resource id `R.string.unclip_button` ("Disable outline clipping")
             *
             * @param bt [View] that was clicked
             */
            // Toggle whether the View is clipped to the outline
            if (clippedView.clipToOutline) {
                /* The Outline is set for the View, but disable clipping. */
                clippedView.clipToOutline = false
                Log.d(TAG, "Clipping to outline is disabled")
                (bt as Button).setText(R.string.clip_button)
            } else {
                /* Enables clipping on the View. */
                clippedView.clipToOutline = true
                Log.d(TAG, "Clipping to outline is enabled")
                (bt as Button).setText(R.string.unclip_button)
            }
        }

        /* When the text is clicked, a new string is shown. */
        view.findViewById<View>(R.id.text_view).setOnClickListener {
            mClickCount++

            // Update the text in the TextView
            changeText()

            // Invalidate the outline just in case the TextView changed size
            clippedView.invalidateOutline()
        }
    }

    /**
     * Changes the text of our [TextView] field [mTextView] to the string found in our [Array] of
     * [String] field [mSampleTexts] at entry [mClickCount] modulo the length of the array
     * [mSampleTexts].
     */
    private fun changeText() {
        // Compute the position of the string in the array using the number of strings
        //  and the number of clicks.
        val newText = mSampleTexts[mClickCount % mSampleTexts.size]

        /* Once the text is selected, change the TextView */
        mTextView!!.text = newText
        Log.d(TAG, "Text was changed.")
    }

    /**
     * A [ViewOutlineProvider] which clips the view with a rounded rectangle which is inset
     * by 10%
     */
    private class ClipOutlineProvider : ViewOutlineProvider() {
        /**
         * Called to get the provider to populate the Outline. This method will be called by a [View]
         * when its owned Drawables are invalidated, when the [View]'s size changes, or if
         * [View.invalidateOutline] is called explicitly. The input outline is empty and has
         * an alpha of `1.0f`.
         *
         * We initialize [Int] variable `val margin` to be the minimum of the width and height of
         * our [View] parameter [view] divided by 10. Then we call the [Outline.setRoundRect] method
         * of our [Outline] parameter [outline] to have a left value of `margin`, a top value of
         * `margin`, a right value of the width of [view] minus `margin`, a bottom value of the
         * height of [view] minus `margin`, and a radius of one half of `margin` (a rounded
         * rectangle which is inset by 10%).
         *
         * @param view The [View] building the outline.
         * @param outline The empty [Outline] to be populated.
         */
        override fun getOutline(view: View, outline: Outline) {
            val margin = Math.min(view.width, view.height) / 10
            outline.setRoundRect(margin, margin, view.width - margin,
                view.height - margin, (margin / 2).toFloat())
        }
    }

    companion object {
        /**
         * TAG used for logging
         */
        private const val TAG = "ClippingBasicFragment"
    }
}
