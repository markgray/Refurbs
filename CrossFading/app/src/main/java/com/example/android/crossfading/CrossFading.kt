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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.example.android.crossfading

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

/**
 * This example shows how to use [TransitionDrawable] to perform a simple cross-fade effect
 * between two drawables.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=atH3o2uh_94
 */
class CrossFading : ComponentActivity() {
    /**
     * Which layer of the [TransitionDrawable] is currently being
     * displayed -- 0 is the first layer, 1 is the second layer.
     */
    var mCurrentDrawable: Int = 0

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.activity_cross_fading`. We initialize
     * [ImageView] variable `val imageView` by finding the view with id `R.id.image_view`. We
     * initialize both [Bitmap] variable `val bitmap0` and [Bitmap] variable `val bitmap1` with 500
     * by 500 pixel bitmaps with a config of ARGB_8888. We initialize [Canvas] variable `var canvas`
     * with an instance that will draw into `bitmap0` then call its [Canvas.drawColor] method to fill
     * its entire bitmap with the color [Color.RED] using `srcover` porterduff mode. We then set
     * `canvas` to a new instance that will draw into `bitmap1` and call its [Canvas.drawColor]
     * method to fill its entire bitmap with the color [Color.GREEN]. We allocate 2 entries to
     * initialize [Array] of [BitmapDrawable] `val drawables`, then set `drawables[0]` to an instance
     * created from `bitmap0`, and `drawables[1]` to an instance created from `bitmap1`. We
     * initialize [TransitionDrawable] variable `val crossfader` with an instance created from
     * `drawables` then set the content of `imageView` to it. Finally we set the [OnClickListener]
     * of `imageView` to an anonymous class whose [OnClickListener.onClick] override branches on the
     * value of [Int] field [mCurrentDrawable]:
     *
     *  * 0: calls the [TransitionDrawable.startTransition] method of `crossfader` with a duration
     *  of 500 milliseconds  to transition the second layer on top of the first layer (taking 500ms
     *  to do so), then sets [mCurrentDrawable] to 1.
     *
     *  * 1: calls the [TransitionDrawable.reverseTransition] method of `crossfader` with a duration
     *  of 500 milliseconds to reverse the transition, picking up where the transition currently is
     *  (taking 500ms to do so), then sets [mCurrentDrawable] to 0.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cross_fading)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
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
        val imageView = findViewById<ImageView>(R.id.image_view)

        // Create red and green bitmaps to cross-fade between
        val bitmap0 = createBitmap(width = 500, height = 500)
        val bitmap1 = createBitmap(width = 500, height = 500)
        var canvas = Canvas(bitmap0)
        canvas.drawColor(Color.RED)
        canvas = Canvas(bitmap1)
        canvas.drawColor(Color.GREEN)
        val drawables: Array<BitmapDrawable?> = arrayOfNulls(size = 2)
        drawables[0] = bitmap0.toDrawable(resources = resources)
        drawables[1] = bitmap1.toDrawable(resources = resources)

        // Add the red/green bitmap drawables to a TransitionDrawable. They are layered
        // in the transition drawable. The cross-fade effect happens by fading one out and the
        // other in.
        val crossfader = TransitionDrawable(drawables)
        imageView.setImageDrawable(crossfader)

        // Clicking on the drawable will cause the cross-fade effect to run. Depending on
        // which drawable is currently being shown, we either 'start' or 'reverse' the
        // transition, which determines which drawable is faded out/in during the transition.
        imageView.setOnClickListener {
            mCurrentDrawable = if (mCurrentDrawable == 0) {
                crossfader.startTransition(500)
                1
            } else {
                crossfader.reverseTransition(500)
                0
            }
        }
    }
}
