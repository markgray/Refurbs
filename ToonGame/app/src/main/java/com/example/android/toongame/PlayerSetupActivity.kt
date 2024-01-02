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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "JoinDeclarationAndAssignment", "ReplaceJavaStaticMethodWithKotlinAnalog", "MemberVisibilityCanBePrivate")

package com.example.android.toongame

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout

/**
 * This activity, launched from the [ToonGame] activity, takes the user between three
 * different setup screens where they choose a name, choose a difficulty rating, and
 * enter important financial information. All of the screens are meant to be
 * simple, engaging, and fun.
 */
class PlayerSetupActivity : Activity() {
    /**
     * The [RelativeLayout] with id [R.id.container] which contains our UI widgets.
     */
    var mContainer: ViewGroup? = null

    /**
     * No idea why this is here. UNUSED
     */
    @Suppress("unused")
    var mEditText: EditText? = null

    /**
     * Current state of the UI, used in the [OnPreDrawListener.onPreDraw] override of our
     * [OnPreDrawListener] field [mPreDrawListener] to decide which views should be visible
     * to the user.
     */
    private var mEntryState: Int = NAME_STATE

    /**
     * [SkewableTextView] in our layout file with id [R.id.nameTV], used to have the user select
     * a name to use.
     */
    var mNameTV: SkewableTextView? = null

    /**
     * [SkewableTextView] in our layout file with id [R.id.ageTV], used to have the user select
     * a difficulty level to use.
     */
    var mDifficultyTV: SkewableTextView? = null

    /**
     * [SkewableTextView] in our layout file with id [R.id.creditTV], used to have the user enter
     * a credit card to use.
     */
    var mCreditTV: SkewableTextView? = null

    /**
     * [LinearLayout] in our layout file with id [R.id.nameButtons], contains the buttons for
     * selecting a name.
     */
    var mNameButtons: ViewGroup? = null

    /**
     * [LinearLayout] in our layout file with id [R.id.difficultyButtons], contains the buttons
     * for selecting the difficulty level.
     */
    var mDifficultyButtons: ViewGroup? = null

    /**
     * [LinearLayout] in our layout file with id [R.id.creditButtons1], contains the buttons for
     * entering the numbers 0 to 4 when entering the credit card number.
     */
    var mCreditButtons1: ViewGroup? = null

    /**
     * [LinearLayout] in our layout file with id [R.id.creditButtons2], contains the buttons for
     * entering the numbers 5 to 9 when entering the credit card number.
     */
    var mCreditButtons2: ViewGroup? = null

    /**
     * [Button] in [ViewGroup] field [mNameButtons] with id [R.id.bobButton], used for selecting
     * the name "Bob"
     */
    var mBobButton: Button? = null

    /**
     * [Button] in [ViewGroup] field [mNameButtons] with id [R.id.janeButton], used for selecting
     * the name "Jane"
     */
    var mJaneButton: Button? = null

    /**
     * [Button] in [ViewGroup] field [mNameButtons] with id [R.id.patButton], used for selecting
     * the name "Pat"
     */
    var mPatButton: Button? = null

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.player_setup_layout] and cancel any
     * pending transition animations. We initialize our [ViewGroup] field [mContainer] by finding
     * the view with id [R.id.container], fetch the [ViewTreeObserver] for its hierarchy and add
     * our field [OnPreDrawListener] field [mPreDrawListener] as a callback to be invoked when
     * its view tree is about to be drawn.
     *
     * We initialize [SkewableTextView] field [mNameTV] by finding the view with id [R.id.nameTV],
     * [SkewableTextView] field [mDifficultyTV] by finding the view with id [R.id.ageTV], and
     * [SkewableTextView] field [mCreditTV] by finding the view with id [R.id.creditTV]. We
     * initialise [Button] field [mBobButton] to the button our method [setupButton] returns after
     * setting up the button with id [R.id.bobButton] ("Bob"), [Button] field [mJaneButton]` to the
     * button our method [setupButton] returns after setting up the button with id [R.id.janeButton]
     * ("Jane"), and [Button] field [mPatButton] to the button our method [setupButton] returns after
     * setting up the button with id [R.id.patButton] ("Pat"). We then call [setupButton] to set up
     * the button with id [R.id.easyButton] ("Easy"), the button with id [R.id.hardButton] ("Hard"),
     * and the button with id [R.id.megaHardButton] ("Mega Hard").
     *
     * We initialize our [ViewGroup] field [mNameButtons] by finding the view with id [R.id.nameButtons],
     * our [ViewGroup] field [mDifficultyButtons] by finding the view with id [R.id.difficultyButtons],
     * our [ViewGroup] field [mCreditButtons1] by finding the view with id [R.id.creditButtons1],
     * and our [ViewGroup] field [mCreditButtons2] by finding the view with id [R.id.creditButtons2].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.player_setup_layout)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION") // Needed for SDK older than 34
            overridePendingTransition(0, 0)
        }
        mContainer = findViewById(R.id.container)
        mContainer!!.viewTreeObserver.addOnPreDrawListener(mPreDrawListener)
        mNameTV = findViewById(R.id.nameTV)
        mDifficultyTV = findViewById(R.id.ageTV)
        mCreditTV = findViewById(R.id.creditTV)
        mBobButton = setupButton(R.id.bobButton)
        mJaneButton = setupButton(R.id.janeButton)
        mPatButton = setupButton(R.id.patButton)
        setupButton(R.id.easyButton)
        setupButton(R.id.hardButton)
        setupButton(R.id.megaHardButton)
        mNameButtons = findViewById(R.id.nameButtons)
        mDifficultyButtons = findViewById(R.id.difficultyButtons)
        mCreditButtons1 = findViewById(R.id.creditButtons1)
        mCreditButtons2 = findViewById(R.id.creditButtons2)
    }

    /**
     * This is called when our activity is done and should be closed. First we call our super's
     * implementation of `finish`, then we cancel any pending transition animations.
     */
    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION") // Needed for SDK older than 34
            overridePendingTransition(0, 0)
        }
    }

    /**
     * Finds the [Button] whose resource id is our [Int] parameter [resourceId], sets its
     * [OnTouchListener] to our field [mButtonPressListener] and returns the button found
     * to our caller.
     *
     * @param resourceId resource id of the button we are to set up.
     * @return the [Button] whose resource ID is our [Int] parameter [resourceId]
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupButton(resourceId: Int): Button {
        val button: Button
        button = findViewById(resourceId)
        button.setOnTouchListener(mButtonPressListener)
        return button
    }

    /**
     * [OnTouchListener] we use for all our [Button]'s, used only to animate the size of the
     * button when it is pressed and released.
     */
    @SuppressLint("ClickableViewAccessibility")
    private val mButtonPressListener = OnTouchListener { v: View, event: MotionEvent ->
        /**
         * Called when a touch event is dispatched to a view. This allows listeners to get a chance
         * to respond before the target view. We switch on the action of our [MotionEvent] parameter
         * `event`:
         *
         *  * [MotionEvent.ACTION_DOWN]: we retrieve a [ViewPropertyAnimator] object for our [View]
         *  parameter `v`, set its duration to [ToonGame.SHORT_DURATION], have it animate the
         *  "scaleX" property of the view from 1 to .8 and the "scaleY" property of the view from
         *  1 to .8 using [DecelerateInterpolator] field [sDecelerator] as its [TimeInterpolator].
         *
         *  * [MotionEvent.ACTION_UP]: we retrieve a [ViewPropertyAnimator] object for our [View]
         *  parameter `v`, set its duration to [ToonGame.SHORT_DURATION], have it animate the
         *  "scaleX" property of the view to 1 and the "scaleY" property of the view to 1 using
         *  [DecelerateInterpolator] field [sDecelerator] as its [TimeInterpolator].
         *
         *  * `else`: we do nothing
         *
         * We then return `false` so that the event will be passed on to [View] parameter `v`.
         *
         * @param v     The view the touch event has been dispatched to.
         * @param event The [MotionEvent] object containing full information about the event.
         * @return `true` if the listener has consumed the event, `false` otherwise.
         */
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate()
                    .setDuration(/* duration = */ ToonGame.SHORT_DURATION)
                    .scaleX(/* value = */ .8f)
                    .scaleY(/* value = */ .8f)
                    .setInterpolator(/* interpolator = */ sDecelerator)
            }
            MotionEvent.ACTION_UP -> {
                v.animate()
                    .setDuration(/* duration = */ ToonGame.SHORT_DURATION)
                    .scaleX(/* value = */ 1f)
                    .scaleY(/* value = */ 1f)
                    .setInterpolator(/* interpolator = */ sAccelerator)
            }
            else -> {}
        }
        false
    }

    /**
     * Called when the user has selected a name [Button] or a difficulty [Button], we animate the
     * movement of a copy of the [Button] to the proper place at the top of the container. We
     * initialize our [ViewGroup] variable `val parent` by retrieving the [View.getParent] value
     * (kotlin `parent` property) of our [View] parameter [clickedView]. Then we loop over [Int]
     * variable `var i` for the number of children in the `parent` view group, initializing [Button]
     * variable `val child` with the child view at position `i` in `parent`. If that `child` is not
     * equal to `clickedView` we fetch a [ViewPropertyAnimator] from `child` and animate its alpha
     * to 0. If it is the [Button] that was clicked we initialize [Button] variable `val buttonCopy`
     * with a new instance and set the visibility of `child` to INVISIBLE. We set the background of
     * `buttonCopy` to the background of `child` and the text of `buttonCopy` to the text of
     * `child`. We initialize [RelativeLayout.LayoutParams] variable `val params` with a new instance
     * whose width and height are both [RelativeLayout.LayoutParams.WRAP_CONTENT], add the layout
     * rule [RelativeLayout.ALIGN_PARENT_TOP] to it, and add the alignment rule contained in our
     * [Int] parameter [alignmentRule] (this will be [RelativeLayout.ALIGN_PARENT_RIGHT] when a
     * difficulty [Button] has been selected or [RelativeLayout.ALIGN_PARENT_LEFT] when a name
     * [Button] has been selected). We set the margins of `params` to 25 left, 50 top, 25 right,
     * and 50 bottom then set the layout params of `buttonCopy` to `params`. We set the padding of
     * `buttonCopy` to the padding of `child` (on all sides) then set the text size to the same
     * size as well. We set the type face of `buttonCopy` to the same as `child` with a
     * [Typeface.BOLD] style. We initialize [ColorStateList] variable `val colors` with the text
     * colors of `child` and set the text color of `buttonCopy` to the [ColorStateList.getDefaultColor]
     * (kotlin `default` property) color of `colors`. We allocate 2 [Int]'s for [IntArray] variable
     * `val oldLocationInWindow` and load it with the (x,y) location of `child`. We then add the view
     * `buttonCopy` to [ViewGroup] field [mContainer].
     *
     * We retrieve the [ViewTreeObserver] for the hierarchy of `buttonCopy` and call its
     * [ViewTreeObserver.addOnPreDrawListener] method to add an anonymous [OnPreDrawListener]
     * instance whose [OnPreDrawListener.onPreDraw] override first removes itself as a
     * [OnPreDrawListener], then allocates 2 ints for [IntArray] variable `val locationInWindow`
     * and loads it with the (x,y) location of `buttonCopy`. We initialize [Float] variable
     * `val deltaX` with the difference between the old X coordinate of `child` in
     * `oldLocationInWindow[0]` and the X coordinate of `buttonCopy` in `locationInWindow[0]`, and
     * initialize [Float] variable `val deltaY` with the difference between the old Y coordinate of
     * `child` in `oldLocationInWindow[1]` and the Y coordinate of `buttonCopy` in `locationInWindow[1]`.
     * We then translate the horizontal location of of `buttonCopy` by `deltaX` and the vertical
     * location by `deltaY` (move the `buttonCopy` to the same location occupied by `child`).
     *
     * We now start to construct the animation that will occur when we move the button back to the
     * top of `container`. We initialize [PropertyValuesHolder] variable `val pvhSX` to an instance
     * which will animate the SCALE_X property to 3, [PropertyValuesHolder] variable `val pvhSY` to
     * an instance which will animate the SCALE_Y property to 3, then initialize [ObjectAnimator]
     * variable `val bounceAnim` to an animator which will animate these properties of `buttonCopy`.
     * We set the repeat count of `bounceAnim` to 1, the repeat mode to REVERSE, the [TimeInterpolator]
     * to our [DecelerateInterpolator] field [sDecelerator] and the duration to 300ms. We initialize
     * [PropertyValuesHolder] variable `val pvhTX` to an instance which will animate the TRANSLATION_X
     * property to 0, and [PropertyValuesHolder] variable `val pvhTY` to an instance which will
     * animate the TRANSLATION_Y property to 0, then initialize [ObjectAnimator] variable `val moveAnim`
     * to an animator which will animate these properties of `buttonCopy`. We set the duration of
     * `moveAnim` to 600ms, then start both `bounceAnim` and `moveAnim` running. We then add to
     * `moveAnim` an anonymous [AnimatorListener] (actually an [AnimatorListenerAdapter] which
     * supplies empty implementations of the abstract methods so we need only implement the
     * [AnimatorListener.onAnimationEnd] method we are interested in) whose [AnimatorListener.onAnimationEnd]
     * override switches on our [Int] field [mEntryState] which allows it to transition the activity
     * to the next stage. Finally our [OnPreDrawListener.onPreDraw] override returns `true` to proceed
     * with the current drawing pass.
     *
     * @param clickedView the [View] that was clicked.
     * @param alignmentRule either [RelativeLayout.ALIGN_PARENT_RIGHT] for the difficulty buttons or
     * [RelativeLayout.ALIGN_PARENT_LEFT] for the name buttons, used for the [RelativeLayout.LayoutParams]
     * used when placing the selected button at the top of the container.
     */
    fun buttonClick(clickedView: View, alignmentRule: Int) {
        val parent = clickedView.parent as ViewGroup
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i) as Button
            if (child !== clickedView) {
                child.animate().alpha(0f)
            } else {
                val buttonCopy = Button(this)
                child.visibility = View.INVISIBLE
                buttonCopy.background = child.background
                buttonCopy.text = child.text
                val params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                params.addRule(alignmentRule)
                params.setMargins(25, 50, 25, 50)
                buttonCopy.layoutParams = params
                buttonCopy.setPadding(
                    /* left = */ child.paddingLeft,
                    /* top = */ child.paddingTop,
                    /* right = */ child.paddingRight,
                    /* bottom = */ child.paddingBottom
                )
                buttonCopy.setTextSize(TypedValue.COMPLEX_UNIT_PX, child.textSize)
                buttonCopy.setTypeface(child.typeface, Typeface.BOLD)
                val colors: ColorStateList = child.textColors
                buttonCopy.setTextColor(colors.defaultColor)
                val oldLocationInWindow = IntArray(2)
                child.getLocationInWindow(oldLocationInWindow)
                mContainer!!.addView(buttonCopy)
                buttonCopy.viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
                    /**
                     * Callback method to be invoked when the view tree is about to be drawn. At this
                     * point, all views in the tree have been measured and given a frame. Clients can
                     * use this to adjust their scroll bounds or even to request a new layout before
                     * drawing occurs.
                     *
                     * First we remove ourselves as a [OnPreDrawListener], then we allocate 2 ints
                     * for [IntArray] variable `val locationInWindow` and load it with the (x,y)
                     * location of `buttonCopy`. We initialize [Float] varialbe `val deltaX` with
                     * the difference between the old X coordinate of `child` in `oldLocationInWindow[0]`
                     * and the X coordinate of `buttonCopy` in `locationInWindow[0]`, and initialize
                     * [Float] variable `val deltaY` with the old Y coordinate of `child` in
                     * `oldLocationInWindow[1]` and the Y coordinate of `buttonCopy` in `locationInWindow[1]`.
                     * We then translate the horizontal location of `buttonCopy` by `deltaX` and the
                     * vertical location by `deltaY` (move the `buttonCopy` to the same location
                     * occupied by `child`).
                     *
                     * We now start to construct the animation that will occur when we move the button
                     * back to the top of `container`. We initialize [PropertyValuesHolder] variable
                     * `val pvhSX` to an instance which will animate the SCALE_X property to 3,
                     * [PropertyValuesHolder] variable `val pvhSY` to an instance which will animate
                     * the SCALE_Y property to 3, then initialize [ObjectAnimator] variable
                     * `val bounceAnim` to an animator which will animate these properties of
                     * `buttonCopy`. We set the repeat count of `bounceAnim` to 1, the repeat mode
                     * to REVERSE, the [TimeInterpolator] to our [DecelerateInterpolator] field
                     * [sDecelerator] and the duration to 300ms. We initialize [PropertyValuesHolder]
                     * variable `val pvhTX` to an instance which will animate the TRANSLATION_X
                     * property to 0, and [PropertyValuesHolder] variable `val pvhTY` to an instance
                     * which will animate the TRANSLATION_Y property to 0, then initialize
                     * [ObjectAnimator]` moveAnim` to an animator which will animate these properties
                     * of `buttonCopy`. We set the duration of `moveAnim` to 600ms, then start both
                     * `bounceAnim` and `moveAnim` running. We then add to `moveAnim` an anonymous
                     * [AnimatorListener] (actually an [AnimatorListenerAdapter] which supplies empty
                     * implementations of the abstract methods so we need only implement the
                     * [AnimatorListener.onAnimationEnd] method we are interested in) whose
                     * [AnimatorListener.onAnimationEnd] override switches on our [Int] field
                     * [mEntryState] which allows it to transition the activity to the next stage.
                     * Finally we return `true` to to proceed with the current drawing pass.
                     *
                     * @return Return `true` to proceed with the current drawing pass, or `false` to cancel.
                     */
                    override fun onPreDraw(): Boolean {
                        buttonCopy.viewTreeObserver.removeOnPreDrawListener(this)
                        val locationInWindow = IntArray(2)
                        buttonCopy.getLocationInWindow(locationInWindow)
                        val deltaX = (oldLocationInWindow[0] - locationInWindow[0]).toFloat()
                        val deltaY = (oldLocationInWindow[1] - locationInWindow[1]).toFloat()
                        buttonCopy.translationX = deltaX
                        buttonCopy.translationY = deltaY
                        val pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 3f)
                        val pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 3f)
                        val bounceAnim = ObjectAnimator.ofPropertyValuesHolder(
                            buttonCopy,
                            pvhSX,
                            pvhSY
                        )
                        bounceAnim.repeatCount = 1
                        bounceAnim.repeatMode = ValueAnimator.REVERSE
                        bounceAnim.interpolator = sDecelerator
                        bounceAnim.duration = 300
                        val pvhTX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f)
                        val pvhTY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f)
                        val moveAnim = ObjectAnimator.ofPropertyValuesHolder(
                            buttonCopy,
                            pvhTX,
                            pvhTY
                        )
                        moveAnim.duration = 600
                        bounceAnim.start()
                        moveAnim.start()
                        moveAnim.addListener(object : AnimatorListenerAdapter() {
                            /**
                             * Notifies the end of the `moveAnim` animation. We switch on the value
                             * of [Int] field [mEntryState]:
                             *
                             *  * [NAME_STATE]: We initialize [Runnable] variable `val runnable` with
                             *  a new instance whose [Runnable.run] override sets the visibility of
                             *  our [ViewGroup] field [mDifficultyButtons] to VISIBLE, the visibility
                             *  of [ViewGroup] field [mNameButtons] to GONE, then calls our [popChildrenIn]
                             *  method to animate the appearance of the children of [mDifficultyButtons].
                             *  We then call our `slideToNext` method which animates the change from
                             *  [SkewableTextView] field [mNameTV] to [SkewableTextView] field
                             *  [mDifficultyTV] then runs [Runnable] `runnable`.
                             *
                             *  * [DIFFICULTY_STATE]: We set the visibility of [ViewGroup] field
                             *  [mDifficultyButtons] to GONE, loop over [Int] variable `var i` from
                             *  0 to 4 adding the view returned by our method [setupNumberButton] for
                             *  `i` to [ViewGroup] vield [mCreditButtons1], then loop from 5 to 9
                             *  adding the view returned by our method [setupNumberButton] for `i`
                             *  to [ViewGroup] field [mCreditButtons2]. We initialize [Runnable]
                             *  variable `val runnable` with a new instance whose [Runnable.run]
                             *  override animates the appearance of [ViewGroup] field [mCreditButtons1]
                             *  and [ViewGroup] field [mCreditButtons2] using our [popChildrenIn]
                             *  method. We then call our [slideToNext] method which animates the
                             *  change from [SkewableTextView] field [mDifficultyTV] to [SkewableTextView]
                             *  field [mCreditTV] then runs [Runnable] `runnable`.
                             *
                             * @param animation The animation which reached its end.
                             */
                            override fun onAnimationEnd(animation: Animator) {
                                when (mEntryState) {
                                    NAME_STATE -> {
                                        /**
                                         * When an object implementing interface `Runnable` is used
                                         * to create a thread, starting the thread causes this method to be
                                         * called in that separately executing thread. We set the visibility
                                         * of `ViewGroup mDifficultyButtons` to VISIBLE and the
                                         * visibility of `ViewGroup mNameButtons` to gone, then call
                                         * our `popChildrenIn` method to animate the appearance of
                                         * the buttons in `mDifficultyButtons`
                                         */
                                        val runnable = Runnable {
                                            mDifficultyButtons!!.visibility = View.VISIBLE
                                            mNameButtons!!.visibility = View.GONE
                                            popChildrenIn(mDifficultyButtons, null)
                                        }
                                        slideToNext(mNameTV, mDifficultyTV, runnable)
                                        mEntryState = DIFFICULTY_STATE
                                    }

                                    DIFFICULTY_STATE -> {
                                        mDifficultyButtons!!.visibility = View.GONE
                                        run {
                                            var iLocal = 0
                                            while (iLocal < 5) {
                                                mCreditButtons1!!.addView(setupNumberButton(iLocal))
                                                ++iLocal
                                            }
                                        }
                                        run {
                                            var iLocal = 5
                                            while (iLocal < 10) {
                                                mCreditButtons2!!.addView(setupNumberButton(iLocal))
                                                ++iLocal
                                            }
                                        }
                                        val runnable = Runnable {
                                            mCreditButtons1!!.visibility = View.VISIBLE
                                            val runnable = Runnable {
                                                mCreditButtons2!!.visibility = View.VISIBLE
                                                popChildrenIn(mCreditButtons2, null)
                                            }
                                            popChildrenIn(mCreditButtons1, runnable)
                                        }
                                        slideToNext(mDifficultyTV, mCreditTV, runnable)
                                        mEntryState = CREDIT_STATE
                                    }
                                }
                            }
                        })
                        return true
                    }
                })
            }
        }
    }

    /**
     * Called when one of the difficulty selection buttons has been clicked (thanks to the attribute
     * android:onClick="selectDifficulty" for each of the buttons in our layout file). We just call
     * our method `buttonClick` with our parameter `View clickedView` and the alignment
     * rule ALIGN_PARENT_RIGHT (used to move a copy of the selected button to the top right of the
     * container).
     *
     * @param clickedView the button which has been clicked.
     */
    fun selectDifficulty(clickedView: View) {
        buttonClick(clickedView, RelativeLayout.ALIGN_PARENT_RIGHT)
    }

    /**
     * Called when one of the name selection buttons has been clicked (thanks to the attribute
     * android:onClick="selectDifficulty" for each of the buttons in our layout file). We just call
     * our method `buttonClick` with our parameter `View clickedView` and the alignment
     * rule ALIGN_PARENT_LEFT (used to move a copy of the selected button to the top left of the
     * container).
     *
     * @param clickedView the button which has been clicked.
     */
    fun selectName(clickedView: View) {
        buttonClick(clickedView, RelativeLayout.ALIGN_PARENT_LEFT)
    }

    /**
     * Called to create a number button displaying the number `int number`. First we initialize
     * `Button button` with a new instance. We set its text size to 36sp, set its text color to
     * white, its type face to the same as `Button mBobButton` in BOLD style, set its text to
     * the string value of our parameter `int number`, and its padding to 0 on all sides. We
     * initialize `OvalShape oval` with a new instance, and create `ShapeDrawable drawable`
     * from it. We fetch the `Paint` used to draw `drawable` and set its color to a random
     * int with a 0xFF alpha. We then set the background of `button` to `drawable` and its
     * `OnTouchListener` to `mButtonPressListener`. Finally we return `button` to the
     * caller.
     *
     * @param number number to display as the text of the button.
     * @return an oval button with a random background color displaying our parameter `int number`
     * as its text, whose `OnTouchListener` is set to our field `mButtonPressListener`
     */
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun setupNumberButton(number: Int): Button {
        val button = Button(this@PlayerSetupActivity)
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
        button.setTextColor(Color.WHITE)
        button.setTypeface(mBobButton!!.typeface, Typeface.BOLD)
        button.text = Integer.toString(number)
        button.setPadding(0, 0, 0, 0)
        val oval = OvalShape()
        val drawable = ShapeDrawable(oval)
        drawable.paint.color = 0xFF shl 24 or ((50 + 150 * Math.random()).toInt() shl 16) or (
            (50 + 150 * Math.random()).toInt() shl 8) or (50 + 150 * Math.random()).toInt()
        button.background = drawable
        button.setOnTouchListener(mButtonPressListener)
        return button
    }

    /**
     * `OnPreDrawListener` we use to animate the appearance of `ViewGroup mContainer`
     * (it scales in from 0 to 1 using an `OvershootInterpolator`, then as the end action of
     * that animation it pops in the name buttons).
     */
    var mPreDrawListener: OnPreDrawListener = object : OnPreDrawListener {
        /**
         * Callback method to be invoked when the view tree is about to be drawn. At this point, all
         * views in the tree have been measured and given a frame. First we remove ourselves as a
         * `OnPreDrawListener` of `mContainer`. We then set the X and Y scale both to
         * 0. We use a `ViewPropertyAnimator` object of `mContainer` to animate its
         * "scaleX" and "scaleY" to 1 using a new instance of `OvershootInterpolator`. We set
         * the duration of the animation to LONG_DURATION with an end action consisting of an anonymous
         * `Runnable` whose `run` override finds the view with id R.id.nameButtons, sets
         * it to be visible, then calls our `popChildrenIn` method to animate the appearance
         * of the buttons in that view.
         *
         *
         * Having done all this, we return true to proceed with the current drawing pass.
         *
         * @return Return true to proceed with the current drawing pass, or false to cancel.
         */
        override fun onPreDraw(): Boolean {
            mContainer!!.viewTreeObserver.removeOnPreDrawListener(this)
            mContainer!!.scaleX = 0f
            mContainer!!.scaleY = 0f
            mContainer!!.animate().scaleX(1f).scaleY(1f).interpolator = OvershootInterpolator()
            mContainer!!.animate().setDuration(ToonGame.LONG_DURATION).withEndAction {
                val buttonsParent = findViewById<ViewGroup>(R.id.nameButtons)
                buttonsParent.visibility = View.VISIBLE
                popChildrenIn(buttonsParent, null)
            }
            return false
        }
    }

    /**
     * Animates the appearance of the child views of our parameter `ViewGroup parent` and has
     * `Runnable endAction` run when the animations end if it is not null. First we initialize
     * `TimeInterpolator overshooter` with a new instance of `OvershootInterpolator`. We
     * initialize `int childCount` with the number of children views contained in our parameter
     * `ViewGroup parent`, and allocate `childCount` `ObjectAnimator` instances to
     * initialize `ObjectAnimator[] childAnims`. We now loop over `int i` for the children
     * in `parent`:
     *
     *  *
     * We initialize `View child` by fetching a reference to the `i`'th child of
     * `parent`, then scale both its X and Y dimensions to 0.
     *
     *  *
     * We initialize `PropertyValuesHolder pvhSX` with an instance which will animate
     * the SCALE_X property to 1, and `PropertyValuesHolder pvhSY` with an instance
     * which will animate the SCALE_Y property to 1.
     *
     *  *
     * We initialize `ObjectAnimator anim` with an instance which will apply both
     * `pvhSX` and `pvhSY` to `child`, set its duration to 150ms, and set
     * its `TimeInterpolator` to `overshooter`.
     *
     *  *
     * We then save `anim` in `childAnims[ i ]` and loop around for the next child.
     *
     *
     * When done creating animations for each of the children of `parent` we initialize
     * `AnimatorSet set` with a new instance, set it up to play each of the animations in the
     * array `childAnims` sequentially, and start it running. Finally if our parameter
     * `Runnable endAction` is not null, we add an `AnimatorListenerAdapter` to `set`
     * whose `onAnimationEnd` calls the `run` method of `endAction`.
     *
     * @param parent `ViewGroup` containing the views whose appearance we want to animate.
     * @param endAction a `Runnable` which if not null we are to run when our animations end.
     */
    @SuppressLint("Recycle")
    private fun popChildrenIn(parent: ViewGroup?, endAction: Runnable?) {
        // for all children, scale in one at a time
        val overshooter: TimeInterpolator = OvershootInterpolator()
        val childCount = parent!!.childCount
        val childAnims = arrayOfNulls<ObjectAnimator>(childCount)
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            child.scaleX = 0f
            child.scaleY = 0f
            val pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f)
            val pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
            val anim = ObjectAnimator.ofPropertyValuesHolder(child, pvhSX, pvhSY)
            anim.duration = 150
            anim.interpolator = overshooter
            childAnims[i] = anim
        }
        val set = AnimatorSet()
        set.playSequentially(*childAnims)
        set.start()
        if (endAction != null) {
            set.addListener(object : AnimatorListenerAdapter() {
                /**
                 * Notifies the end of the animation. We just call the `run` method of
                 * `Runnable endAction`.
                 *
                 * @param animation The animation which reached its end.
                 */
                override fun onAnimationEnd(animation: Animator) {
                    endAction.run()
                }
            })
        }
    }

    /**
     * Called to animate the transition between two UI states, optionally running a `Runnable`
     * when the animation is done (if it is not null). We initialize `ObjectAnimator currentSkewer`
     * with a new instance which will animate the "skewX" property of our parameter `currentView`
     * to -.5, and set its `TimeInterpolator` to `DecelerateInterpolator sDecelerator`.
     * We initialize `ObjectAnimator currentMover` with a new instance which will animate the
     * TRANSLATION_X property of our parameter `currentView` to minus the width of our container
     * `ViewGroup mContainer`, set its `TimeInterpolator` to our field
     * `LinearInterpolator sLinearInterpolator`, and set its duration to MEDIUM_DURATION.
     *
     *
     * We set the visibility of our parameter `SkewableTextView nextView` to VISIBLE, set its
     * "skewX" property to -.5, and set its TRANSLATION_X property to the width of our container
     * `ViewGroup mContainer` (off the right side of the screen).
     *
     *
     * We then initialize `ObjectAnimator nextMover` with a new instance which will animate the
     * TRANSLATION_X property of our parameter `currentView` to 0, set its `TimeInterpolator`
     * to our field `AccelerateInterpolator sAccelerator`, and set its duration to MEDIUM_DURATION.
     * We initialize `ObjectAnimator nextSkewer` with a new instance which will animate the "skewX"
     * property of our parameter `nextView` to 0, and set its `TimeInterpolator` to our
     * field `sOvershooter` (an `OvershootInterpolator` under the hood).
     *
     *
     * We initialize `AnimatorSet moverSet` with a new instance, and set it up to play both
     * `currentMover` and `nextMover` at the same time. We initialize `AnimatorSet fullSet`
     * with a new instance and set it up to play `currentSkewer`, `moverSet`, and `nextSkewer`
     * sequentially. We then add an `AnimatorListenerAdapter` listener to `fullSet` which overrides
     * the `onAnimationEnd` method to set the "skewX" property of `currentView` to 0, set its
     * visibility to GONE, and set its TRANSLATION_X property to 0. Then if our parameter `endAction`
     * is not null we call its `run` method.
     *
     *
     * When done setting things up we start `AnimatorSet fullSet` running.
     *
     * @param currentView the `SkewableTextView` which is currently being displayed
     * @param nextView the next `SkewableTextView` to display
     * @param endAction the `Runnable` to run (if not null) when the animation between the two
     * `SkewableTextView` parameters has finished.
     */
    private fun slideToNext(currentView: SkewableTextView?,
                            nextView: SkewableTextView?, endAction: Runnable?) {
        // skew/anticipate current view, slide off, set GONE, restore translation
        val currentSkewer = ObjectAnimator.ofFloat(currentView, "skewX", -.5f)
        currentSkewer.interpolator = sDecelerator
        val currentMover = ObjectAnimator.ofFloat(currentView, View.TRANSLATION_X,
            -mContainer!!.width.toFloat())
        currentMover.interpolator = sLinearInterpolator
        currentMover.duration = ToonGame.MEDIUM_DURATION

        // set next view visible, translate off to right, skew,
        // slide on in parallel, overshoot/wobble, unskew
        nextView!!.visibility = View.VISIBLE
        nextView.skewX = -.5f
        nextView.translationX = mContainer!!.width.toFloat()
        val nextMover = ObjectAnimator.ofFloat(nextView, View.TRANSLATION_X, 0f)
        nextMover.interpolator = sAccelerator
        nextMover.duration = ToonGame.MEDIUM_DURATION
        val nextSkewer = ObjectAnimator.ofFloat(nextView, "skewX", 0f)
        nextSkewer.interpolator = sOvershooter
        val moverSet = AnimatorSet()
        moverSet.playTogether(currentMover, nextMover)
        val fullSet = AnimatorSet()
        fullSet.playSequentially(currentSkewer, moverSet, nextSkewer)
        fullSet.addListener(object : AnimatorListenerAdapter() {
            /**
             * Notifies the end of the animation. First we set the "skewX" property of `currentView`
             * to 0, set its visibility to GONE, and set its TRANSLATION_X property to 0. Then if our
             * parameter `endAction` is not null we call its `run` method.
             *
             * @param animation The animation which reached its end.
             */
            override fun onAnimationEnd(animation: Animator) {
                currentView!!.skewX = 0f
                currentView.visibility = View.GONE
                currentView.translationX = 0f
                endAction?.run()
            }
        })
        fullSet.start()
    }

    companion object {
        /**
         * The instance of `AccelerateInterpolator` we use.
         */
        private val sAccelerator = AccelerateInterpolator()

        /**
         * The instance of `LinearInterpolator` we use.
         */
        private val sLinearInterpolator = LinearInterpolator()

        /**
         * Name choosing state of the UI.
         */
        private const val NAME_STATE = 0

        /**
         * Difficulty choosing state of the UI.
         */
        private const val DIFFICULTY_STATE = 1

        /**
         * Credit card entering state of the UI.
         */
        private const val CREDIT_STATE = 2

        /**
         * The `OvershootInterpolator` we use to animate the "skewX" property when skewing between
         * the current and new `SkewableTextView` in our `slideToNext` method.
         */
        private val sOvershooter: TimeInterpolator = OvershootInterpolator()

        /**
         * `DecelerateInterpolator` we use for several different animations.
         */
        private val sDecelerator = DecelerateInterpolator()
    }
}
