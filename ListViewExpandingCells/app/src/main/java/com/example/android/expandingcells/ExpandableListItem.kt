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
package com.example.android.expandingcells

/**
 * This custom object is used to populate the list adapter. It contains a reference
 * to an image, title, and the extra text to be displayed. Furthermore, it keeps track
 * of the current state (collapsed/expanded) of the corresponding item in the list,
 * as well as storing the height of the cell in its collapsed state.
 *
 * Our constructor, we just initialize our fields appropriately.
 *
 * @param title text to be displayed as the title of our item
 * @param imgResource resource id of the jpg to be used as the icon of our item
 * @param collapsedHeight height when in the collapsed state, always CELL_DEFAULT_HEIGHT (200 pixels)
 * @param text text to be displayed when we are in our expanded state
 */
class ExpandableListItem(
    /**
     * Title of the item, it is displayed to the right of the image whether the `ExpandableListItem`
     * is "expanded" or not. The `getView` override of `CustomArrayAdapter` uses it as the
     * text of the `TextView` with id R.id.title_view.
     */
    val title: String,
    /**
     * Resource id of the jpg used for the `ImageView` with id R.id.image_view in the item
     * layout, it is displayed to the left of the title of the item.
     */
    val imgResource: Int,
    /**
     * Height of the item in the collapsed state, it is set to CELL_DEFAULT_HEIGHT (200 pixels) in
     * calls to our constructor.
     */
    var collapsedHeight: Int,
    /**
     * Text used by the `TextView` inside the `ExpandingLayout` with id R.id.text_view
     */
    var text: String) : OnSizeChangedListener {

    /**
     * Flag used to indicate whether the item is in the expanded (true) or default state (false)
     */
    var isExpanded: Boolean = false

    /**
     * This is the height of the item when it is in the expanded state, it starts at -1 when we are
     * constructed, and is set to the height reported to our `onSizeChanged` override (we
     * are set as the `OnSizeChangedListener` of the `ExpandingLayout` displaying us)
     */
    var expandedHeight: Int

    init {
        expandedHeight = -1
    }

    /**
     * Called from the `onSizeChanged` override of the `ExpandingLayout` displaying our
     * item's expanded text. We just call our `setExpandedHeight` override with our parameter
     * `newHeight` as the argument.
     *
     * @param newHeight new height of the `View`
     */
    override fun onSizeChanged(newHeight: Int) {
        expandedHeight = newHeight
    }
}