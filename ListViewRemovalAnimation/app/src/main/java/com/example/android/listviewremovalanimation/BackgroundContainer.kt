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

package com.example.android.listviewremovalanimation

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ListView
import androidx.core.graphics.withTranslation

/**
 * Draws a png [Drawable] in the area where an item in our [ListView] is partially swiped off the
 * screen (slightly shows through the item being swiped as well).
 */
class BackgroundContainer : FrameLayout {
    /**
     * Flag indicating that we are supposed to be "showing through" in the area occupied by the
     * swiped item in the [ListView], it is set by our [showBackground] method along with the top
     * Y coordinate and the height of the area we are to "show through"
     */
    var mShowing: Boolean = false

    /**
     * The [Drawable] with id `R.drawable.shadowed_background` (shadowed_background.9.png) which
     * we draw in the area occupied by the item being swiped when [mShowing] is true.
     */
    var mShadowedBackground: Drawable? = null

    /**
     * Top Y coordinate of the item [View] which is being swiped
     */
    var mOpenAreaTop: Int = 0

    /**
     * Height of the item [View] which is being swiped
     */
    var mOpenAreaHeight: Int = 0

    /**
     * Flag indicating that our [showBackground] has been called at least least once and we should
     * start setting the bounds of [Drawable] field [mShadowedBackground] to our width and the
     * height value contained in [Int] field [mOpenAreaHeight] (This flag is unnecessary in my
     * opinion due to the fact that a `true` [mShowing] guarantees that [mUpdateBounds] is also
     * `true`).
     */
    var mUpdateBounds: Boolean = false

    /**
     * Our one argument constructor. First we call our super's constructor, then we call our method
     * [initialize] to load [Drawable] field [mShadowedBackground] from our resources. UNUSED
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     */
    constructor(context: Context?) : super(context!!) {
        initialize()
    }

    /**
     * Perform inflation from XML. First we call our super's constructor, then we call our method
     * [initialize] to load [Drawable] field [mShadowedBackground] from our resources. This is the
     * one that is used
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        initialize()
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. We call our super's constructor, then call our method [initialize] to load
     * [Drawable] field [mShadowedBackground] from our resources. UNUSED
     *
     * @param context The [Context] the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a
     * reference to a style resource that supplies default values for
     * the view. Can be 0 to not look for defaults.
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    ) {
        initialize()
    }

    /**
     * Called from our constructors to initialize our [Drawable] field [mShadowedBackground] with
     * the 9 patch png in our resources with id `R.drawable.shadowed_background`. If the SDK of the
     * device we are running on is older than `LOLLIPOP` we use the 1 argument overload of the
     * [Resources.getDrawable], and for `LOLLIPOP` and newer we use the 2 argument overload which
     * adds an optional `theme` argument (we pass `null`).
     */
    private fun initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mShadowedBackground =
                context.resources.getDrawable(R.drawable.shadowed_background, null)
        } else {
            @Suppress("DEPRECATION") // Needed for SDK less than LOLLIPOP
            mShadowedBackground = context.resources.getDrawable(R.drawable.shadowed_background)
        }
    }

    /**
     * TODO: Continue here.
     * Causes our [Drawable] field [mShadowedBackground] to be drawn in the area occupied by the
     * item [View] which is being swiped. First we call the [setWillNotDraw] method with `false` so
     * that our [onDraw] override will be called. Then we save our [Int] parameter [top] in our
     * [Int] field [mOpenAreaTop], our [Int] parameter [height] in our [Int] field [mOpenAreaHeight]
     * and set our [Boolean] flag fields [mShowing] and [mUpdateBounds] both to true.
     *
     * @param top Top Y coordinate of the item [View] which is being swiped
     * @param height Height of the item [View] which is being swiped
     */
    fun showBackground(top: Int, height: Int) {
        setWillNotDraw(false)
        mOpenAreaTop = top
        mOpenAreaHeight = height
        mShowing = true
        mUpdateBounds = true
    }

    /**
     * Causes our [onDraw] override to stop drawing our shadowed background. First we call the
     * [setWillNotDraw] method with `true` to set our view's WILL_NOT_DRAW flag (allowing
     * optimizations), then we set our flag field [mShowing] to `false`.
     */
    fun hideBackground() {
        setWillNotDraw(true)
        mShowing = false
    }

    /**
     * We implement this to do our drawing. If our [Boolean] flag field [mShowing] is `false` we do
     * nothing. Otherwise we set the bounds of our [Drawable] field [mShadowedBackground] to have
     * the width of our [View] and the height of [Int] field [mOpenAreaHeight]. We then save the
     * current matrix and clip of our [Canvas] parameter [canvas] onto a private stack, translate it
     * in the Y dimension to [Int] field [mOpenAreaTop] and instruct [Drawable] field
     * [mShadowedBackground] to draw itself on [canvas]. Finally we restore [canvas] to the state it
     * had when we were called.
     *
     * @param canvas the [Canvas] on which the background will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        if (mShowing) {
            if (mUpdateBounds) {
                mShadowedBackground!!.setBounds(0, 0, width, mOpenAreaHeight)
            } else {
                Log.i("BackgroundContainer", "onDraw called with Drawable mUpdateBounds false")
            }
            canvas.withTranslation(x = 0f, y = mOpenAreaTop.toFloat()) {
                mShadowedBackground!!.draw(this)
            }
        }
    }
}
