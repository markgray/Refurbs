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
@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.example.android.windowanimations

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView

/**
 * This example shows how to create custom Window animations to animate between different
 * sub-activities.
 *
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [...](https://www.youtube.com/watch?v=Ho8vk61lVIU)
 */
class WindowAnimations : Activity() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * and set our content view to our layout file R.layout.activity_window_animations. We initialize
     * `Button defaultButton` by finding the view with id R.id.defaultButton ("Default Animations"),
     * initialize `Button translateButton` by finding the view with id R.id.translateButton ("Translate Animations")
     * initialize `Button scaleButton` by finding the view with id R.id.scaleButton ("Scale Animations"),
     * and initialize `ImageView thumbnail` by finding the view with id R.id.thumbnail (displays the thumbnail
     * drawable-nodpi/thumbnail.png). We set the `OnClickListener` of `defaultButton` to an anonymous
     * class whose `onClick` override just creates an `Intent` to launch the activity `SubActivity`
     * and starts it running (relying on the default transition animations). We set the `OnClickListener` of
     * `translateButton` to an anonymous class whose `onClick` override creates an `Intent` to
     * launch `AnimatedSubActivity`, then creates a `Bundle translateBundle` from an `ActivityOptions`
     * instance which specifies the use of the xml animation with resource id R.anim.slide_in_left as the animation to
     * use for the incoming activity, and R.anim.slide_out_left as the animation to use for the outgoing activity,
     * and finally launches the `Intent subActivity` with `translateBundle` as the option bundle. We set
     * the `OnClickListener` of `scaleButton` to an anonymous class whose `onClick` override creates
     * an `Intent` to launch `AnimatedSubActivity`, then creates a `Bundle scaleBundle` from an
     * `ActivityOptions` instance which specifies a scale up animation from the `View v` that was clicked
     * with a start X and Y of 0 to the width and height of `v`, and finally launches the `Intent subActivity`
     * with `scaleBundle` as the option bundle. We set the `OnClickListener` of `thumbnail` to an
     * anonymous class whose `onClick` override initializes `BitmapDrawable drawable` with the current
     * drawable of `ImageView thumbnail`, initializes `Bitmap bm` with the bitmap used to render `drawable`,
     * creates an `Intent subActivity` to launch `AnimatedSubActivity`, then creates a `Bundle scaleBundle`
     * from an `ActivityOptions` instance which specifies a thumbnail scale up animation from `thumbnail`, using
     * `bm` as the bitmap that will be shown as the initial thumbnail of the animation, with the starting point relative
     * to the source being (0,0), and finally launches the `Intent subActivity` with `scaleBundle` as the option
     * bundle.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_animations)
        val defaultButton = findViewById<Button>(R.id.defaultButton)
        val translateButton = findViewById<Button>(R.id.translateButton)
        val scaleButton = findViewById<Button>(R.id.scaleButton)
        val thumbnail = findViewById<ImageView>(R.id.thumbnail)

        // By default, launching a sub-activity uses the system default for window animations
        defaultButton.setOnClickListener { v: View? ->
            val subActivity = Intent(this@WindowAnimations, SubActivity::class.java)
            startActivity(subActivity)
        }

        // Custom animations allow us to do things like slide the next activity in as we
        // slide this activity out
        translateButton.setOnClickListener { v: View? ->
            // Using the AnimatedSubActivity also allows us to animate exiting that
            // activity - see that activity for details
            val subActivity = Intent(this@WindowAnimations, AnimatedSubActivity::class.java)
            // The enter/exit animations for the two activities are specified by xml resources
            val translateBundle = ActivityOptions
                .makeCustomAnimation(this@WindowAnimations,
                    R.anim.slide_in_left,
                    R.anim.slide_out_left)
                .toBundle()
            startActivity(subActivity, translateBundle)
        }

        // Starting in Jellybean, you can provide an animation that scales up the new
        // activity from a given source rectangle
        scaleButton.setOnClickListener { v: View ->
            val subActivity = Intent(this@WindowAnimations, AnimatedSubActivity::class.java)
            val scaleBundle = ActivityOptions
                .makeScaleUpAnimation(v, 0, 0,
                    v.width, v.height)
                .toBundle()
            startActivity(subActivity, scaleBundle)
        }

        // Starting in Jellybean, you can also provide an animation that scales up the new
        // activity from a given bitmap, cross-fading between the starting and ending
        // representations. Here, we scale up from a thumbnail image of the final sub-activity
        thumbnail.setOnClickListener { v: View? ->
            val drawable = thumbnail.drawable as BitmapDrawable
            val bm = drawable.bitmap
            val subActivity = Intent(this@WindowAnimations, AnimatedSubActivity::class.java)
            val scaleBundle = ActivityOptions
                .makeThumbnailScaleUpAnimation(thumbnail, bm, 0, 0)
                .toBundle()
            startActivity(subActivity, scaleBundle)
        }
    }
}