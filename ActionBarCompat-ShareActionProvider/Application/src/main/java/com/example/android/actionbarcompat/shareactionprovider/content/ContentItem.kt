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
@file:Suppress("MemberVisibilityCanBePrivate") // I like to use kdoc [] references

package com.example.android.actionbarcompat.shareactionprovider.content

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.core.net.toUri

/**
 * This class encapsulates a content item. Referencing the content's type, and the differing way
 * to reference the content (asset [Uri] or resource id).
 */
class ContentItem {
    /**
     * Content type of this instance, either CONTENT_TYPE_IMAGE or CONTENT_TYPE_TEXT
     */
    val contentType: Int

    /**
     * Resource ID associated with this `Content` instance
     */
    val contentResourceId: Int

    /**
     * Asset file path associated with this `Content` instance
     */
    val contentAssetFilePath: String?

    /**
     * Creates a [ContentItem] with the specified type, referencing a resource id. We save our
     * parameters in our fields [contentType] and [contentResourceId] respectively, and set our
     * field [contentAssetFilePath] to null.
     *
     * @param type       - One of [CONTENT_TYPE_IMAGE] or [CONTENT_TYPE_TEXT]
     * @param resourceId - Resource ID to use for this item's content
     */
    constructor(type: Int, resourceId: Int) {
        contentType = type
        contentResourceId = resourceId
        contentAssetFilePath = null
    }

    /**
     * Creates a [ContentItem] with the specified type, referencing an asset file path. We save our
     * parameters in our fields [contentType] and [contentAssetFilePath] respectively and set our
     * field [contentResourceId] to 0.
     *
     * @param type          - One of [CONTENT_TYPE_IMAGE] or [CONTENT_TYPE_TEXT]
     * @param assetFilePath - File path from the application's asset for this item's content
     */
    constructor(type: Int, assetFilePath: String?) {
        contentType = type
        contentAssetFilePath = assetFilePath
        contentResourceId = 0
    }

    /**
     * Creates a content [Uri] from our field [contentAssetFilePath] if it is not the empty string
     * which we return to our caller, otherwise we return null.
     *
     * @return [Uri] to the content
     */
    val contentUri: Uri?
        get() = if (!TextUtils.isEmpty(contentAssetFilePath)) {
            // If this content has an asset, then return a AssetProvider Uri
            ("content://" + AssetProvider.CONTENT_URI + "/" + contentAssetFilePath).toUri()
        } else {
            null
        }

    /**
     * Returns an [Intent] which can be used to share this item's content with other applications.
     * First we initialize our [Intent] variable `val intent` with a new instance with action
     * ACTION_SEND. Then we switch on our field [contentType]:
     *  * [CONTENT_TYPE_IMAGE] - we set the type of `intent` to "image/jpg", and add the content
     *  [Uri] returned from our property [contentUri] as an extra under the key EXTRA_STREAM.
     *
     *  * [CONTENT_TYPE_TEXT] - we set the type of `intent` to "text/plain", and add the string
     *  returned from the method `getString` for the resource ID [contentResourceId]
     *  as an extra under the key EXTRA_TEXT.
     *
     * Finally we return `intent` to the caller.
     *
     * @param context the [Context] to be used for fetching resources if needed
     * @return [Intent] to be given to a `ShareActionProvider`.
     */
    fun getShareIntent(context: Context): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        when (contentType) {
            CONTENT_TYPE_IMAGE -> {
                intent.type = "image/jpg"
                // Bundle the asset content uri as the EXTRA_STREAM uri
                intent.putExtra(Intent.EXTRA_STREAM, contentUri)
            }

            CONTENT_TYPE_TEXT -> {
                intent.type = "text/plain"
                // Get the string resource and bundle it as an intent extra
                intent.putExtra(Intent.EXTRA_TEXT, context.getString(contentResourceId))
            }
        }
        return intent
    }

    companion object {
        /**
         * Used to signify an image content type
         */
        const val CONTENT_TYPE_IMAGE: Int = 0

        /**
         * Used to signify a text/string content type
         */
        const val CONTENT_TYPE_TEXT: Int = 1
    }
}