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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "MemberVisibilityCanBePrivate")

package com.example.android.pictureviewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.graphics.drawable.toDrawable

/**
 * This example shows how to use [ViewPropertyAnimator] to get a cross-fade effect as new
 * bitmaps get installed in an ImageView.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [...](https://www.youtube.com/watch?v=9XbKMUtVnJA).
 */
class PictureViewer : ComponentActivity() {
    /**
     * Index into the [IntArray] field [drawableIDs] array of resource ids for drawables, it is
     * incremented round robin every time the [ImageView] is clicked and points to the resource id
     * of the jpg that is currently being displayed in that [ImageView]
     */
    var mCurrentDrawable: Int = 0

    /**
     * Array of resource ids for the jpg images that are displayed in our [ImageView]
     */
    var drawableIDs: IntArray = intArrayOf(
        R.drawable.p1,
        R.drawable.p2,
        R.drawable.p3,
        R.drawable.p4
    )

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_picture_viewer` (this is a [FrameLayout] containing two
     * [ImageView] widgets occupying the same space).
     *
     * We initialize our [FrameLayout] variable `rootView` to the view with ID
     * `R.id.root_view` then call [ViewCompat.setOnApplyWindowInsetsListener] to
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
     * We initialize [ImageView] variable `val prevImageView` by finding the view with id
     * `R.id.prevImageView`, and [ImageView] variable `val nextImageView` by finding the view
     * with id `R.id.nextImageView`, and set the background color of both to [Color.TRANSPARENT].
     * We also set the duration of both view's [ViewPropertyAnimator] to 1000ms. We initialize the
     * [BitmapDrawable] variable `val drawables` array with an instance which will hold as many
     * [BitmapDrawable] objects as there are entries in our [IntArray] field [drawableIDs]. We then
     * loop over [Int] variable `var i` for these entries creating [Bitmap] variable `val bitmap`
     * by decoding the jpg whose resource id is at index `i` in [drawableIDs] and setting the
     * contents of the `i`th entry in `drawables` to the [BitmapDrawable] created from `bitmap`.
     * When done loading up `drawables` we set the content of `prevImageView` to `drawables` at
     * index 0 and the content of `nextImageView` to `drawables` at index 1. Finally we set the
     * [View.OnClickListener] of `prevImageView` to an anonymous instance whose
     * [View.OnClickListener.onClick] override animates the alpha of `prevImageView` to 0 and the
     * alpha of `nextImageView` to 1 with an end action consisting of an anonymous [Runnable] whose
     * [Runnable.run] override sets the content of `prevImageView` and `nextImageView` to the next
     * round robin [BitmapDrawable] they should display, and sets the alpha of `nextImageView` to 0
     * and the alpha of `prevImageView` to 1.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_viewer)
        val rootView = findViewById<FrameLayout>(R.id.root_view)
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

        // This app works by having two views, which get faded in/out for the cross-fade effect
        val prevImageView = findViewById<ImageView>(R.id.prevImageView)
        val nextImageView = findViewById<ImageView>(R.id.nextImageView)
        prevImageView.setBackgroundColor(Color.TRANSPARENT)
        nextImageView.setBackgroundColor(Color.TRANSPARENT)

        // Setup default ViewPropertyAnimator durations for the two ImageViews
        prevImageView.animate().duration = 1000
        nextImageView.animate().duration = 1000

        // NOte that a real app would do this more robustly, and not just load all possible
        // bitmaps at onCreate() time.
        val drawables = arrayOfNulls<BitmapDrawable>(drawableIDs.size)
        for (i in drawableIDs.indices) {
            val bitmap: Bitmap = BitmapFactory.decodeResource(resources, drawableIDs[i])
            drawables[i] = bitmap.toDrawable(resources = resources)
        }
        prevImageView.setImageDrawable(drawables[0])
        nextImageView.setImageDrawable(drawables[1])
        prevImageView.setOnClickListener { _: View? ->
            // Use ViewPropertyAnimator to fade the previous imageView out and the next one in
            prevImageView.animate().alpha(0f).withLayer()
            // When the animation ends, set up references to change the prev/next
            // associations
            nextImageView.animate().alpha(1f).withLayer().withEndAction {
                mCurrentDrawable = (mCurrentDrawable + 1) % drawables.size
                val nextDrawableIndex = (mCurrentDrawable + 1) % drawables.size
                prevImageView.setImageDrawable(drawables[mCurrentDrawable])
                nextImageView.setImageDrawable(drawables[nextDrawableIndex])
                nextImageView.alpha = 0f
                prevImageView.alpha = 1f
            }
        }
    }
}
