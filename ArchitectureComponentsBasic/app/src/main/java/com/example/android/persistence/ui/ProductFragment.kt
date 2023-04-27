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
@file:Suppress("RedundantNullableReturnType", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.persistence.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.android.persistence.R
import com.example.android.persistence.databinding.ProductFragmentBinding
import com.example.android.persistence.model.Comment
import com.example.android.persistence.viewmodel.ProductViewModel

/**
 * `Fragment` which displays the information about a particular `Product` using the layout
 * file layout/product_fragment.xml (this layout file has two bound variables "isLoading" and
 * "productViewModel")
 */
class ProductFragment : Fragment() {
    /**
     * `ProductFragmentBinding` created when we use `DataBindingUtil` to inflate our
     * layout file layout/product_fragment.xml
     */
    private var mBinding: ProductFragmentBinding? = null

    /**
     * `CommentAdapter` for the `Comment` list we display for this `Product`
     */
    private var mCommentAdapter: CommentAdapter? = null

    /**
     * Called to have the fragment instantiate its user interface view. First we inflate our data
     * binding layout R.layout.product_fragment to create `ProductFragmentBinding mBinding`.
     * We create a new instance for `CommentAdapter mCommentAdapter`, and use our field
     * `mBinding` to set the adapter of the `commentList` (the android:id="@+id/comment_list"
     * `RecyclerView` inside of our layout) to `mCommentAdapter`. Finally we return the
     * the outermost View in the layout file associated with the Binding `mBinding` to the
     * caller.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate this data binding layout
        mBinding = DataBindingUtil.inflate(inflater, R.layout.product_fragment, container, false)

        // Create and set the adapter for the RecyclerView.
        mCommentAdapter = CommentAdapter(mCommentClickCallback)
        mBinding!!.commentList.adapter = mCommentAdapter
        return mBinding!!.root
    }

    /**
     * `CommentClickCallback` we use when we create the `CommentAdapter` for our list of
     * `Comment` object, it is used in a lambda specified using android:onClick in the layout
     * file for a comment item R.layout.comment_item.
     */
    private val mCommentClickCallback = object: CommentClickCallback() {
        /**
         * This is used in a lambda specified using android:onClick in the layout file for a comment
         * item R.layout.comment_item, and is called with the bound variable "comment" from that file.
         *
         * @param comment the `Comment` item that has been clicked
         */
        override fun onClick(comment: Comment?) {
            Log.i("CommentClickCallback", "We have been clicked: " + comment!!.getText())
        }
    }

    /**
     * Called when the fragment's activity has been created and this fragment's view hierarchy
     * instantiated. First we call our super's implementation of `onActivityCreated`. Next we
     * create an instance for `ProductViewModel.Factory factory` using the int stored under
     * the key KEY_PRODUCT_ID in the arguments supplied when the fragment was instantiated as the
     * product id. We use `factory` to create `ProductViewModel model` (a ViewModelProvider,
     * which retains ViewModels while a scope our fragment is alive is created to use `factory`,
     * and this is used to create a ViewModel with the class `ProductViewModel`). We then use
     * `mBinding` to set the "productViewModel" variable in our layout file. Finally we call
     * our method `subscribeToModel` to add `Observer` callbacks to `model` for the
     * observable field `product` of the `ProductEntity` we display, and the list of
     * `CommentEntity` objects we display for our `Product`.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        val factory = ProductViewModel.Factory(
            requireActivity().application, requireArguments().getInt(KEY_PRODUCT_ID))
        val model = ViewModelProvider(this, factory)[ProductViewModel::class.java]
        mBinding!!.productViewModel = model
        subscribeToModel(model)
    }

    /**
     * Adds `Observer` callbacks to `model` for the observable field `product` of
     * the `ProductEntity` we display, and the list of `CommentEntity` objects we display
     * for our `Product`.
     *
     * @param model `ProductViewModel` we are using
     */
    private fun subscribeToModel(model: ProductViewModel) {

        // Observe product data
        model.observableProduct.observe(viewLifecycleOwner) { productEntity ->
            /**
             * Called when the data is changed. We just use `model` to set the value of its
             * `ObservableField` `product` to the new `productEntity`.
             *
             * @param productEntity The new data
             */
            /**
             * Called when the data is changed. We just use `model` to set the value of its
             * `ObservableField` `product` to the new `productEntity`.
             *
             * @param productEntity The new data
             */
            model.setProduct(productEntity)
        }

        // Observe comments
        model.comments.observe(viewLifecycleOwner) { commentEntities ->

            /**
             * Called when the data is changed. If our parameter `commentEntities` is not null
             * we use `mBinding` to set the bound variable "isLoading" to false, and use
             * `mCommentAdapter` to set the list used by the android:id="@+id/comment_list"
             * `RecyclerView` to `commentEntities`. If `commentEntities` is null,
             * we use `mBinding` to set the bound variable "isLoading" to true.
             *
             * @param commentEntities The new data
             */
            if (commentEntities != null) {
                mBinding!!.isLoading = false
                mCommentAdapter!!.setCommentList(commentEntities)
            } else {
                mBinding!!.isLoading = true
            }
        }
    }

    companion object {
        /**
         * Product ID of the `Product` we are displaying
         */
        private const val KEY_PRODUCT_ID = "product_id"

        /**
         * Creates product fragment for specific product ID. First we create a new instance to initialize
         * `ProductFragment fragment`, then we create a new instance for `Bundle args` and
         * add `productId` to it under the key KEY_PRODUCT_ID. We set the arguments of `fragment`
         * to `args` and return `fragment` to the caller.
         *
         * @param productId product ID of the `Product` we are supposed to display
         * @return a new instance of `ProductFragment` created to display the `Product` with
         * product ID `productId`
         */
        fun forProduct(productId: Int): ProductFragment {
            val fragment = ProductFragment()
            val args = Bundle()
            args.putInt(KEY_PRODUCT_ID, productId)
            fragment.arguments = args
            return fragment
        }
    }
}