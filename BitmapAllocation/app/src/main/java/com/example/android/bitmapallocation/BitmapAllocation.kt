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

package com.example.android.bitmapallocation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.graphics.createBitmap

/**
 * This example shows how to speed up bitmap loading and reduce garbage collection
 * by reusing existing bitmaps.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com,
 * or on YouTube at [...](https://www.youtube.com/watch?v=rsQet4nBVi8).
 */
class BitmapAllocation : AppCompatActivity() {
    // There are some assumptions in this demo app that don't carry over well to the real world:
    // it assumes that all bitmaps are the same size and that loading all bitmaps as the activity
    // starts is good enough. A real application would be take a more flexible and robust
    // approach. But these assumptions are good enough for the purposes of this tutorial,
    // which is about reusing existing bitmaps of the same size.
    /**
     * The current index into our array of image ids.
     */
    var mCurrentIndex: Int = 0

    /**
     * The current bitmap being displayed.
     */
    var mCurrentBitmap: Bitmap? = null

    /**
     * Bitmap options we use for all [BitmapFactory] calls.
     */
    var mBitmapOptions: BitmapFactory.Options? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.activity_bitmap_allocation`.
     *
     * We initialize our [LinearLayout] variable `rootView`
     * to the view with ID `R.id.root_view` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
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
     * We initialize our [IntArray] variable `val imageIDs` with the resource id's of the
     * 6 jpg's we display, initialize [CheckBox] variable `val checkbox` by finding the view
     * in our layout with id `R.id.checkbox` ("Reuse Bitmap" -- if checked the old bitmap will
     * be reused), initialize [TextView] variable `val durationTextview` by finding the view
     * with id `R.id.loadDuration` (we will display the time it took to decode and display our
     * images here), and initialize [ImageView] variable `val imageview` by finding the view
     * with id `R.id.imageview` (we will display our images here). We initialize our
     * [BitmapFactory.Options] field [mBitmapOptions] with a new instance, and set its
     * `inJustDecodeBounds` field to `true` (the decoder will return `null` (no bitmap), but
     * the size fields of [mBitmapOptions] will still be set, allowing the caller to query the
     * bitmap without having to allocate the memory for its pixels). We then call the
     * [BitmapFactory.decodeResource] method to set the fields of [mBitmapOptions] given the jpg
     * with resource id `R.drawable.a`. We then create a bitmap for [Bitmap] field [mCurrentBitmap]
     * using the `outWidth` field of [mBitmapOptions] as the width of the bitmap and the
     * `outHeight` field as the height of the bitmap, using ARGB_8888 as the bitmap configuration
     * (each pixel is stored in 4 bytes, with each channel (RGB and alpha for translucency) stored
     * with 8 bits of precision). We then set the `inJustDecodeBounds` field of [mBitmapOptions] to
     * `false`, set its `inBitmap` field to [mCurrentBitmap] (decode methods that take the
     * `Options` object will attempt to reuse this bitmap when loading content), and set its
     * `inSampleSize` field to 1 (no sub-sampling). We then call the `decodeResource` method of
     * [BitmapFactory] to decode the jpg with resource id `R.drawable.a` into [Bitmap] field
     * [mCurrentBitmap], then set the content of [ImageView] variable `imageview` to it.
     * Finally we set the [View.OnClickListener] of `imageview` to an anonymous class which cycles
     * through the resource id's in `imageIDs` displaying each jpg in turn and reusing
     * [mCurrentBitmap] if the checkbox `checkbox` is checked.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    @SuppressLint("SetTextI18n")
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitmap_allocation)
        val rootView = findViewById<LinearLayout>(R.id.root_view)
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
        val imageIDs: IntArray = intArrayOf(R.drawable.a, R.drawable.b, R.drawable.c, R.drawable.d,
            R.drawable.e, R.drawable.f)
        val checkbox: CheckBox = findViewById(R.id.checkbox)
        val durationTextview: TextView = findViewById(R.id.loadDuration)
        val imageview: ImageView = findViewById(R.id.imageview)

        // Create bitmap to be re-used, based on the size of one of the bitmaps
        mBitmapOptions = BitmapFactory.Options()
        mBitmapOptions!!.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, R.drawable.a, mBitmapOptions)
        mCurrentBitmap = createBitmap(mBitmapOptions!!.outWidth, mBitmapOptions!!.outHeight)
        mBitmapOptions!!.inJustDecodeBounds = false
        mBitmapOptions!!.inBitmap = mCurrentBitmap
        mBitmapOptions!!.inSampleSize = 1
        BitmapFactory.decodeResource(resources, R.drawable.a, mBitmapOptions)
        imageview.setImageBitmap(mCurrentBitmap)

        // When the user clicks on the image, load the next one in the list
        imageview.setOnClickListener {
            mCurrentIndex = (mCurrentIndex + 1) % imageIDs.size
            var bitmapOptions: BitmapFactory.Options? = null
            if (checkbox.isChecked) {
                // Re-use the bitmap by using BitmapOptions.inBitmap
                bitmapOptions = mBitmapOptions
                bitmapOptions!!.inBitmap = mCurrentBitmap
            }
            val startTime = System.currentTimeMillis()
            mCurrentBitmap = BitmapFactory.decodeResource(resources,
                imageIDs[mCurrentIndex], bitmapOptions)
            imageview.setImageBitmap(mCurrentBitmap)

            // One way you can see the difference between reusing and not is through the
            // timing reported here. But you can also see a huge impact in the garbage
            // collector if you look at logcat with and without reuse. Avoiding garbage
            // collection when possible, especially for large items like bitmaps,
            // is always a good idea.
            durationTextview.text = "Load took " +
                (System.currentTimeMillis() - startTime)
        }
    }
}
