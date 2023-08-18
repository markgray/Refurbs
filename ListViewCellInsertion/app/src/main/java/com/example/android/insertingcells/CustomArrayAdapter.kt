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

package com.example.android.insertingcells

import android.app.Activity
import android.content.Context
import android.content.res.Resources
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
import android.widget.AbsListView.LayoutParams.MATCH_PARENT
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView

/**
 * This custom array adapter is used to populate the [ListView] in this application. This adapter
 * also maintains a map of unique stable ids for each object in the data set. Since this adapter
 * has to support the addition of a new cell to the 1ist index, it also provides a mechanism to add
 * a stable ID for new data that was recently inserted.
 *
 * Our constructor. First we call our super's constructor. Then in our `init` block we call our
 * method [updateStableIds] to build our [HashMap] of [ListItemObject] to [Int] field [mIdMap],
 * which we use to access stable ids for each of the [ListItemObject] items in our dataset.
 *
 * @param mContext The current [Context]
 * @param mLayoutViewResourceId Resource id for the item layout field we should use
 * @param mData our dataset.
 */
class CustomArrayAdapter(
    /**
     * The current context as passed to our constructor, used for quick access to the [LayoutInflater]
     * instance that our activity retrieved from its Context, and to retrieve a [Resources] instance
     * for the application's package.
     */
    var mContext: Context,
    /**
     * Resource id for the item layout field we should use as passed to our constructor.
     */
    var mLayoutViewResourceId: Int,
    /**
     * Our dataset, set by our constructor, shared with the [InsertionListView] we are the adapter
     * for and added to by it behind our back.
     */
    var mData: List<ListItemObject>
) : ArrayAdapter<ListItemObject?>(mContext, mLayoutViewResourceId, mData) {
    /**
     * Stable ids for each of the [ListItemObject] items in our dataset.
     */
    var mIdMap: HashMap<ListItemObject?, Int> = HashMap()

    /**
     * Counter we use to assign a stable id to each of the [ListItemObject] items in our dataset.
     */
    var mCounter: Int = 0

    init {
        updateStableIds()
    }

    /**
     * Get the row id associated with the specified position in the list. We initialize our variable
     * `ListItemObject item` with the `ListItemObject` in position `position` in our
     * dataset. If our field `HashMap<ListItemObject, Integer> mIdMap` contains a value for
     * `item` we return that value to the caller, otherwise we return -1.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return if (mIdMap.containsKey(item)) {
            mIdMap[item]!!.toLong()
        } else -1L
    }

    /**
     * Called to create the mapping of `ListItemObject` items to item id held in our field
     * `HashMap<ListItemObject, Integer> mIdMap`. First we remove all of the mappings from
     * `mIdMap`, and set our field `int mCounter` to 0. Then we loop over `int i`
     * for all of the entries in our dataset `List<ListItemObject> mData` storing the value
     * of `mCounter` under the `ListItemObject` key we get from the `i`'th entry
     * in `mData` (`i` is post auto-incremented to be ready for the next item id).
     */
    fun updateStableIds() {
        mIdMap.clear()
        mCounter = 0
        for (i in mData.indices) {
            mIdMap[mData[i]] = mCounter++
        }
    }

    /**
     * Generates and adds a new stable id to our `HashMap<ListItemObject, Integer> mIdMap` for
     * the `ListItemObject` at position `position` in our `List<ListItemObject> mData`
     * dataset. We fetch the `ListItemObject` at position `position` in `mData`,
     * then pre auto-incrementing `mCounter` first store `mCounter` in `mIdMap`
     * using that item as the key.
     *
     * @param position position in our dataset whose `ListItemObject` needs a new item id
     */
    fun addStableIdForDataAtPosition(position: Int) {
        mIdMap[mData[position]] = ++mCounter
    }

    /**
     * Indicates whether the item ids are stable across changes to the underlying data.
     * We return true.
     *
     * @return True if the same id always refers to the same object.
     */
    override fun hasStableIds(): Boolean {
        return true
    }

    /**
     * Get a View that displays the data at the specified position in the data set. First we initialize
     * `ListItemObject obj` with the `ListItemObject` at position `position` in our
     * dataset `List<ListItemObject> mData`. If our parameter `View convertView` is null
     * we initialize `LayoutInflater inflater` with the [LayoutInflater] instance that
     * the `Activity` whose context was passed to our constructor used. Then we use it to inflate
     * the item layout file passed to our constructor (and we saved in `mLayoutViewResourceId`)
     * to set `convertView` using our parameter `ViewGroup parent` for the layout params
     * without attaching to it.
     *
     *
     * Whether recycling or using a new `View convertView` we set its layout params to MATCH_PARENT
     * for the X size, and the height of `obj` for the Y size. We initialize `ImageView imgView`
     * by finding the view in `convertView` with id R.id.image_view, and `TextView textView`
     * by finding the view with id R.id.text_view. We initialize `Bitmap bitmap` by decoding the
     * image whose resource id is that returned by the `getImgResource` method of `obj`,
     * set the text of `textView` to the string returned by the `getTitle` method of
     * `obj` and set the content of `imgView` to the circular cropped `Bitmap` created
     * by our method `getCroppedBitmap` from `bitmap`. Finally we return `convertView`
     * to the caller.
     *
     * @param position The position of the item within the adapter's dataset whose view we want.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertViewLocal = convertView
        val obj = mData[position]
        if (convertViewLocal == null) {
            val inflater = (mContext as Activity).layoutInflater
            convertViewLocal = inflater.inflate(mLayoutViewResourceId, parent, false)
        }
        convertViewLocal!!.layoutParams = AbsListView.LayoutParams(MATCH_PARENT, obj.height)
        val imgView = convertViewLocal.findViewById<ImageView>(R.id.image_view)
        val textView = convertViewLocal.findViewById<TextView>(R.id.text_view)
        val bitmap = BitmapFactory.decodeResource(mContext.resources, obj.imgResource, null)
        textView.text = obj.title
        imgView.setImageBitmap(getCroppedBitmap(bitmap))
        return convertViewLocal
    }

    companion object {
        /**
         * Returns a circular cropped version of the bitmap passed in. We create `Bitmap output` to
         * be the same width and height as our parameter `Bitmap bitmap` with a ARGB_8888 config,
         * and we initialize `Rect rect` to be the width and height of `bitmap` as well. We
         * initialize `Canvas canvas` to an instance that will draw into `output`. We initialize
         * `Paint paint` with a new instance and enable its anti alias flag. We initialize our variable
         * `int halfWidth` to half the width of `bitmap` and `int halfHeight` to half of
         * its height. We then draw a circle on `canvas` with its center at (`halfWidth`, `halfHeight`)
         * whose radius is the maximum of `halfWidth` and `halfHeight` using `paint` as
         * the `Paint`. We then set the transfer mode of `halfHeight` to a new instance of
         * `PorterDuffXfermode` which will use SRC_IN mode (keeps the source pixels that cover the
         * destination pixels, discards the remaining source and destination pixels -- only pixels that
         * fall with the circle we just drew will be drawn). We then draw `bitmap` on `canvas`
         * using `paint` as the `Paint`, and `rect` as the bounds for the bits that will
         * be sourced from `bitmap` (the entire bitmap as you recall) as well as `rect` as the
         * destination area that will be drawn into. Finally we return `output` to the caller.
         *
         * @param bitmap `Bitmap` to use for the circular cropped version we create
         * @return a circular cropped version of the bitmap passed in.
         */
        @JvmStatic
        fun getCroppedBitmap(bitmap: Bitmap): Bitmap {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
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
}