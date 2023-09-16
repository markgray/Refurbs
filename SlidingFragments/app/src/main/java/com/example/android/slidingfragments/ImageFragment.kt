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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * TODO: Add kdoc
 */
class ImageFragment : Fragment() {
    /**
     * `OnClickListener` we should set our view's `OnClickListener` to.
     */
    private var mClickListener: View.OnClickListener? = null

    /**
     * Called to have the fragment instantiate its user interface view. We initialize `View view`
     * by using our parameter `LayoutInflater inflater` to inflate our layout file R.layout.image_fragment
     * into it using our parameter `ViewGroup container` for layout params without attaching to it.
     * Then we set our `OnClickListener` to our field `clickListener` and return `view` to the caller.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed, we ignore.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.image_fragment, container, false)
        view.setOnClickListener(mClickListener)
        return view
    }

    /**
     * Setter for our field `OnClickListener clickListener`.
     *
     * @param clickListener `OnClickListener` we should use for our view
     */
    fun setClickListener(clickListener: View.OnClickListener?) {
        this.mClickListener = clickListener
    }
}