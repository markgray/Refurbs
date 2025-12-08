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

package com.example.squashandstretch

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This example shows how to add some life to a view during animation by deforming the shape.
 * As the button "falls", it stretches along the line of travel. When it hits the bottom, it
 * squashes, like a real object when hitting a surface. Then the button reverses these actions
 * to bounce back up to the start.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the android developers channel on YouTube at
 * [...](https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0)
 * [...](https://www.youtube.com/watch?v=wJL1oW6DlCc)
 */
class SquashAndStretch : ComponentActivity() {
    /**
     * The [RelativeLayout] in our layout file with id `R.id.container` which contains our button.
     */
    private var mContainer: ViewGroup? = null

    /**
     * Scale value for the duration of our animations, either 1 or 5 set by our option menu checkbox.
     */
    private var sAnimatorScale: Long = 1

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable
     * edge to edge display, then we call our super's implementation of `onCreate`, and
     * set our content view to our layout file `R.layout.main`.
     *
     * We initialize our [LinearLayout] variable `rootView` to the view with ID
     * `R.id.container` then call [ViewCompat.setOnApplyWindowInsetsListener] to
     * take over the policy for applying window insets to `rootView`, with the
     * `listener` argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * Finally we initialize
     * our [ViewGroup] field [mContainer] to our [LinearLayout] variable `rootView` (the view with
     * id `R.id.container` recall).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val rootView = findViewById<LinearLayout>(R.id.container)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        mContainer = rootView
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We retrieve a [MenuInflater]
     * for our context and use it to inflate our menu layout file `R.menu.main` into our [Menu]
     * parameter [menu], then return `true` so that our menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(/* menuRes = */ R.menu.main, /* menu = */ menu)
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected. If the
     * [MenuItem.getItemId] method (kotlin `itemId` property) of our [MenuItem] parameter [item]
     * is `R.id.menu_slow` we set our [Long] field [sAnimatorScale] to 1 if that item is currently
     * checked, or to 5 if it is unchecked, then we toggle the checked state of [item]. In any case
     * we return the value returned by our super's implementation of `onOptionsItemSelected`
     * to the caller.
     *
     * @param item The menu item that was selected.
     * @return [Boolean] Return `false` to allow normal menu processing to
     * proceed, `true` to consume it here.
     *
     * @see onCreateOptionsMenu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_slow) {
            sAnimatorScale = (if (item.isChecked) 1 else 5).toLong()
            item.isChecked = !item.isChecked
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Specified to be called when the "Click Me!" [Button] in our layout file is clicked by an
     * android:onClick="onButtonClick" attribute. We animate the fall of the button from the top of
     * the screen to the bottom with appropriate deformation of the button. We initialize our [Long]
     * variable `val animationDuration` to [BASE_DURATION] times our [Long] field [sAnimatorScale].
     * We set the X location of the point around which our [View] parameter [view] is scaled to the
     * middle of the view, and the Y location of the point to the height of the view. We initialize
     * [PropertyValuesHolder] variable `var pvhTY` with an instance that will animate the
     * [View.TRANSLATION_Y] property to the height of our [ViewGroup] field [mContainer] minus the
     * height of our [View] parameter [view], initialize [PropertyValuesHolder] variable `var pvhSX`
     * to an instance which will animate the [View.SCALE_X] property to .7f, and initialize
     * [PropertyValuesHolder] variable `var pvhSY` to an instance which will animate the
     * [View.SCALE_Y] property to 1.2f, then initialize [ObjectAnimator] variable `val downAnim`
     * to an instance which will apply `pvhTY`, `pvhSX`, and `pvhSY` to [view], set its
     * interpolator to [AccelerateInterpolator] field [sAccelerator]` and set its duration to 2
     * times `animationDuration`.
     *
     * We then set `pvhSX` to an instance which will animate the [View.SCALE_X] property to 2, and
     * `pvhSY` to an instance which will animate the [View.SCALE_Y] property to .5f, initialize
     * [ObjectAnimator] variable `val stretchAnim` to an instance which will apply `pvhSX`, and
     * `pvhSY` to [view], set its repeat count to 1, its repeat mode to [ValueAnimator.REVERSE],
     * its interpolator to [DecelerateInterpolator] field [sDecelerator] and its duration to
     * `animationDuration`.
     *
     * We then set `pvhTY` to an instance which will animate the [View.TRANSLATION_Y] property to 0,
     * `pvhSX` to an instance which will animate the [View.SCALE_X] property to 1, and `pvhSY` to an
     * instance which will animate the [View.SCALE_Y] property to 1, then initialize [ObjectAnimator]
     * variable `val upAnim` to an instance which will apply `pvhTY`, `pvhSX`, and `pvhSY` to [view],
     * set its duration to 2 times `animationDuration`, and set its interpolator to [DecelerateInterpolator]
     * field [sDecelerator].
     *
     * We then initialize [AnimatorSet] variable `val set` with a new instance, set it to play
     * sequentially `downAnim`, `stretchAnim`, and `upAnim` and then start it running.
     *
     * @param view the [View] that was clicked.
     */
    fun onButtonClick(view: View) {
        val animationDuration = BASE_DURATION * sAnimatorScale

        // Scale around bottom/middle to simplify squash against the window bottom
        view.pivotX = view.width / 2f
        view.pivotY = view.height.toFloat()

        // Animate the button down, accelerating, while also stretching in Y and squashing in X
        var pvhTY = PropertyValuesHolder.ofFloat(
            /* property = */ View.TRANSLATION_Y,
            /* ...values = */ (mContainer!!.height - view.height).toFloat()
        )
        var pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, .7f)
        var pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
        val downAnim = ObjectAnimator.ofPropertyValuesHolder(view, pvhTY, pvhSX, pvhSY)
        downAnim.interpolator = sAccelerator
        downAnim.duration = animationDuration * 2

        // Stretch in X, squash in Y, then reverse
        pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 2f)
        pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, .5f)
        val stretchAnim = ObjectAnimator.ofPropertyValuesHolder(view, pvhSX, pvhSY)
        stretchAnim.repeatCount = 1
        stretchAnim.repeatMode = ValueAnimator.REVERSE
        stretchAnim.interpolator = sDecelerator
        stretchAnim.duration = animationDuration

        // Animate back to the start
        pvhTY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f)
        pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f)
        pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
        val upAnim = ObjectAnimator.ofPropertyValuesHolder(view, pvhTY, pvhSX, pvhSY)
        upAnim.duration = animationDuration * 2
        upAnim.interpolator = sDecelerator
        val set = AnimatorSet()
        set.playSequentially(downAnim, stretchAnim, upAnim)
        set.start()
    }

    companion object {
        /**
         * The [AccelerateInterpolator] we use for the down animation
         */
        private val sAccelerator = AccelerateInterpolator()

        /**
         * The [DecelerateInterpolator] we use for the stretch and up animations.
         */
        private val sDecelerator = DecelerateInterpolator()

        /**
         * Base multiplier for the duration of our animations.
         */
        private const val BASE_DURATION: Long = 300
    }
}
