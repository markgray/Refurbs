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

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This example shows how to create custom Window animations to animate between different
 * sub-activities.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at [...](https://www.youtube.com/watch?v=Ho8vk61lVIU)
 */
class WindowAnimations : ComponentActivity() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * and set our content view to our layout file `R.layout.activity_window_animations`. We
     * initialize [Button] variable `val defaultButton` by finding the view with id `R.id.defaultButton`
     * ("Default Animations"), initialize [Button] variable `val translateButton` by finding the view
     * with id `R.id.translateButton` ("Translate Animations") initialize [Button] variable
     * `val scaleButton` by finding the view with id `R.id.scaleButton` ("Scale Animations"), and
     * initialize [ImageView] variable `val thumbnail` by finding the view with id `R.id.thumbnail`
     * (displays the thumbnail drawable-nodpi/thumbnail.png). We set the [View.OnClickListener] of
     * `defaultButton` to an anonymous class whose [View.OnClickListener.onClick] override just
     * creates an [Intent] to launch the activity [SubActivity] and starts it running (relying on
     * the default transition animations). We set the [View.OnClickListener] of `translateButton` to
     * an anonymous class whose [View.OnClickListener.onClick] override creates an [Intent] to
     * launch [AnimatedSubActivity], then creates a [Bundle] variable `val translateBundle` from an
     * [ActivityOptions] instance which specifies the use of the xml animation with resource id
     * `R.anim.slide_in_left` as the animation to use for the incoming activity, and
     * `R.anim.slide_out_left` as the animation to use for the outgoing activity, and finally
     * launches the [Intent] variable `subActivity` with `translateBundle` as the option bundle.
     * We set the [View.OnClickListener] of `scaleButton` to an anonymous class whose
     * [View.OnClickListener.onClick] override creates an `Intent` to launch [AnimatedSubActivity],
     * then creates a [Bundle] variable `val scaleBundle` from an [ActivityOptions] instance which
     * specifies a scale up animation from the [View] parameter `v`  that was clicked with a start X
     * and Y of 0 to the width and height of `v`, and finally launches the [Intent] variable
     * `subActivity` with `scaleBundle` as the option bundle. We set the [View.OnClickListener] of
     * `thumbnail` to an anonymous class whose [View.OnClickListener.onClick] override initializes
     * [BitmapDrawable] variable `val drawable` with the current drawable of [ImageView] variable
     * `thumbnail`, initializes [Bitmap] variable `val bm` with the bitmap used to render `drawable`,
     * creates an [Intent] variable `val subActivity` to launch [AnimatedSubActivity], then creates
     * a [Bundle] variable `val scaleBundle` from an [ActivityOptions] instance which specifies a
     * thumbnail scale up animation from `thumbnail`, using `bm` as the bitmap that will be shown as
     * the initial thumbnail of the animation, with the starting point relative to the source being
     * (0,0), and finally launches the [Intent] variable `subActivity` with `scaleBundle` as the
     * option bundle.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_animations)
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
        val defaultButton: Button = findViewById(R.id.defaultButton)
        val translateButton: Button = findViewById(R.id.translateButton)
        val scaleButton: Button = findViewById(R.id.scaleButton)
        val thumbnail: ImageView = findViewById(R.id.thumbnail)

        // By default, launching a sub-activity uses the system default for window animations
        defaultButton.setOnClickListener { v: View? ->
            val subActivity = Intent(
                /* packageContext = */ this@WindowAnimations,
                /* cls = */ SubActivity::class.java
            )
            startActivity(subActivity)
        }

        // Custom animations allow us to do things like slide the next activity in as we
        // slide this activity out
        translateButton.setOnClickListener { v: View? ->
            // Using the AnimatedSubActivity also allows us to animate exiting that
            // activity - see that activity for details
            val subActivity = Intent(
                /* packageContext = */ this@WindowAnimations,
                /* cls = */ AnimatedSubActivity::class.java
            )
            // The enter/exit animations for the two activities are specified by xml resources
            val translateBundle: Bundle = ActivityOptions
                .makeCustomAnimation(
                    /* context = */ this@WindowAnimations,
                    /* enterResId = */ R.anim.slide_in_left,
                    /* exitResId = */ R.anim.slide_out_left
                ).toBundle()
            startActivity(subActivity, translateBundle)
        }

        // Starting in Jellybean, you can provide an animation that scales up the new
        // activity from a given source rectangle
        scaleButton.setOnClickListener { v: View ->
            val subActivity = Intent(
                /* packageContext = */ this@WindowAnimations,
                /* cls = */ AnimatedSubActivity::class.java
            )
            val scaleBundle: Bundle = ActivityOptions
                .makeScaleUpAnimation(
                    /* source = */ v,
                    /* startX = */ 0,
                    /* startY = */ 0,
                    /* width = */ v.width,
                    /* height = */ v.height
                ).toBundle()
            startActivity(subActivity, scaleBundle)
        }

        // Starting in Jellybean, you can also provide an animation that scales up the new
        // activity from a given bitmap, cross-fading between the starting and ending
        // representations. Here, we scale up from a thumbnail image of the final sub-activity
        thumbnail.setOnClickListener { v: View? ->
            val drawable = thumbnail.drawable as BitmapDrawable
            val bm: Bitmap = drawable.bitmap
            val subActivity = Intent(
                /* packageContext = */ this@WindowAnimations,
                /* cls = */ AnimatedSubActivity::class.java
            )
            val scaleBundle: Bundle = ActivityOptions
                .makeThumbnailScaleUpAnimation(
                    /* source = */ thumbnail,
                    /* thumbnail = */ bm,
                    /* startX = */ 0,
                    /* startY = */ 0
                ).toBundle()
            startActivity(subActivity, scaleBundle)
        }
    }
}
