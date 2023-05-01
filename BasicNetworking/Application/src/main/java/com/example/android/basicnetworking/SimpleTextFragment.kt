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

package com.example.android.basicnetworking

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Simple fragment containing only a TextView. Used by TextPagerAdapter to create
 * tutorial-style pages for apps.
 */
class SimpleTextFragment
/**
 * Our required constructor, it does nothing.
 */
    : Fragment() {
    /**
     * Contains the text that will be displayed by this Fragment
     */
    var mText: String? = null

    /**
     * Contains a resource ID for the text that will be displayed by this fragment.
     */
    var mTextId: Int = -1
    /**
     * Getter for our `TextView mTextView` field.
     *
     * @return contents of our `TextView mTextView` field
     */
    /**
     * For situations where the app wants to modify text at Runtime, exposing the TextView.
     */
    var textView: TextView? = null
        private set

    /**
     * Called to have the fragment instantiate its user interface view. First we call our method
     * `processArguments()` to process any arguments that may have been provided via a call to
     * `setArguments` (None are in this example). We initialize our field `TextView mTextView`
     * with a new instance and set its gravity to Gravity.CENTER (places the object in the center of
     * its container in both the vertical and horizontal axis, not changing its size). If our field
     * `String mText` is not null we set the text of `mTextView` to it and log that text.
     * Finally we return `mTextView` to the caller.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here. We ignore.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Before initializing the textView, check if any arguments were provided via setArguments.
        processArguments()

        // Create a new TextView and set its text to whatever was provided.
        textView = TextView(activity)
        textView!!.gravity = Gravity.CENTER
        if (mText != null) {
            textView!!.text = mText
            Log.i("SimpleTextFragment", mText!!)
        }
        return textView
    }

    /**
     * Changes the text for this TextView, according to the resource ID provided. We call our method
     * `getTextView` to get our text view and call its `setText` method with the string
     * that the `FragmentActivity` this fragment is currently associated with fetches for the
     * resource id contained in our parameter `int stringId`.
     *
     * @param stringId A resource ID representing the text content for this Fragment's TextView.
     */
    fun setText(stringId: Int) {
        textView!!.text = requireActivity().getString(stringId)
    }

    /**
     * Processes the arguments passed into this Fragment via setArguments method.
     * Currently the method only looks for text or a textID, nothing else.
     *
     *
     * If `getArguments()` returns null we do nothing, otherwise we set `Bundle args` to
     * the arguments supplied when the fragment was instantiated and if `args` contains a string
     * under the key TEXT_KEY we set our field `String mText` to that string, and log what we did.
     * Else if `args` contains an integer stored under the key TEXT_ID_KEY, we set our field
     * `int mTextId` to that integer and set our field `String mText` to the string in
     * our resources with the id `mTextId`.
     */
    fun processArguments() {
        // For most objects we'd handle the multiple possibilities for initialization variables
        // as multiple constructors.  For Fragments, however, it's customary to use
        // setArguments / getArguments.
        if (arguments != null) {
            val args = arguments
            if (args!!.containsKey(TEXT_KEY)) {
                mText = args.getString(TEXT_KEY)
                Log.d("Constructor", "Added Text.")
            } else if (args.containsKey(TEXT_ID_KEY)) {
                mTextId = args.getInt(TEXT_ID_KEY)
                mText = getString(mTextId)
            }
        }
    }

    companion object {
        /**
         * Key which will be used to store/retrieve text passed in via setArguments.
         * Unused in this case.
         */
        const val TEXT_KEY: String = "text"

        /**
         * Key which will be used to store/retrieve text passed in via setArguments.
         * Unused in this case.
         */
        const val TEXT_ID_KEY: String = "text_id"
    }
}