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
package com.example.android.persistence.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.android.persistence.model.Product

/**
 * This is a row in the table "products" of our Room database. The Entity annotation marks this class
 * as an entity. This class will have a mapping SQLite table in the database, and all fields will be
 * automatically persisted. The tableName property sets the table name used to "products", and the
 * primary key is our field `int id` (indicated by the PrimaryKey annotation).
 */
@Entity(tableName = "products")
class ProductEntity : Product {
    /**
     * Our primary key is a unique `int` for each row, generated in the class `DataGenerator`
     * by the method `generateProducts`.
     */
    @PrimaryKey
    private var id = 0

    /**
     * Name of our product, generated in the class `DataGenerator` by the method
     * `generateProducts`.
     */
    private var name: String? = null

    /**
     * Description of our product, generated in the class `DataGenerator` by the method
     * `generateProducts`.
     */
    private var description: String? = null

    /**
     * Price of our product, generated in the class `DataGenerator` by the method
     * `generateProducts`.
     */
    private var price = 0

    /**
     * Getter for the `int id` field.
     *
     * @return the value in our field `id`
     */
    override fun getId(): Int {
        return id
    }

    /**
     * Setter for the `int id` field
     *
     * @param id value to set our field `int id` to
     */
    fun setId(id: Int) {
        this.id = id
    }

    /**
     * Getter for the `String name` field.
     *
     * @return the value in our field `String name`
     */
    override fun getName(): String {
        return name!!
    }

    /**
     * Setter for the `String name` field
     *
     * @param name value to set our field `String name` to
     */
    fun setName(name: String?) {
        this.name = name
    }

    /**
     * Getter for the `String description` field
     *
     * @return the value in our field `String description`
     */
    override fun getDescription(): String {
        return description!!
    }

    /**
     * Setter for the `String description` field
     *
     * @param description value to set our field `String description` to
     */
    fun setDescription(description: String?) {
        this.description = description
    }

    /**
     * Getter for the `int prince` field
     *
     * @return value in our field `int price`
     */
    override fun getPrice(): Int {
        return price
    }

    /**
     * Setter for the `int price` field
     *
     * @param price value to set our field `int price` to
     */
    fun setPrice(price: Int) {
        this.price = price
    }

    /**
     * Our empty constructor, does nothing
     */
    constructor()

    /**
     * Constructor that initializes all fields.
     *
     * @param id value to set our field `int id` to
     * @param name value to set our field `String name` to
     * @param description value to set our field `String description` to
     * @param price value to set our field `int price` to
     */
    constructor(id: Int, name: String?, description: String?, price: Int) {
        this.id = id
        this.name = name
        this.description = description
        this.price = price
    }

    /**
     * Our copy constructor.
     *
     * @param product `Product` object to copy
     */
    constructor(product: Product) {
        id = product.getId()
        name = product.getName()
        description = product.getDescription()
        price = product.getPrice()
    }
}