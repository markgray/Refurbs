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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.android.persistence.R
import com.example.android.persistence.databinding.ProductItemBinding
import com.example.android.persistence.db.entity.ProductEntity
import com.example.android.persistence.model.Product
import com.example.android.persistence.ui.ProductAdapter.ProductViewHolder

/**
 * [RecyclerView.Adapter] for [Product] objects (that interface is implemented by our
 * [ProductEntity] class in package com.example.android.persistence.db.entity). In our
 * constructor we just use our [ProductClickCallback] parameter as our [mProductClickCallback]
 * field.
 *
 * @param mProductClickCallback the [ProductClickCallback] implementation we should use. It is a
 * class which is defined in [ProductListFragment] and which overrides the `onClick` method to show
 * the product detail fragment. It is associated with the [ProductItemBinding] created from the
 * layout file layout/product_item.xml by the [onCreateViewHolder] override in [ProductAdapter].
 */
class ProductAdapter(
    private val mProductClickCallback: ProductClickCallback?
) : RecyclerView.Adapter<ProductViewHolder>() {
    /**
     * The list of `Product` objects we display.
     */
    var mProductList: List<Product>? = null

    /**
     * If the current list of [Product] (aka [ProductEntity]) objects in our [List] of [Product]
     * field [mProductList] is `null` we save our parameter [productList] in it and call the method
     * [notifyItemRangeInserted] to notify any registered observers that the items have been newly
     * inserted into the previously empty list. Otherwise we create [DiffUtil.DiffResult] variable
     * `val result` by calling the [DiffUtil.calculateDiff] method to calculate the list of update
     * operations that can convert [mProductList] into `productList`. `result` takes as its argument
     * an anonymous [DiffUtil.Callback] class (A Callback class used by DiffUtil while calculating
     * the diff between two lists) whose `getOldListSize` override returns the size of our field
     * [mProductList], whose `getNewListSize` override returns the size of our parameter [productList],
     * whose `areItemsTheSame` override returns `true` if the item id of the [Product] at its parameter
     * `oldItemPosition` in [mProductList] is the same as the id of the [Product] at its parameter
     * `newItemPosition` in `productList`, and whose override of `areContentsTheSame` returns `true`
     * if the [Product] at its parameter `oldItemPosition` in [mProductList] has the same id,
     * description, name, and price as the [Product] as the [Product] at its parameter `newItemPosition`
     * in `productList`. Finally we set our field [mProductList] to our parameter `productList`, and
     * call the [DiffUtil.DiffResult.dispatchUpdatesTo] method of `diffResult` to dispatch the update
     * events to "this" adapter.
     *
     * @param productList list of [Product] objects we should display.
     */
    fun setProductList(productList: List<Product>) {
        if (mProductList == null) {
            mProductList = productList
            notifyItemRangeInserted(0, productList.size)
        } else {
            val result: DiffUtil.DiffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return mProductList!!.size
                }

                override fun getNewListSize(): Int {
                    return productList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return mProductList!![oldItemPosition].getId() ==
                        productList[newItemPosition].getId()
                }

                @SuppressLint("NewApi")
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val newProduct = productList[newItemPosition]
                    val oldProduct = mProductList!![oldItemPosition]
                    return newProduct.getId() == oldProduct.getId()
                        && newProduct.getDescription() == oldProduct.getDescription()
                        && newProduct.getName() == oldProduct.getName()
                        && newProduct.getPrice() == oldProduct.getPrice()
                }
            })
            mProductList = productList
            result.dispatchUpdatesTo(this)
        }
    }

    /**
     * Called when RecyclerView needs a new [ProductViewHolder] of the given type to represent an
     * item. We create [ProductItemBinding] variable `val binding` by inflating our layout file
     * [R.layout.product_item] using the [DataBindingUtil.inflate] method (a [ProductItemBinding]
     * is a view data binding class generated from the inflated layout, Data-binding layout files
     * are slightly different and start with a root tag of layout followed by a data element and a
     * view root element. This view element is what your root would be in a non-binding layout file.)
     * We then set the "callback" variable defined in the layout of `binding` to our [ProductClickCallback]
     * field [mProductClickCallback] which is set by our constructor when called from [ProductListFragment]
     * to a class which implements the interface [ProductClickCallback] by defining an `onClick` method.
     * Finally we return a [ProductViewHolder] constructed using `binding`.
     *
     * @param parent   The [ViewGroup] into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new [View].
     * @return A new [ProductViewHolder] that holds a [View] of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding: ProductItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.product_item,
            parent,
            false
        )
        binding.callback = mProductClickCallback
        return ProductViewHolder(binding)
    }

    /**
     * Called by [RecyclerView] to display the data at the specified position. This method should
     * update the contents of the `ViewHolder.itemView` to reflect the item at the given
     * position. We use the `setProduct` method of the `binding` field of `holder`
     * to set the "product" variable of our binding to the `Product` in position `position`
     * in our `mProductList` list of `Product` objects, then call the `executePendingBindings`
     * of the `binding` field of `holder` to evaluate the pending bindings, updating any
     * Views that have expressions bound to modified variables.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.binding.product = mProductList!![position]
        holder.binding.executePendingBindings()
    }

    /**
     * Returns the total number of items in the data set held by the adapter. If `mProductList`
     * is null we return 0, otherwise we return the size of `mProductList`.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return if (mProductList == null) 0 else mProductList!!.size
    }

    /**
     * A ViewHolder class for our `Product` objects, describes an item view and metadata about
     * its place within the RecyclerView.
     */
    class ProductViewHolder
    /**
     * Our constructor. First we call our super's constructor with the outermost View in the layout
     * file associated with the Binding `ProductItemBinding binding`, then we save our parameter
     * in our field `ProductItemBinding binding`.
     *
     * @param binding the `ProductItemBinding` containing our view and the variables we
     * use to modify the view.
     */(
        /**
         * `ProductItemBinding` we are constructed with, it is used to set the "product"
         * variable in the `layout` to the appropriate text in `onBindViewHolder`
         */
        val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root)
}