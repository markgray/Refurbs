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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.example.android.persistence.R
import com.example.android.persistence.databinding.ListFragmentBinding
import com.example.android.persistence.model.Product
import com.example.android.persistence.viewmodel.ProductListViewModel

/**
 * Fragment used to display our list of `Product` objects.
 */
class ProductListFragment : Fragment() {
    /**
     * `ProductAdapter` used to hold and display our `Product` list in our
     * `RecyclerView`
     */
    private var mProductAdapter: ProductAdapter? = null

    /**
     * `ListFragmentBinding` auto-generated from our "layout" file layout/list_fragment.xml
     * that we use for binding to that layout.
     */
    private var mBinding: ListFragmentBinding? = null


    /**
     * Called to have the fragment instantiate its user interface view. First we initialize our field
     * `ListFragmentBinding mBinding` by inflating the binding layout R.layout.list_fragment
     * into a binding for that layout. Next we initialize `ProductAdapter mProductAdapter` with
     * a new instance using `ProductClickCallback mProductClickCallback` as its callback, and
     * then use `mBinding` to set the adapter of the `RecyclerView` in its layout file
     * with id android:id="@+id/products_list" to `mProductAdapter`. Finally we return the
     * outermost View in the layout file associated with the Binding `mBinding` to the caller.
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
    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.list_fragment, container, false)
        mProductAdapter = ProductAdapter(mProductClickCallback)
        mBinding!!.productsList.adapter = mProductAdapter
        return mBinding!!.root
    }

    /**
     * Called when the fragment's activity has been created and this fragment's view hierarchy
     * instantiated. First we call our super's implementation of `onActivityCreated`. We
     * initialize `ProductListViewModel viewModel` with a `ViewModel` for the class
     * `ProductListViewModel` obtained from a `ViewModelProvider` for this fragment,
     * and call our method `subscribeUi` to add a `Observer` callback to `viewModel`
     * for the field `MediatorLiveData<List<ProductEntity>> mObservableProducts` in our
     * `ProductListViewModel`.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Deprecated("Deprecated in Java") // TODO: Replace onActivityCreated
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION") // TODO: Replace onActivityCreated
        super.onActivityCreated(savedInstanceState)
        val viewModel = ViewModelProvider(this)[ProductListViewModel::class.java]
        subscribeUi(viewModel)
    }

    /**
     * Adds a `Observer` callback to its parameter `viewModel` for the field
     * `MediatorLiveData<List<ProductEntity>> mObservableProducts`. We use the `getProducts`
     * method of our parameter `viewModel` to get a reference to its field `mObservableProducts`
     * and call its `observe` method to add an anonymous class of `Observer` to it whose
     * `onChanged` override (if its parameter `myProducts` is not null) sets the product
     * list of `mProductAdapter` to `myProducts`, and sets the "isLoading" variable of
     * the `mBinding` bound layout to false. Otherwise it sets the "isLoading" variable of the
     * `mBinding` bound layout to true. Finally the callback calls the `executePendingBindings`
     * method of `mBinding` to sync the variable change.
     *
     * @param viewModel `ProductListViewModel` whose field `mObservableProducts` we are
     * to observe
     */
    private fun subscribeUi(viewModel: ProductListViewModel) {
        // Update the list when the data changes
        viewModel.products.observe(viewLifecycleOwner) { myProducts ->
            if (myProducts != null) {
                mBinding!!.isLoading = false
                mProductAdapter!!.setProductList(myProducts)
            } else {
                mBinding!!.isLoading = true
            }
            // espresso does not know how to wait for data binding's loop so we execute changes
            // sync.
            mBinding!!.executePendingBindings()
        }
    }

    /**
     * `ProductClickCallback` callback we pass to the `ProductAdapter` constructor, which
     * it uses to set the "callback" variable in its layout/product_item.xml "layout".
     */
    private val mProductClickCallback = object: ProductClickCallback() {
        /**
         * This is used in a lambda specified using android:onClick in the layout file for a product
         * item R.layout.product_item, and is called with the bound variable "comment" from that file.
         * When clicked if our lifecycle is at least in the STARTED state, we call the `getActivity`
         * for our main activity `MainActivity` to get the `FragmentActivity` this fragment
         * is currently associated with and instruct it to show the product detail fragment for our
         * parameter `Product product`.
         *
         * @param product the `Product` item that has been clicked
         */
        override fun onClick(product: Product?) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                (activity as MainActivity?)!!.show(product!!)
            }
        }

    }

    companion object {
        /**
         * TAG name used when adding this Fragment
         */
        const val TAG: String = "ProductListViewModel"
    }
}