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
package com.example.android.insertingcells

/**
 * The data model for every cell in the ListView for this application. This model stores
 * a title, an image resource and a default cell height for every item in the ListView.
 */
class ListItemObject
/**
 * Our constructor, we call our super's constructor (first time I have seen an `Object` do
 * this), then we save our parameters in their appropriate fields.
 *
 * @param title       text we are to use for our `String mTitle` field
 * @param imgResource resource id we are to use for our `int mImgResource` field
 * @param height      height we are to use for our `int mHeight` field
 */(
    /**
     * The title displayed in our `TextView`, set by our constructor.
     */
    val title: String,
    /**
     * The resource id of the image displayed in our `ImageView`, set by our constructor.
     */
    val imgResource: Int,
    /**
     * Cell height of this object, set by our constructor.
     */
    val height: Int) {
    /**
     * Getter for our `String mTitle` field.
     *
     * @return the contents of our `String mTitle` field
     */
    /**
     * Getter for our `int mImgResource` field.
     *
     * @return the contents of our `int mImgResource` field
     */
    /**
     * Getter for our `int mHeight` field.
     *
     * @return the contents of our `int mHeight` field
     */

}