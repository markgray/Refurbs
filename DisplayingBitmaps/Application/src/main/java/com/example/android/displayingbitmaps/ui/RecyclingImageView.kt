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
     * @param context [Context] to use to access resources
     */
    constructor(context: Context?) : super(context)

    /**
     * Perform inflation from XML. We just call our super's constructor.
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * This is called when the view is detached from a window. At this point it no longer has a
     * surface for drawing. We call our [setImageDrawable] method with `null` to clear the drawable,
     * then call our super's implementation of `onDetachedFromWindow`
     *
     * @see android.widget.ImageView.onDetachedFromWindow
     */
    override fun onDetachedFromWindow() {
        // This has been detached from Window, so clear the drawable
        setImageDrawable(null)
        super.onDetachedFromWindow()
    }

    /**
     * Sets a drawable as the content of this [ImageView]. First we save a reference to the present
     * drawable in [Drawable] variable `val previousDrawable`, then we call our super's implementation
     * of `setImageDrawable` to set our [Drawable] parameter [drawable] as the content of this
     * [ImageView]. We then call our method [notifyDrawable] with [drawable] as its `drawable`
     * argument and `true` as its `isDisplayed` argument to notify [drawable] that it is being
     * displayed, and call it again with `previousDrawable` as its `drawable` argument and `false`
     * as its `isDisplayed` argument to notify `previousDrawable` that it is no longer being
     * displayed.
     *
     * @param drawable the Drawable to set, or `null` to clear the
     * content
     */
    override fun setImageDrawable(drawable: Drawable?) {
        // Keep hold of previous Drawable
        val previousDrawable: Drawable? = getDrawable()

        // Call super to set new Drawable
        super.setImageDrawable(drawable)

        // Notify new Drawable that it is being displayed
        notifyDrawable(drawable = drawable, isDisplayed = true)

        // Notify old Drawable so it is no longer being displayed
        notifyDrawable(drawable = previousDrawable, isDisplayed = false)
    }

    companion object {
        /**
         * Notifies the drawable that it's displayed state has changed. We branch based on the class
         * of our [Drawable] parameter [drawable]:
         *
         *  * [RecyclingBitmapDrawable]: We call the [RecyclingBitmapDrawable.setIsDisplayed] method
         *  of [drawable] with our [Boolean] parameter [isDisplayed] (This is only used on devices
         *  older than HONEYCOMB)
         *
         *  * [LayerDrawable]: For each of the layers (2) in [drawable] we call this method on the
         *  [Drawable] of that layer and our parameter [isDisplayed].
         *
         * @param drawable [Drawable] to notify
         * @param isDisplayed flag indicating whether the drawable is displayed (`true`)
         * or not (`false`)
         */
        private fun notifyDrawable(drawable: Drawable?, isDisplayed: Boolean) {
            if (drawable is RecyclingBitmapDrawable) {
                // The drawable is a CountingBitmapDrawable, so notify it
                drawable.setIsDisplayed(isDisplayed = isDisplayed)
            } else if (drawable is LayerDrawable) {
                // The drawable is a LayerDrawable, so recurse on each layer
                var i = 0
                val z: Int = drawable.numberOfLayers
                while (i < z) {
                    notifyDrawable(
                        drawable = drawable.getDrawable(/* index = */ i),
                        isDisplayed = isDisplayed
                    )
                    i++
                }
            }
        }
    }
}