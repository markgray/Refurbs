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
@file:Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")

package com.example.android.revealeffectbasic

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log

/**
 * This sample shows a view that is revealed when a button is clicked.
 */
class RevealEffectBasicFragment : Fragment() {
    /**
     * Called to do initial creation of a fragment. First we call our super's implementation of
     * `onCreate`, then we call the [setHasOptionsMenu] method with `true` to report that this
     * fragment would like to participate in populating the options menu by receiving a call to
     * [onCreateOptionsMenu] and related methods.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") // TODO: Use MenuProvider
        setHasOptionsMenu(true)
    }

    /**
     * Called to have the fragment instantiate its user interface view. We return the view that our
     * [LayoutInflater] parameter [inflater] inflates from our layout file `R.layout.reveal_effect_basic`
     * using our [ViewGroup] parameter [container] for LayoutParams without attaching to that view.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or `null`.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(
            /* resource = */ R.layout.reveal_effect_basic,
            /* root = */ container,
            /* attachToRoot = */ false
        )
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. We initialize our [View] variable ` shape` by finding the view in
     * our [View] parameter [view] with resource ID `R.id.circle`, and our [Button] variable
     * `val button` by finding the view with id `R.id.button` ("Reveal"). We then set the
     * [View.OnClickListener] of `button` to a lambda whose [View.OnClickListener.onClick] override
     * creates and runs an [Animator] which animates a clipping circle which obscures [View] `shape`
     * then explodes from the top left corner of `shape` until it reveals the original view again.
     * We then call our super's implementation of `onViewCreated`.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val shape: View = view.findViewById(R.id.circle)
        val button: Button = view.findViewById(R.id.button)
        // Set a listener to reveal the view when clicked.
        button.setOnClickListener {
            // Create a reveal `Animator` that starts clipping the view from
            // the top left corner until the whole view is covered.
            val circularReveal: Animator = ViewAnimationUtils.createCircularReveal(
                /* view = */ shape,
                /* centerX = */ 0,
                /* centerY = */ 0,
                /* startRadius = */ 0f,
                /* endRadius = */ Math.hypot(shape.width.toDouble(), shape.height.toDouble()).toFloat()
            )
            circularReveal.interpolator = AccelerateDecelerateInterpolator()

            // Finally start the animation
            circularReveal.start()
            Log.d(TAG, "Starting Reveal animation")
        }
        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "RevealEffectBasicFragment"
    }
}
