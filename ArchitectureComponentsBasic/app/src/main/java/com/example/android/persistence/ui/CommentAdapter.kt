/*
 * Copyright 2017, The Android Open Source Project
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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.persistence.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.android.persistence.R
import com.example.android.persistence.databinding.CommentItemBinding
import com.example.android.persistence.model.Comment
import com.example.android.persistence.ui.CommentAdapter.CommentViewHolder

/**
 * `RecyclerView.Adapter` for `Comment` objects (that interface is implemented by our
 * `CommentEntity` class in package com.example.android.persistence.db.entity).
 */
class CommentAdapter
/**
 * Our constructor, called only by `ProductFragment`. We just save our parameter in our
 * field `CommentClickCallback mCommentClickCallback`.
 *
 * @param mCommentClickCallback `CommentClickCallback` whose `onClick` override should
 * be called when a `Comment` is clicked.
 */
(
    /**
     * The `CommentClickCallback` instance we should use when a `Comment` in our list is
     * clicked, set by our constructor (an anonymous class which does nothing is used when we are
     * constructed by `ProductFragment`)
     */
    private val mCommentClickCallback: CommentClickCallback?) : RecyclerView.Adapter<CommentViewHolder>() {
    /**
     * Our list of `Comment` (`CommentEntity`) objects, set by our method `setCommentList`
     */
    private var mCommentList: List<Comment>? = null

    /**
     * If the current list of `Comment` (`CommentEntity`) objects in our field
     * `List<`? `extends Comment> mCommentList` is null we save our parameter `comments`
     * in it and call the method `notifyItemRangeInserted` to notify any registered observers
     * that the items have been newly inserted into the previously empty list, otherwise we create
     * `DiffUtil.DiffResult diffResult` to calculate the list of update operations that can
     * convert `mCommentList` into `comments`. `diffResult` takes as its argument
     * an anonymous `DiffUtil.Callback` class (A Callback class used by DiffUtil while
     * calculating the diff between two lists) whose `getOldListSize` override returns
     * the size of our field `mCommentList`, whose `getNewListSize` returns the size of
     * our parameter `comments`, whose `areItemsTheSame` override returns true if the
     * item id of the `Comment` at its parameter `oldItemPosition` in `mCommentList`
     * is the same as the id of the `Comment` at its parameter `newItemPosition` in
     * `comments`, and whose override of `areContentsTheSame` returns true if the
     * `Comment` at its parameter `oldItemPosition` in `mCommentList` has the same
     * id, date posted stamp, product id, and the same text as the `Comment` as its parameter
     * `newItemPosition` in `comments`. Finally we set our field `mCommentList` to
     * our parameter `comments`, and call the `dispatchUpdatesTo` method of `diffResult`
     * to dispatch the update events to "this" adapter.
     *
     * @param comments list of `Comment` objects we should display.
     */
    fun setCommentList(comments: List<Comment>) {
        if (mCommentList == null) {
            mCommentList = comments
            notifyItemRangeInserted(0, comments.size)
        } else {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                /**
                 * Returns the size of the old list (`mCommentList`).
                 *
                 * @return size of the old list
                 */
                override fun getOldListSize(): Int {
                    return mCommentList!!.size
                }

                /**
                 * Returns the size of the new list (`comments`).
                 *
                 * @return size of the new list
                 */
                override fun getNewListSize(): Int {
                    return comments.size
                }

                /**
                 * Called by the DiffUtil to decide whether two object represent the same Item. We
                 * retrieve `Comment old` from position `oldItemPosition` in our list
                 * `mCommentList`, and `Comment comment` from position `newItemPosition`
                 * in our parameter `comments`, then we return the result of comparing the id
                 * of the two `Comment` objects for equality.
                 *
                 * @param oldItemPosition The position of the item in the old list
                 * @param newItemPosition The position of the item in the new list
                 * @return True if the two items represent the same object or false if they are different.
                 */
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = mCommentList!![oldItemPosition]
                    val comment = comments[newItemPosition]
                    return old.getId() == comment.getId()
                }

                /**
                 * Called by the DiffUtil when it wants to check whether two items have the same data.
                 * DiffUtil uses this information to detect if the contents of an item has changed.
                 * We retrieve `Comment old` from position `oldItemPosition` in our list
                 * `mCommentList`, and `Comment comment` from position `newItemPosition`
                 * in our parameter `comments`, then we return true iff the id's of the two
                 * `Comment` objects are the same, the time stamp posted are the same, the product
                 * id are the same, and the text of both are the same.
                 *
                 * @param oldItemPosition The position of the item in the old list
                 * @param newItemPosition The position of the item in the new list which replaces the
                 * oldItem
                 * @return True if the contents of the items are the same or false if they are different.
                 */
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = mCommentList!![oldItemPosition]
                    val comment = comments[newItemPosition]
                    return old.getId() == comment.getId() && old.getPostedAt() === comment.getPostedAt() && old.getProductId() == comment.getProductId() && old.getText() == comment.getText()
                }
            })
            mCommentList = comments
            diffResult.dispatchUpdatesTo(this)
        }
    }

    /**
     * Called when RecyclerView needs a new `ViewHolder` of the given type to represent an item.
     * We create `CommentItemBinding binding` by inflating our layout `R.layout.comment_item`
     * (a `CommentItemBinding` is a view data binding class generated from the inflated layout,
     * Data-binding layout files are slightly different and start with a root tag of layout followed
     * by a data element and a view root element. This view element is what your root would be in a
     * non-binding layout file.) We then set the "callback" variable defined in the layout of
     * `binding` to our field `CommentClickCallback mCommentClickCallback` which is set
     * by our constructor when called from `ProductFragment` to a class which implements the
     * interface `CommentClickCallback` by defining an `onClick` method. Finally we return
     * a `CommentViewHolder` constructed using `binding`.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = DataBindingUtil
            .inflate<CommentItemBinding>(LayoutInflater.from(parent.context), R.layout.comment_item,
                parent, false)
        binding.callback = mCommentClickCallback
        return CommentViewHolder(binding)
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the `ViewHolder.itemView` to reflect the item at the given
     * position. We use the `setComment` method of the `binding` field of `holder`
     * to set the "comment" variable of our binding to the text in position `position` in our
     * `mCommentList` list of `Comment` objects, then call the `executePendingBindings`
     * of the `binding` field of `holder` to evaluate the pending bindings, updating any
     * Views that have expressions bound to modified variables.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.binding.comment = mCommentList!![position]
        holder.binding.executePendingBindings()
    }

    /**
     * Returns the total number of items in the data set held by the adapter. If `mCommentList`
     * is null we return 0, otherwise we return the size of `mCommentList`.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return if (mCommentList == null) 0 else mCommentList!!.size
    }

    /**
     * A ViewHolder class for our `Comment` objects, describes an item view and metadata about
     * its place within the RecyclerView.
     */
    class CommentViewHolder
    /**
     * Our constructor. First we call our super's constructor with the outermost View in the layout
     * file associated with the Binding `CommentItemBinding binding`, then we save our parameter
     * in our field `CommentItemBinding binding`.
     *
     * @param binding the `CommentItemBinding` containing our view and the variables we
     * use to modify the view.
     */(
        /**
         * `CommentItemBinding` we are constructed with, it is used to set the "comment"
         * variable in the `layout` to the appropriate text in `onBindViewHolder`
         */
        val binding: CommentItemBinding) : RecyclerView.ViewHolder(binding.root)
}