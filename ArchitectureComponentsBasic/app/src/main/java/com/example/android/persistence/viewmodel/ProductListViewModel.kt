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
@file:Suppress("JoinDeclarationAndAssignment")

package com.example.android.persistence.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.example.android.persistence.BasicApp
import com.example.android.persistence.db.entity.ProductEntity

/**
 * The purpose of this `AndroidViewModel` is to acquire and keep the information that is
 * necessary for our `ProductListFragment` fragment. First we call our super's constructor,
 * then we initialize our field [mObservableProducts] with a new instance
 * of [MediatorLiveData] for data of type [List] of [ProductEntity] and set its value
 * to `null`. We initialize `LiveData<List<ProductEntity>> products` by launching an
 * asynchronous query to our database to retrieve the list of `ProductEntity` objects in
 * it, and finally we add an observer for `products` whose `onChanged` method is
 * the `setValue` method of `mObservableProducts`.
 *
 * @param application a fragment, in whose scope this ViewModel should be retained
 */
class ProductListViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * MediatorLiveData can observe other LiveData objects and react on their emissions. Our constructor
     * adds an `Observer` to the list of products from the database (which is  get notified when
     * the data changes) and the `subscribeUi` method of `ProductListFragment` also adds an
     * `Observer` which updates the UI when this data is changed.
     */
    private val mObservableProducts: MediatorLiveData<List<ProductEntity>?>

    init {
        mObservableProducts = MediatorLiveData()
        // set by default null, until we get data from the database.
        mObservableProducts.value = null
        val products: LiveData<List<ProductEntity>> = (application as BasicApp).repository.products

        // observe the changes of the products from the database and forward them
        mObservableProducts.addSource(products, mObservableProducts::setValue)
    }

    /**
     * Expose the LiveData Products query so the UI can observe it. We just return a reference to
     * our field `MediatorLiveData<List<ProductEntity>> mObservableProducts`.
     *
     * @return a reference to our field `MediatorLiveData<List<ProductEntity>> mObservableProducts`
     */
    val products: LiveData<List<ProductEntity>?>
        get() = mObservableProducts
}