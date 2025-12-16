/*
* Copyright (C) 2014 The Android Open Source Project
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
package com.example.android.directoryselection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Provides views to RecyclerView with the directory entries.
 *
 * @param mDirectoryEntries [List] of [DirectoryEntry] directory entries of the Adapter.
 */
class DirectoryEntryAdapter(
    private var mDirectoryEntries: List<DirectoryEntry>
) : RecyclerView.Adapter<DirectoryEntryAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        /**
         * Cached link to the [TextView] we display the file name in: `R.id.textview_filename`
         */
        @Suppress("JoinDeclarationAndAssignment") // Easier to debug this way
        val fileName: TextView

        /**
         * Cached link to the [TextView] we display the mime type in: `R.id.textview_mimetype`
         */
        val mimeType: TextView

        /**
         * Cached link to the [ImageView] we display the file icon in: `R.id.entry_image`
         */
        val imageView: ImageView

        /**
         * Our init block. First we call our super's constructor, then we initialize our `TextView`
         * field `mFileName` by finding the view with id R.id.textview_filename, our `TextView` field
         * `mMimeType` by finding the view with id R.id.textview_mimetype, and our `ImageView` field
         * `mImageView` by finding the view with id R.id.entry_image.
         *
         * param v View inflated from the layout file of `R.layout.directory_item` that we are
         * to use to display our directory entry.
         */
        init {
            fileName = v.findViewById(R.id.textview_filename)
            mimeType = v.findViewById(R.id.textview_mimetype)
            imageView = v.findViewById(R.id.entry_image)
        }
    }

    /**
     * Called when RecyclerView needs a new [ViewHolder] of the given type to represent an item.
     * We initialize [View] variable `val v` by using the [LayoutInflater] from the context of our
     * [ViewGroup] parameter [viewGroup] to inflate our layout file `R.layout.directory_item`, using
     * [viewGroup] for the layout parameters but not attaching the view to it. We then return a
     * [ViewHolder] constructed from `v` to the caller.
     *
     * @param viewGroup The [ViewGroup] into which the new [View] will be added after it is bound to
     * an adapter position.
     * @param viewType  The view type of the new [View].
     * @return A new [ViewHolder] that holds a [View] of the given view type.
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(viewGroup.context)
            .inflate(
                /* resource = */ R.layout.directory_item,
                /* root = */ viewGroup,
                /* attachToRoot = */ false
            )
        return ViewHolder(v)
    }

    /**
     * TODO: Continue here.
     * Called by RecyclerView to display the data at the specified position. We set the `text`
     * of the [ViewHolder.fileName] cached [TextView] of our [ViewHolder] parameter [viewHolder]
     * to the [DirectoryEntry.fileName] field of the [DirectoryEntry] object in position
     * [position] of our data set field [mDirectoryEntries], and set the `text` of the
     * [ViewHolder.mimeType] cached [TextView] of our [ViewHolder] parameter [viewHolder] to the
     * [DirectoryEntry.mimeType] field of the [DirectoryEntry] object in position [position] of our
     * data set field [mDirectoryEntries]. If the [DirectoryEntry.mimeType] field of the
     * [DirectoryEntry] object in position [position] of our data set [mDirectoryEntries] is
     * [DIRECTORY_MIME_TYPE], we set the image of the [ViewHolder.imageView] cached [ImageView] of
     * our [ViewHolder] parameter [viewHolder] to the png with the resource code
     * `R.drawable.ic_folder_grey600_36dp` (a folder icon), otherwise we do the same only using the
     * png with resource id `R.drawable.ic_description_grey600_36dp` (a file icon).
     *
     * @param viewHolder The [ViewHolder] which should be updated to represent the contents of the
     * item at the given [position] in the data set.
     * @param position   The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.fileName.text = mDirectoryEntries[position].fileName
        viewHolder.mimeType.text = mDirectoryEntries[position].mimeType
        if (DIRECTORY_MIME_TYPE == mDirectoryEntries[position].mimeType) {
            viewHolder.imageView.setImageResource(R.drawable.ic_folder_grey600_36dp)
        } else {
            viewHolder.imageView.setImageResource(R.drawable.ic_description_grey600_36dp)
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter. This is just the size
     * of our [List] of [DirectoryEntry] field [mDirectoryEntries].
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return mDirectoryEntries.size
    }

    /**
     * Setter for our [List] of [DirectoryEntry] field [mDirectoryEntries].
     *
     * @param directoryEntries a [List] of [DirectoryEntry] to use to set our field
     * [mDirectoryEntries].
     */
    fun setDirectoryEntries(directoryEntries: List<DirectoryEntry>) {
        mDirectoryEntries = directoryEntries
    }

    companion object {
        /**
         * MIME type of a document which is a directory that may contain additional documents.
         */
        const val DIRECTORY_MIME_TYPE: String = "vnd.android.document/directory"
    }
}
