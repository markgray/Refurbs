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
package com.example.android.displayingbitmaps.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.widget.ImageView
import com.example.android.displayingbitmaps.util.RecyclingBitmapDrawable

/**
 * Sub-class of ImageView which automatically notifies the drawable when it is
 * being displayed.
 */
@SuppressLint("AppCompatCustomView")
class RecyclingImageView : ImageView {
    /**
     * Our one argument constructor, just calls our super's constructor.
     *
     * @param context `Context` to use to access resources
     */
    constructor(context: Context?) : super(context)

    /**
     * Perform inflation from XML. We just call our super's constructor.
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * This is called when the view is detached from a window.  At this point it
     * no longer has a surface for drawing. We call our `setImageDrawable`
     * method to clear the drawable, then call our super's implementation of
     * `onDetachedFromWindow`
     *
     * @see android.widget.ImageView.onDetachedFromWindow
     */
    override fun onDetachedFromWindow() {
        // This has been detached from Window, so clear the drawable
        setImageDrawable(null)
        super.onDetachedFromWindow()
    }

    /**
     * Sets a drawable as the content of this ImageView. First we save a reference to the present
     * drawable in `Drawable previousDrawable`, then we call our super's implementation of
     * `setImageDrawable` to set our parameter `Drawable drawable` as the content of
     * this ImageView. We then call our method `notifyDrawable` to notify `drawable`
     * that it is being displayed, and call it again to notify `previousDrawable` that it is
     * no longer being displayed.
     *
     * @param drawable the Drawable to set, or `null` to clear the
     * content
     */
    override fun setImageDrawable(drawable: Drawable?) {
        // Keep hold of previous Drawable
        val previousDrawable = getDrawable()

        // Call super to set new Drawable
        super.setImageDrawable(drawable)

        // Notify new Drawable that it is being displayed
        notifyDrawable(drawable, true)

        // Notify old Drawable so it is no longer being displayed
        notifyDrawable(previousDrawable, false)
    }

    companion object {
        /**
         * Notifies the drawable that it's displayed state has changed. We branch based on the class of
         * our parameter `Drawable drawable`:
         *
         *  *
         * `RecyclingBitmapDrawable`: We call the `setIsDisplayed` method of `drawable`
         * with our parameter `isDisplayed` (This is only used on devices older than HONEYCOMB)
         *
         *  *
         * `LayerDrawable`: We cast `drawable` to get `LayerDrawable layerDrawable`,
         * then for each of the layers (2) in `layerDrawable` we call this method on the drawable
         * of that layer and our parameter `isDisplayed`.
         *
         *
         *
         * @param drawable    `Drawable` to notify
         * @param isDisplayed flag indicating whether the drawable is displayed (true) or not (false)
         */
        private fun notifyDrawable(drawable: Drawable?, isDisplayed: Boolean) {
            if (drawable is RecyclingBitmapDrawable) {
                // The drawable is a CountingBitmapDrawable, so notify it
                drawable.setIsDisplayed(isDisplayed)
            } else if (drawable is LayerDrawable) {
                // The drawable is a LayerDrawable, so recurse on each layer
                @Suppress("UnnecessaryVariable")
                val layerDrawable = drawable
                var i = 0
                val z = layerDrawable.numberOfLayers
                while (i < z) {
                    notifyDrawable(layerDrawable.getDrawable(i), isDisplayed)
                    i++
                }
            }
        }
    }
}