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
package com.example.android.persistence.model

import com.example.android.persistence.db.entity.ProductEntity

/**
 * Interface for a [Product] object, it is implemented by the [ProductEntity] class
 */
interface Product {
    /**
     * Getter for a unique [Int] ID value for the [Product].
     *
     * @return returns a unique [Int] ID value for the [Product].
     */
    fun getId() : Int
    /**
     * Getter for a [String] to use as the name of the product
     *
     * @return [String] to use as the name of the product
     */
    fun getName() : String

    /**
     * Getter for a [String] to use as the description of the product
     *
     * @return [String] to use as the description of the product
     */
    fun getDescription(): String

    /**
     * Getter for a [Int] to use as the price of the product
     *
     * @return [Int] to use as the price of the product
     */
    fun getPrice(): Int
}