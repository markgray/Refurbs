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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.slidingfragments

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * This is the [Fragment] which describes the image displayed in our [ImageFragment],
 * and we are toggled on and off the screen by a user click.
 */
class TextFragment : Fragment() {
    /**
     * [View.OnClickListener] we should set on the view returned by our [onCreateView] override
     */
    private var mClickListener: View.OnClickListener? = null

    /**
     * [OnTextFragmentAnimationEndListener] whose [OnTextFragmentAnimationEndListener.onAnimationEnd]
     * override we should call when our enter animation ends.
     * TODO: Continue here.
     */
    var mListener: OnTextFragmentAnimationEndListener? = null

    /**
     * Called to have the fragment instantiate its user interface view. We initialize `View view`
     * by using our parameter `LayoutInflater inflater` to inflate our layout file R.layout.text_fragment
     * into it using our parameter `ViewGroup container` for layout params without attaching to it.
     * Then we set our `OnClickListener` to our field `clickListener` and return `view` to the caller.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed, we ignore.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.text_fragment, container, false)
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

    /**
     * Called when a fragment loads an animation. We initialize `int id` to the animator xml
     * file R.animator.slide_fragment_in if our parameter `enter` is true or to the file
     * R.animator.slide_fragment_out if it is false. We initialize `Animator anim` by loading
     * the `Animator` from the resource file `id`. If `enter` is true we add an
     * anonymous `AnimatorListenerAdapter` listener to it whose `onAnimationEnd` override
     * calls the `onAnimationEnd` method of our field `OnTextFragmentAnimationEndListener mListener`.
     * Finally we return `anim` to the caller.
     *
     * @param transit  transition type, always 0 in our case - we ignore.
     * @param enter    true if 'entering,' false otherwise
     * @param nextAnim The resource ID of the animation that is about to play, we ignore
     * @return the `Animator` we want to have used.
     */
    override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
        val id = if (enter) R.animator.slide_fragment_in else R.animator.slide_fragment_out
        val anim = AnimatorInflater.loadAnimator(activity, id)
        if (enter) {
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mListener!!.onAnimationEnd()
                }
            })
        }
        return anim
    }

    /**
     * Setter for our field `OnTextFragmentAnimationEndListener mListener`.
     *
     * @param listener the `OnTextFragmentAnimationEndListener` we should set our field
     * `mListener` to
     */
    fun setOnTextFragmentAnimationEnd(listener: OnTextFragmentAnimationEndListener?) {
        mListener = listener
    }
}