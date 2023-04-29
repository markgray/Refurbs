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
package com.example.android.persistence.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.android.persistence.db.entity.ProductEntity

/**
 * This class is the Data Access Object for the [ProductEntity] table "products" as specified
 * by the Dao annotation.
 */
@Dao
interface ProductDao {
    /**
     * Loads all the [ProductEntity] objects in the "products" table of the data base into a
     * [LiveData] wrapped [List].
     *
     * @return a [LiveData] wrapped [List] of all the [ProductEntity] objects in the data base
     */
    @Query("SELECT * FROM products")
    fun loadAllProducts(): LiveData<List<ProductEntity>>

    /**
     * Inserts a list of [ProductEntity] objects into the database, with a conflict strategy
     * of replacing the old data.
     *
     * @param products [List] of [ProductEntity] objects to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products: List<ProductEntity>)

    /**
     * Retrieves the [ProductEntity] in the "products" table of the data base with the
     * `id` column value equal to our parameter [productId] wrapped in a [LiveData] object.
     *
     * @param productId `id` column value of product to retrieve
     * @return the [LiveData] wrapped [ProductEntity] requested.
     */
    @Query("select * from products where id = :productId")
    fun loadProduct(productId: Int): LiveData<ProductEntity>

    /**
     * Retrieves the [ProductEntity] in the "products" table of the data base with the
     * `id` column value equal to our parameter [productId]. UNUSED
     *
     * @param productId `id` column value of product to retrieve
     * @return the [ProductEntity] requested.
     */
    @Query("select * from products where id = :productId")
    fun loadProductSync(productId: Int): ProductEntity
}