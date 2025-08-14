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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.slidingfragments

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import androidx.fragment.app.FragmentTransaction

/**
 * This application shows a simple technique to animate and overlay two fragments
 * on top of each other in order to provide a more immersive experience,
 * as opposed to only having full screen transitions. When additional content
 * (text) related to the currently displayed content (image) is to be shown,
 * the currently visible content can be moved into the background instead of
 * being removed from the screen entirely. This effect can therefore
 * provide a more natural way of displaying additional information to the user
 * using a different fragment.
 *
 * In this specific demo, tapping on the screen toggles between the two
 * animated states of the fragment. When the animation is called,
 * the fragment with an image animates into the background while the fragment
 * containing text slides up on top of it. When the animation is toggled once
 * more, the text fragment slides back down and the image fragment regains
 * focus. See [...](https://www.youtube.com/watch?v=xbl5cxfA1n4)
 */
class SlidingFragments : AppCompatActivity(), OnTextFragmentAnimationEndListener, OnBackStackChangedListener {
    /**
     * [ImageFragment] with id `R.id.move_fragment`, it displays an [ImageView] of the jpg with ID
     * `R.drawable.golden_gate` (drawable-hdpi/golden_gate.jpg)
     */
    var mImageFragment: ImageFragment? = null

    /**
     * [TextFragment] which displays the layout with ID `R.layout.text_fragment`
     * (layout/text_fragment.xml), it is added in a [FragmentTransaction] to the
     * container with id `R.id.move_to_back_container` which is the in the layout
     * file with ID `R.layout.sliding_fragments_layout` that we use
     * (layout/sliding_fragments_layout.xml)
     */
    var mTextFragment: TextFragment? = null

    /**
     * [View] with id `R.id.dark_hover_view` in our layout file, its alpha is animated, it shares
     * the [FrameLayout] in our layout file with the [ImageFragment] fragment and is atop it with
     * its alpha set to 0 in our [onCreate] override.
     */
    var mDarkHoverView: View? = null

    /**
     * Flag to indicate that we have "slid" in the [TextFragment] field [mTextFragment], it is
     * toggled in our [switchFragments] method.
     */
    var mDidSlideOut: Boolean = false

    /**
     * Flag to indicate that we are currently animating the switch of fragments, it disables our
     * [switchFragments] method when `true` until it is set to `false` by the `onAnimationEnd`
     * callbacks added to the animations.
     */
    var mIsAnimating: Boolean = false

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.sliding_fragments_layout`. We
     * initialize [View] field [mDarkHoverView] by finding the view with id `R.id.dark_hover_view`
     * and set its alpha to 0. We initialize [ImageFragment] field [mImageFragment] by using the
     * [FragmentManager] for interacting with fragments associated with this activity to find the
     * fragment that was identified by the id `R.id.move_fragment` when inflated from XML. We
     * initialize our [TextFragment] field [mTextFragment] with a new instance. We use the
     * [FragmentManager] for interacting with fragments associated with this activity to add
     * 'this' as an [OnBackStackChangedListener] so that our [onBackStackChanged] override will be
     * called whenever the contents of the back stack change. We then set the [OnClickListener]
     * of [mImageFragment], [mTextFragment], and [mDarkHoverView] all to our [OnClickListener]
     * field [mClickListener]. Finally we call the [TextFragment.setOnTextFragmentAnimationEnd]
     * method of [mTextFragment] to set its [OnTextFragmentAnimationEndListener] field `mListener`
     * to 'this' (it adds an [AnimatorListenerAdapter] to the [Animator] that is returned by its
     * `onCreateAnimator` override whose [onAnimationEnd] override calls our method [onAnimationEnd]).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sliding_fragments_layout)
        val rootView = findViewById<FrameLayout>(R.id.move_to_back_container)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        mDarkHoverView = findViewById(R.id.dark_hover_view)
        mDarkHoverView!!.alpha = 0f
        mImageFragment = supportFragmentManager.findFragmentById(R.id.move_fragment) as ImageFragment?
        mTextFragment = TextFragment()
        supportFragmentManager.addOnBackStackChangedListener(this)
        mImageFragment!!.setClickListener(mClickListener)
        mTextFragment!!.setClickListener(mClickListener)
        mDarkHoverView!!.setOnClickListener(mClickListener)
        mTextFragment!!.setOnTextFragmentAnimationEnd(this)
    }

    /**
     * [OnClickListener] used by our fields [mImageFragment], [mTextFragment], and [mDarkHoverView],
     * its [OnClickListener.onClick] override just calls our [switchFragments] method.
     */
    var mClickListener: OnClickListener = OnClickListener {
        switchFragments()
    }

    /**
     * This method is used to toggle between the two fragment states by calling the appropriate
     * animations between them. The entry and exit animations of the text fragment are specified
     * in `R.animator` resource files. The entry and exit animations of the image fragment are
     * specified in the [slideBack] and [slideForward] methods below. The reason for separating
     * the animation logic in this way is because the translucent dark hover view must fade in at
     * the same time as the image fragment animates into the background, which would be difficult
     * to time properly given that the [FragmentTransaction.setCustomAnimations] method can only
     * modify the two fragments in the transaction. If our [Boolean] flag field [mIsAnimating] is
     * `true` we return having done nothing (the animations started by a previous call to this
     * method are still in progress). Otherwise we set [mIsAnimating] to `true` and branch on the
     * value of our [Boolean] flag field [mDidSlideOut]:
     *
     *  * `true`: (we have previously animated the image fragment into the background and want to
     *  restore it) We set [mDidSlideOut] to `false` and call the [FragmentManager.popBackStack]
     *  method of the [FragmentManager] for interacting with fragments associated with this activity
     *  to Pop the top state off the back stack.
     *
     *  * `false`: (we are in the starting state of our app with the image fragment in front and
     *  want to animate it into the background) We set [mDidSlideOut] to `true`, then initialize
     *  [AnimatorListener] variable `val listener` with an [AnimatorListenerAdapter] whose
     *  [AnimatorListener.onAnimationEnd] override uses the [FragmentManager] for interacting with
     *  fragments associated with this activity to begin [FragmentTransaction] variable
     *  `val transaction` then calls the [FragmentTransaction.setCustomAnimations] method of
     *  `transaction` to set the enter animation to the xml file `R.animator.slide_fragment_in`,
     *  and the pop exit animation to xml file `R.animator.slide_fragment_out`. It then uses
     *  `transaction` to add our [TextFragment] field [mTextFragment] to the container with id
     *  `R.id.move_to_back_container`, calls the [FragmentTransaction.addToBackStack] method of
     *  `transaction` with `null` to add this transaction to the BackStack with no name for the
     *  BackStack state, and commit the transaction. Having created [AnimatorListener] variable
     *  `val listener` we now call our method [slideBack] with it to animate the image fragment
     *  into the background by both scaling and rotating the fragment's view, as well as adding
     *  a translucent dark hover view to inform the user that it is inactive and to add `listener`
     *  as a [AnimatorListener] to that animation.
     */
    private fun switchFragments() {
        if (mIsAnimating) {
            return
        }
        mIsAnimating = true
        if (mDidSlideOut) {
            mDidSlideOut = false
            supportFragmentManager.popBackStack()
        } else {
            mDidSlideOut = true
            val listener: AnimatorListener = object : AnimatorListenerAdapter() {
                /**
                 * Notifies the end of the animation. We use the `FragmentManager` for interacting
                 * with fragments associated with this activity to begin `FragmentTransaction transaction`.
                 * We call the `setCustomAnimations` method of `transaction` to set the enter
                 * animation to the xml file R.animator.slide_fragment_in, and the pop exit animation to
                 * xml file R.animator.slide_fragment_out. We then use `transaction` to add our field
                 * `mTextFragment` to the container with id R.id.move_to_back_container, call the
                 * `addToBackStack` method of `transaction` with null to add this transaction
                 * to the BackStack with no name for the BackStack state, and commit the transaction.
                 *
                 * @param arg0 The animation which reached its end.
                 */
                override fun onAnimationEnd(arg0: Animator) {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.setCustomAnimations(
                        /* enter = */ R.animator.slide_fragment_in,
                        /* exit = */ 0,
                        /* popEnter = */ 0,
                        /* popExit = */ R.animator.slide_fragment_out
                    )
                    transaction.add(R.id.move_to_back_container, mTextFragment!!)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
            }
            slideBack(listener)
        }
    }

    /**
     * Called whenever the contents of the back stack change. If our [Boolean] flag field
     * [mDidSlideOut] is `false` we call our method [slideForward] with `null` as the
     * [AnimatorListener] argument (it animates the image fragment into the foreground by both
     * scaling and rotating the fragment's view, while also removing the previously added translucent
     * dark hover view. Upon the completion of this animation, the image fragment regains focus
     * since this method is called from the [onBackStackChanged] method).
     */
    override fun onBackStackChanged() {
        if (!mDidSlideOut) {
            slideForward(null)
        }
    }

    /**
     * This method animates the image fragment into the background by both scaling and rotating the
     * fragment's view, as well as adding a translucent dark hover view to inform the user that it
     * is inactive. We initialize [View] variable `val movingFragmentView` with the root view for
     * the layout of [ImageFragment] field [mImageFragment]. We initialize [PropertyValuesHolder]
     * variable `val rotateX` with an instance which will animate the "rotationX" property to 40f,
     * [PropertyValuesHolder] variable `val scaleX` to an instance which will animate the "scaleX"
     * property to 0.8f, and [PropertyValuesHolder] varible `val scaleY` to an instance which will
     * animate the "scaleY" property to 0.8f, then initialize [ObjectAnimator] variable
     * `val movingFragmentAnimator` with an instance which will apply these three to
     * `movingFragmentView`. We initialize [ObjectAnimator] variable `val darkHoverViewAnimator` to
     * an instance which will animate the "alpha" property of [View] field [mDarkHoverView] from
     * 0.0f to 0.5f, then initialize [ObjectAnimator] variable `val movingFragmentRotator` to an
     * instance which will animate the "rotationX" property of `movingFragmentView` to 0, and set
     * its start delay to the integer stored as a resource under the id
     * `R.integer.half_slide_up_down_duration` (150ms). Now we initialize [AnimatorSet] variable
     * `val s` with a new instance, set it to play together `movingFragmentAnimator`,
     * `darkHoverViewAnimator`, and `movingFragmentRotator`, add our [AnimatorListener] parameter
     * [listener] to it and start it running.
     *
     * @param listener an [AnimatorListener] to add to our animation
     */
    fun slideBack(listener: AnimatorListener?) {
        val movingFragmentView: View? = mImageFragment!!.view
        val rotateX = PropertyValuesHolder.ofFloat("rotationX", 40f)
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.8f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.8f)
        val movingFragmentAnimator = ObjectAnimator.ofPropertyValuesHolder(movingFragmentView, rotateX, scaleX, scaleY)
        val darkHoverViewAnimator = ObjectAnimator.ofFloat(mDarkHoverView, "alpha", 0.0f, 0.5f)
        val movingFragmentRotator = ObjectAnimator.ofFloat(movingFragmentView, "rotationX", 0f)
        movingFragmentRotator.startDelay = resources.getInteger(R.integer.half_slide_up_down_duration).toLong()
        val s = AnimatorSet()
        s.playTogether(movingFragmentAnimator, darkHoverViewAnimator, movingFragmentRotator)
        s.addListener(listener)
        s.start()
    }

    /**
     * This method animates the image fragment into the foreground by both scaling and rotating the
     * fragment's view, while also removing the previously added translucent dark hover view. Upon
     * the completion of this animation, the image fragment regains focus since this method is
     * called from the [onBackStackChanged] method. We initialize [View] variable
     * `val movingFragmentView` with the root view for the layout of [ImageFragment] field
     * [mImageFragment]. We initialize [PropertyValuesHolder] variable `val rotateX` with an
     * instance which will animate the "rotationX" property to 40f, [PropertyValuesHolder] variable
     * `val scaleX` to an instance which will animate the "scaleX" property to 1.0f, and
     * [PropertyValuesHolder] variable `val scaleY` to an instance which will animate the "scaleY"
     * property to 1.0f, then initialize [ObjectAnimator] variable `val movingFragmentAnimator` with
     * an instance which will apply these three to `movingFragmentView`. We initialize
     * [ObjectAnimator] variable `val darkHoverViewAnimator` to an instance which will animate the
     * "alpha" property of [View] field [mDarkHoverView] from 0.5f to 0.0f, then initialize
     * [ObjectAnimator] variable `val movingFragmentRotator` to an instance which will animate the
     * "rotationX" property of `movingFragmentView` to 0, and set its start delay to the integer
     * stored as a resource under the id `R.integer.half_slide_up_down_duration` (150ms). Now we
     * initialize [AnimatorSet] variable `val s` with a new instance, set it to play together
     * `movingFragmentAnimator`, `movingFragmentRotator`, and `darkHoverViewAnimator`, set its start
     * delay to the integer stored as a resource under the id `R.integer.half_slide_up_down_duration`
     * (150ms), and add an anonymous [AnimatorListenerAdapter] whose
     * [AnimatorListenerAdapter.onAnimationEnd] override sets our [Boolean] flag field [mIsAnimating]
     * to `false`. Finally we start `s` running.
     *
     * @param listener an unused [AnimatorListener] always null
     */
    fun slideForward(listener: AnimatorListener?) {
        val movingFragmentView = mImageFragment!!.view
        val rotateX = PropertyValuesHolder.ofFloat("rotationX", 40f)
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f)
        val movingFragmentAnimator = ObjectAnimator.ofPropertyValuesHolder(movingFragmentView, rotateX, scaleX, scaleY)
        val darkHoverViewAnimator = ObjectAnimator.ofFloat(mDarkHoverView, "alpha", 0.5f, 0.0f)
        val movingFragmentRotator = ObjectAnimator.ofFloat(movingFragmentView, "rotationX", 0f)
        movingFragmentRotator.startDelay = resources.getInteger(R.integer.half_slide_up_down_duration).toLong()
        val s = AnimatorSet()
        s.playTogether(movingFragmentAnimator, movingFragmentRotator, darkHoverViewAnimator)
        s.startDelay = resources.getInteger(R.integer.slide_up_down_duration).toLong()
        s.addListener(object : AnimatorListenerAdapter() {
            /**
             * Notifies the end of the animation. We just set our [Boolean] flag field
             * [mIsAnimating] to `false`.
             *
             * @param animation The animation which reached its end.
             */
            override fun onAnimationEnd(animation: Animator) {
                mIsAnimating = false
            }
        })
        s.start()
    }

    /**
     * Notifies us that the entry animation of the text fragment has completed. We just set our
     * [Boolean] field flag [mIsAnimating] to false (part of the [OnTextFragmentAnimationEndListener]
     * interface).
     */
    override fun onAnimationEnd() {
        mIsAnimating = false
    }
}
