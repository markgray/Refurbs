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
package com.example.android.persistence.viewmodel

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.persistence.BasicApp
import com.example.android.persistence.DataRepository
import com.example.android.persistence.db.entity.CommentEntity
import com.example.android.persistence.db.entity.ProductEntity

/**
 * The purpose of this `AndroidViewModel` is to acquire and keep the information that is
 * necessary for our `ProductFragment` fragment and its "layout" file layout/product_fragment.xml.
 * Our constructor. First we call our super's constructor, then we save our parameter `productId`
 * in our field `int mProductId`. We initialize `mObservableComments` by initiating
 * a load from `DataRepository repository` of the list of `CommentEntity` objects for
 * the product with id `productId`, and then initialize `mObservableProduct` by
 * initiating a load from `DataRepository repository` of the `ProductEntity` with product
 * id `productId`.
 *
 * @param application `Application` to use as the life cycle scope for our `ViewModel`
 * @param repository  `DataRepository` to load our data from
 * @param productId   product ID of the `ProductEntity` we are concerned with.
 */
class ProductViewModel(
    application: Application,
    repository: DataRepository,
    productId: Int
) : AndroidViewModel(application) {
    /**
     * Expose the LiveData `ProductEntity` query so the UI can observe it. We just return a
     * reference to our field `LiveData<ProductEntity> mObservableProduct`.
     *
     * @return a reference to our field `LiveData<ProductEntity> mObservableProduct`
     */
    /**
     * `LiveData` data holder class for the observable `ProductEntity` we are concerned with
     */
    val observableProduct: LiveData<ProductEntity>

    /**
     * `ObservableField` that is used to bind our `ProductEntity` to the `View`
     */
    @JvmField
    var product: ObservableField<ProductEntity> = ObservableField<ProductEntity>()

    /**
     * Product ID of the `ProductEntity` we are concerned with
     */
    private var mProductId = 0
    /**
     * Expose the LiveData Comments query so the UI can observe it. We just return a reference to
     * our field `LiveData<List<CommentEntity>> mObservableComments`.
     *
     * @return a reference to our field `LiveData<List<CommentEntity>> mObservableComments`
     */
    /**
     * `LiveData` query for the `CommentEntity` objects for our `ProductEntity`
     */
    val comments: LiveData<List<CommentEntity>>

    /**
     *
     */
    init {
        mProductId = productId
        comments = repository.loadComments(mProductId)
        observableProduct = repository.loadProduct(mProductId)
    }

    /**
     * Setter for our `ObservableField` `product`, called from `ProductFragment`
     * when the database read has completed. We just call the `set` method of our field
     * `product` to set it to our parameter `ProductEntity product`.
     *
     * @param product `ProductEntity` we have loaded, now ready to be displayed
     */
    fun setProduct(product: ProductEntity) {
        this.product.set(product)
    }

    /**
     * A creator is used to inject the product ID into the ViewModel This creator is to showcase
     * how to inject dependencies into ViewModels. It's not actually necessary in this case, as
     * the product ID can be passed in a public method.
     *
     * Our constructor. We save our parameters in our fields `mApplication` and
     * `mProductId`, then initialize our field `DataRepository mRepository` by
     * calling the `getRepository` method of our parameter `application`.
     *
     * @param mApplication `Application` to use for the scope of the `ProductViewModel`
     * we create
     * @param mProductId   Product id of the `ProductEntity` to use when we create a
     * `ProductViewModel`
     */
    class Factory(
        /**
         * `Application` to use for the scope of the `ProductViewModel` our `Factory`
         * creates.
         */
        private val mApplication: Application,
        /**
         * Product id of the `ProductEntity` to use when we create a `ProductViewModel`
         */
        private val mProductId: Int
    ) : ViewModelProvider.NewInstanceFactory() {
        /**
         * `DataRepository` to use when we create a `ProductViewModel`.
         */
        private var mRepository: DataRepository = (mApplication as BasicApp).repository

        /**
         * Returns a `ProductViewModel` constructed using our fields `mApplication`,
         * `mRepository` and `mProductId`.
         *
         * @param modelClass The class of the ViewModel to create an instance of it if it is not
         * present. UNUSED
         * @param <T>        The type parameter for the ViewModel.
         * @return A ViewModel that is an instance of the given type `T`.
        </T> */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(mApplication, mRepository, mProductId) as T
        }
    }
}