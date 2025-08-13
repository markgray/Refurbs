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
package com.example.android.keyframeanimation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

/**
 * This example shows how to use [AnimationDrawable] to construct a keyframe animation where each
 * frame is shown for a specified duration.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [Key Frame Animations](https://www.youtube.com/watch?v=V3ksidLf7vA)
 */
class KeyframeAnimation : ComponentActivity() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.activity_keyframe_animation`, and
     * we initialize [ImageView] variable `val imageview` by finding the view with id `R.id.imageview`.
     * We initialize our [AnimationDrawable] variable `val animationDrawable` to a new instance then
     * loop for [Int] variable `var i` from 0 to 9 calling the [AnimationDrawable.addFrame] method
     * of `animationDrawable` to add the [BitmapDrawable] returned by our method
     * [getDrawableForFrameNumber] for `i` to it with a duration of 300ms. We call the
     * [AnimationDrawable.setOneShot] method (kotlin `isOneShot` property) of `animationDrawable`
     * with `false` so that it will run until it stops, and set the content of `imageview` to
     * `animationDrawable`. Finally we set the [OnClickListener] of `imageview` to an anonymous
     * class whose [OnClickListener.onClick] override stops `animationDrawable` if it is currently
     * running or starts it running if it is not running.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyframe_animation)
        val rootView = findViewById<RelativeLayout>(R.id.root_view)
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

        val imageview = findViewById<ImageView>(R.id.imageview)

        // Create the AnimationDrawable in which we will store all frames of the animation
        val animationDrawable = AnimationDrawable()
        for (i in 0..9) {
            animationDrawable.addFrame(getDrawableForFrameNumber(i), 300)
        }
        // Run until we say stop
        animationDrawable.isOneShot = false
        imageview.setImageDrawable(animationDrawable)

        // When the user clicks on the image, toggle the animation on/off
        imageview.setOnClickListener {
            if (animationDrawable.isRunning) {
                animationDrawable.stop()
            } else {
                animationDrawable.start()
            }
        }
    }

    /**
     * Creates a [BitmapDrawable] for the given frame number. The 'frames' in this app are
     * nothing more than a gray background with text indicating the number of the frame. First we
     * initialize [Bitmap] variable `val bitmap` with a 400 pixel by 400 pixel instance using the
     * config ARGB_8888. We initialize [Canvas] variable `val canvas` with an instance that will
     * draw into `bitmap` and use its [Canvas.drawColor] method to fill it with the color GRAY.
     * We initialize [Paint] variable `val paint` with a new instance with its ANTI_ALIAS_FLAG flag
     * set, set its text size to 80f, and set its color to BLACK. We call the [Canvas.drawText]
     * method of `canvas` to draw a string formed by concatenating the string "Frame " to the string
     * value of our [Int] parameter [frameNumber] at the location (40,220) using `paint` as the
     * paint. Finally we return a [BitmapDrawable] created from `bitmap` to the caller.
     *
     * @param frameNumber frame number we are creating
     * @return a [BitmapDrawable] consisting of a gray background with text indicating the number
     * of the frame.
     */
    private fun getDrawableForFrameNumber(frameNumber: Int): BitmapDrawable {
        val bitmap = createBitmap(width = 400, height = 400)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.GRAY)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = 80f
        paint.color = Color.BLACK
        canvas.drawText("Frame $frameNumber", 40f, 220f, paint)
        return bitmap.toDrawable(resources = resources)
    }
}
