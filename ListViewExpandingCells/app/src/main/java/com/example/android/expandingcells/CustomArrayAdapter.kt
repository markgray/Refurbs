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
@file:Suppress("ReplaceJavaStaticMethodWithKotlinAnalog", "MemberVisibilityCanBePrivate")

package com.example.android.expandingcells

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.graphics.createBitmap

/**
 * This is a custom array adapter used to populate the [ListView] whose items will expand to display
 * extra content in addition to the default display.
 *
 * Constructor. We just we call our super's constructor, our parameters are also our fields.
 *
 * @param context              The current [Context].
 * @param mLayoutViewResourceId The resource ID for a layout file containing a [TextView] to use
 * when instantiating views.
 * @param mData                 The objects to represent in the [ListView].
 */
class CustomArrayAdapter(
    /**
     * The current [Context].
     */
    context: Context?,
    /**
     * The resource id we use as the layout to display the [ExpandableListItem] objects in our dataset.
     */
    private val mLayoutViewResourceId: Int,
    /**
     * Our dataset, it is set by our constructor.
     */
    private val mData: List<ExpandableListItem>
) : ArrayAdapter<ExpandableListItem?>(context!!, mLayoutViewResourceId, mData) {
    /**
     * Get a [View] that displays the data at the specified position in the data set. Populates the
     * item in the [ListView] cell with the appropriate data. This method sets the thumbnail image,
     * the title and the extra text. This method also updates the layout parameters of the item's
     * view so that the image and title are centered in the bounds of the collapsed view, and such
     * that the extra text is not displayed in the collapsed state of the cell.
     *
     * First we initialize [View] variable `var convertViewLocal` to our [View] parameter [convertView],
     * and our [ExpandableListItem] variable `val listItem` with the item whose index is given by [Int]
     * parameter [position] in our [List] of [ExpandableListItem] dataset field [mData]. If our [View]
     * variable `convertViewLocal` is `null` we initialize [LayoutInflater] variable `val inflater`
     * with the instance for our context and use it to inflate the layout file with the resource id
     * in [Int] field [mLayoutViewResourceId] into `convertViewLocal` using our [ViewGroup] parameter
     * [parent] for the layout params without attaching to it. Having ensured that `convertViewLocal`
     * holds a non-`null` [View] we initialize [LinearLayout] variable `val linearLayout` by finding
     * the view in `convertViewLocal` with id `R.id.item_linear_layout`, then we initialize
     * [LinearLayout.LayoutParams] variable `val linearLayoutParams` with an instance whose X size
     * is `MATCH_PARENT` and whose Y size is the collapsed height of `listItem`, and we then set the
     * layout params of `linearLayout` to `linearLayoutParams`.
     *
     * We initialize [ImageView] variable `val imgView` by finding the view in `convertViewLocal`
     * with id `R.id.image_view`, [TextView] variable `val titleView` by finding the view with id
     * `R.id.title_view`, and [TextView] variable `val textView` by finding the view with id
     * `R.id.text_view`. We set the text of `titleView` to the [ExpandableListItem.title] field of
     * `listItem`, the content of `imgView` to the bitmap returned by our method [getCroppedBitmap]
     * when passed the bitmap decoded from the resource id in the [ExpandableListItem.imgResource]
     * field of `listItem` ([getCroppedBitmap] returns a circle cut out of the bitmap), and the text
     * of `textView` to the text string in the [ExpandableListItem.text] field of `listItem`. We then
     * set the layout params of `convertViewLocal` to an instance whose X size is MATCH_PARENT and
     * whose Y size is WRAP_CONTENT.
     *
     * We initialize [ExpandingLayout] variable `val expandingLayout` by finding the view in
     * `convertViewLocal` with id `R.id.expanding_layout`, set its expanded height property to the
     * expanded height in the [ExpandableListItem.expandedHeight] field of `listItem`, and set its
     * [OnSizeChangedListener] to `listItem`. If `listItem` is not expanded we set its visibility to
     * GONE, and if it is expanded we set it to VISIBLE. Finally we return `convertViewLocal` to the
     * caller.
     *
     * @param position The position of the item within the adapter's data set whose [View] we want.
     * @param convertView The old [View] to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to
     * @return A [View] corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertViewLocal: View? = convertView
        val listItem: ExpandableListItem = mData[position]
        if (convertViewLocal == null) {
            val inflater: LayoutInflater = (context as Activity).layoutInflater
            convertViewLocal = inflater.inflate(
                /* resource = */ mLayoutViewResourceId,
                /* root = */ parent,
                /* attachToRoot = */ false
            )
        }
        val linearLayout = convertViewLocal!!.findViewById<LinearLayout>(R.id.item_linear_layout)
        val linearLayoutParams = LinearLayout.LayoutParams(
            /* width = */ AbsListView.LayoutParams.MATCH_PARENT,
            /* height = */ listItem.collapsedHeight
        )
        linearLayout.layoutParams = linearLayoutParams
        val imgView = convertViewLocal.findViewById<ImageView>(R.id.image_view)
        val titleView = convertViewLocal.findViewById<TextView>(R.id.title_view)
        val textView = convertViewLocal.findViewById<TextView>(R.id.text_view)
        titleView.text = listItem.title
        imgView.setImageBitmap(
            getCroppedBitmap(
                BitmapFactory.decodeResource(
                    /* res = */ context.resources,
                    /* id = */ listItem.imgResource,
                    /* opts = */ null
                )
            )
        )
        textView.text = listItem.text
        convertViewLocal.layoutParams = AbsListView.LayoutParams(
            /* w = */ AbsListView.LayoutParams.MATCH_PARENT,
            /* h = */ AbsListView.LayoutParams.WRAP_CONTENT
        )
        val expandingLayout = convertViewLocal.findViewById<ExpandingLayout>(R.id.expanding_layout)
        expandingLayout.expandedHeight = listItem.expandedHeight
        expandingLayout.setSizeChangedListener(listItem)
        if (!listItem.isExpanded) {
            expandingLayout.visibility = View.GONE
        } else {
            expandingLayout.visibility = View.VISIBLE
        }
        return convertViewLocal
    }

    /**
     * TODO: Continue here.
     * Crops a circle out of the thumbnail photo passed it and returns the circle to the caller.
     * First we initialize [Bitmap] variable `val output` with a [Bitmap] that is the same width
     * and height as our [Bitmap] parameter [bitmap]. We initialize [Rect] variable `val rect` to
     * an instance whose dimensions are the width of [bitmap] by its height. We initialize [Canvas]
     * variable `val canvas` with an instance which will draw into `output`. We initialize [Paint]
     * variable `val paint` with a new instance and set its antialias flag to `true`, initialize
     * [Int] variable `val halfWidth` to be half the width of [bitmap] and [Int] variable
     * `val halfHeight` to be half its height. We then draw a circle on `canvas` centered at
     * (`halfWidth`, `halfHeight`) whose radius is the larger of `halfWidth` and `halfHeight`
     * using `paint` as the [Paint]. New we set the transfer mode of `paint` to SRC_IN (keeps the
     * source pixels that cover the destination pixels, discards the remaining source and destination
     * pixels), and use it to draw [bitmap] on `canvas` using `rect` as both the source and
     * destination rectangle (utilizes the entire bitmaps without scaling or translation). Finally
     * we return `output` to the caller.
     *
     * @param bitmap the [Bitmap] to be cropped.
     * @return circle [Bitmap] cropped out of our [Bitmap] parameter [bitmap]
     */
    fun getCroppedBitmap(bitmap: Bitmap): Bitmap {
        val output = createBitmap(width = bitmap.width, height = bitmap.height)
        val rect = Rect(
            /* left = */ 0,
            /* top = */ 0,
            /* right = */ bitmap.width,
            /* bottom = */ bitmap.height
        )
        val canvas = Canvas(/* bitmap = */ output)
        val paint = Paint()
        paint.isAntiAlias = true
        val halfWidth = bitmap.width / 2
        val halfHeight = bitmap.height / 2
        canvas.drawCircle(
            /* cx = */ halfWidth.toFloat(),
            /* cy = */ halfHeight.toFloat(),
            /* radius = */ Math.max(halfWidth, halfHeight).toFloat(),
            /* paint = */ paint
        )
        paint.xfermode = PorterDuffXfermode(/* mode = */ PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(
            /* bitmap = */ bitmap,
            /* src = */ rect,
            /* dst = */ rect,
            /* paint = */ paint
        )
        return output
    }
}
