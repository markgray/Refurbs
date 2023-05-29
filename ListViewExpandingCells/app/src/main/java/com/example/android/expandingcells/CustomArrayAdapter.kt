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
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * This is a custom array adapter used to populate the ListView whose items will
 * expand to display extra content in addition to the default display.
 *
 * Constructor. First we call our super's constructor, then we save our parameter `data` in
 * our field `List<ExpandableListItem> mData` and our parameter `layoutViewResourceId`
 * in our field `int mLayoutViewResourceId`.
 *
 * @param context              The current context.
 * @param mLayoutViewResourceId The resource ID for a layout file containing a TextView to use when
 * instantiating views.
 * @param mData                 The objects to represent in the ListView.
 */
class CustomArrayAdapter(
    /**
     * The current context.
     */
    context: Context?,
    /**
     * The resource id we use as the layout for the `ExpandableListItem` objects in our dataset.
     */
    private val mLayoutViewResourceId: Int,
    /**
     * Our dataset, it is set by our constructor.
     */
    private val mData: List<ExpandableListItem>) : ArrayAdapter<ExpandableListItem?>(context!!, mLayoutViewResourceId, mData) {
    /**
     * Get a View that displays the data at the specified position in the data set. Populates the
     * item in the ListView cell with the appropriate data. This method sets the thumbnail image,
     * the title and the extra text. This method also updates the layout parameters of the item's
     * view so that the image and title are centered in the bounds of the collapsed view, and such
     * that the extra text is not displayed in the collapsed state of the cell.
     *
     *
     * First we initialize `ExpandableListItem object` with the item at position `position`
     * in our dataset `List<ExpandableListItem> mData`. If our parameter `View convertView`
     * is null we initialize `LayoutInflater inflater` with the instance for our context and use
     * it to inflate the layout file with id `mLayoutViewResourceId` into `convertView`
     * using our parameter `ViewGroup parent` for the layout params without attaching to it.
     * Null or not we initialize `LinearLayout linearLayout` by finding the view in `convertView`
     * with id R.id.item_linear_layout, initialize `LayoutParams linearLayoutParams` with an instance
     * whose X size is MATCH_PARENT and whose Y size is the collapsed height of `object`, then
     * set the layout params of `linearLayout` to `linearLayoutParams`.
     *
     *
     * We initialize `ImageView imgView` by finding the view in `convertView` with id
     * R.id.image_view, `TextView titleView` by finding the view with id R.id.title_view, and
     * `TextView textView` by finding the view with id R.id.text_view. We set the text of
     * `titleView` to the title of `object`, the content of `imgView` to the bitmap
     * returned by our method `getCroppedBitmap` when passed the bitmap decoded from the image
     * resource id of `object` (`getCroppedBitmap` returns a circle cut out of the bitmap),
     * and the text of `textView` to the text string of `object`. We then set the layout
     * params of `convertView` to an instance whose X size is MATCH_PARENT and whose Y size is
     * WRAP_CONTENT.
     *
     *
     * We initialize `ExpandingLayout expandingLayout` by finding the view in `convertView`
     * with id R.id.expanding_layout, set its expanded height property to the expanded height of
     * `object`, and set its `OnSizeChangedListener` to `object`. If `object`
     * is not expanded we set its visibility to GONE, and if it is expanded we set it to VISIBLE.
     * Finally we return `convertView` to the caller.
     *
     * @param position The position of the item within the adapter's data set whose view we want.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertViewLocal = convertView
        val `object` = mData[position]
        if (convertViewLocal == null) {
            val inflater = (context as Activity).layoutInflater
            convertViewLocal = inflater.inflate(mLayoutViewResourceId, parent, false)
        }
        val linearLayout = convertViewLocal!!.findViewById<LinearLayout>(R.id.item_linear_layout)
        val linearLayoutParams = LinearLayout.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
            `object`.collapsedHeight)
        linearLayout.layoutParams = linearLayoutParams
        val imgView = convertViewLocal.findViewById<ImageView>(R.id.image_view)
        val titleView = convertViewLocal.findViewById<TextView>(R.id.title_view)
        val textView = convertViewLocal.findViewById<TextView>(R.id.text_view)
        titleView.text = `object`.title
        imgView.setImageBitmap(getCroppedBitmap(BitmapFactory.decodeResource(context
            .resources, `object`.imgResource, null)))
        textView.text = `object`.text
        convertViewLocal.layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
            AbsListView.LayoutParams.WRAP_CONTENT)
        val expandingLayout = convertViewLocal.findViewById<ExpandingLayout>(R.id.expanding_layout)
        expandingLayout.expandedHeight = `object`.expandedHeight
        expandingLayout.setSizeChangedListener(`object`)
        if (!`object`.isExpanded) {
            expandingLayout.visibility = View.GONE
        } else {
            expandingLayout.visibility = View.VISIBLE
        }
        return convertViewLocal
    }

    /**
     * Crops a circle out of the thumbnail photo passed it and returns the circle to the caller. First
     * we initialize `Bitmap output` with a bitmap that is the same width and height as our
     * parameter `Bitmap bitmap`. We initialize `Rect rect` whose dimensions are the width
     * of `bitmap` by its height. We initialize `Canvas canvas` with an instance which will
     * draw into `output`. We initialize `Paint paint` with a new instance and set its
     * antialias flag, initialize `int halfWidth` to be half the width of `bitmap` and
     * `int halfHeight` to be half its height. We then draw a circle on `canvas` centered
     * at (halfWidth, halfHeight) whose radius is the larger of `halfWidth` and `halfHeight`
     * using `paint` as the `Paint`. New we set the transfer mode of `paint` to
     * SRC_IN (keeps the source pixels that cover the destination pixels, discards the remaining source
     * and destination pixels), and use it to draw `bitmap` on `canvas` using `rect`
     * as both the source and destination rectangle (utilizes the entire bitmaps without scaling or
     * translation). Finally we return `output` to the caller.
     *
     * @param bitmap `Bitmap` to be cropped.
     * @return circle `Bitmap` cropped out of our parameter `Bitmap bitmap`
     */
    fun getCroppedBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height,
            Bitmap.Config.ARGB_8888)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val canvas = Canvas(output)
        val paint = Paint()
        paint.isAntiAlias = true
        val halfWidth = bitmap.width / 2
        val halfHeight = bitmap.height / 2
        canvas.drawCircle(halfWidth.toFloat(), halfHeight.toFloat(), Math.max(halfWidth, halfHeight).toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
}