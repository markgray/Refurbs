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

package com.example.android.bitmapscaling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * This example shows how the use of [BitmapFactory.Options] affects the resulting size of a loaded
 * bitmap. Sub-sampling can speed up load times and reduce the need for large bitmaps in memory if
 * your target bitmap size is much smaller, although it's good to understand that you can't get
 * specific [Bitmap] sizes, but rather power-of-two reductions in sizes.
 */
class BitmapScaling : AppCompatActivity() {
    /**
     * The [TextView] we use to display the size of the [Bitmap] for different scaling
     */
    var sizeOfBitmap: TextView? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_bitmap_scaling`.
     *
     * We initialize our [LinearLayout] variable `rootView`
     * to the view with ID `R.id.root_view` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `insets.left`, the right margin set to
     * `insets.right`, the top margin set to `insets.top`, and the bottom margin
     * set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We initialize [LinearLayout] variable `val container` by finding the view with id
     * `R.id.scaledImageContainer` (we will place our scaled images in this [ViewGroup]), and
     * [ImageView] variable `val finding the view with id `R.id.originalImageHolder` (we will place
     * our full sized bitmap here). We initialize our [TextView] field [sizeOfBitmap] by finding the
     * view with id `R.id.size_of_bitmap` (we will list the size of the bitmaps created here).
     *
     * We then create [Bitmap] variable `val bitmap` by decoding the jpg with resource id
     * `R.drawable.jellybean_statue` and set it to be the content of `originalImageView`. We append
     * text to [sizeOfBitmap] reporting the size of `bitmap`. Then for [Int] `i` from 2 to 9 we call
     * our method [addScaledImageView] to add a bitmap scaled from the jpg we just using a scaling
     * factor of `i` into `container`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitmap_scaling)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
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
        val container: LinearLayout = findViewById(R.id.scaledImageContainer)
        val originalImageView: ImageView = findViewById(R.id.originalImageHolder)
        sizeOfBitmap = findViewById(R.id.size_of_bitmap)
        val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.jellybean_statue)
        originalImageView.setImageBitmap(bitmap)
        sizeOfBitmap!!.append("Size of bitmap for no scaling ${bitmap.byteCount}\n")
        for (i in 2..9) {
            addScaledImageView(i, container)
        }
    }

    /**
     * Scales the jpg with resource id `R.drawable.jellybean_statue` by the parameter [sampleSize]
     * and adds it to the [LinearLayout] parameter [container] . We initialize our
     * [BitmapFactory.Options] variable `val bitmapOptions` with a new instance and set its
     * `inSampleSize` field to our parameter [sampleSize]. We then initialize [Bitmap] variable
     * `val scaledBitmap` by decoding the jpg with resource id `R.drawable.jellybean_statue` into it
     * using `bitmapOptions` for the bitmap options. We then append text to [sizeOfBitmap] reporting
     * the size of `scaledBitmap`. We initialize [ImageView] variable `val scaledImageView` with a
     * new instance, set its layout parameters to use WRAP_CONTENT for both width and height, set
     * its content to `scaledBitmap`, and finally add it to our parameter [container].
     *
     * @param sampleSize scaling factor to use to sub-sample `original`
     * @param container [ViewGroup] we are to add our sub-sampled bitmap to
     */
    private fun addScaledImageView(sampleSize: Int, container: LinearLayout) {

        // inSampleSize tells the loader how much to scale the final image, which it does at
        // load time by simply reading less pixels for every pixel value in the final bitmap.
        // Note that it only scales by powers of two, so a value of two results in a bitmap
        // 1/2 the size of the original and a value of four results in a bitmap 1/4 the original
        // size. Intermediate values are rounded down, so a value of three results in a bitmap 1/2
        // the original size.
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inSampleSize = sampleSize
        val scaledBitmap = BitmapFactory.decodeResource(resources,
            R.drawable.jellybean_statue, bitmapOptions)
        sizeOfBitmap!!.append("Size of bitmap with scaling of $sampleSize ${scaledBitmap.byteCount}\n")
        val scaledImageView = ImageView(this)
        scaledImageView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        scaledImageView.setImageBitmap(scaledBitmap)
        container.addView(scaledImageView)
    }
}
