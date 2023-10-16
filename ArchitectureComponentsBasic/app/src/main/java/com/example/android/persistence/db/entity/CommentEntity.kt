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
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.android.persistence.model.Comment
import java.util.Date

/**
 * This is a row in the table "comments" of our Room database. The `Entity` annotation marks this
 * class as an entity. This class will have a mapping SQLite table in the database, and all fields
 * will be automatically persisted. The `tableName` property gives the name of the table as "comments".
 * The `foreignKeys` property defines a [ForeignKey] constraint for the entity [ProductEntity], with
 * the `parentColumns` "id" linked to our childColumns field "productId", with the `onDelete` action
 * to take specified as [ForeignKey.CASCADE] (propagates the delete operation on the parent key to
 * each dependent child key). Indices for our entity are specified to be only the column "productId".
 */
@Entity(tableName = "comments",
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("productId"),
            onDelete = CASCADE)],
    indices = [Index(value = arrayOf("productId"))])
class CommentEntity : Comment {
    /**
     * The unique primary key for this row. The `autoGenerate` property of the `PrimaryKey`
     * annotation specifies that SQLite should generate a unique id.
     */
    @PrimaryKey(autoGenerate = true)
    private var id = 0

    /**
     * The `id` of the [ProductEntity] that we are a comment to. It is the `childColumns`
     * column of the foreign key that links us.
     */
    private var productId = 0

    /**
     * Text of the comment that we contain
     */
    private var text: String? = null

    /**
     * [Date] the comment was posted.
     */
    private var postedAt: Date? = null

    /**
     * Getter for the value in our [id] field.
     *
     * @return the value in our [id] field
     */
    override fun getId(): Int {
        return id
    }

    /**
     * Setter for our `id` field.
     *
     * @param id value to set our `id` field to
     */
    @Suppress("unused")
    fun setId(id: Int) {
        this.id = id
    }

    /**
     * Getter for the value in our `productId` field.
     *
     * @return the value in our `productId` field.
     */
    override fun getProductId(): Int {
        return productId
    }

    /**
     * Setter for our `productId` field.
     *
     * @param productId value to set our `productId` field to
     */
    fun setProductId(productId: Int) {
        this.productId = productId
    }

    /**
     * Getter for the value in our `text` field.
     *
     * @return the value in our `text` field.
     */
    override fun getText(): String {
        return text!!
    }

    /**
     * Setter for our `text` field.
     *
     * @param text value to set our `text` field to
     */
    fun setText(text: String?) {
        this.text = text
    }

    /**
     * Getter for the value in our `postedAt` field.
     *
     * @return the value in our `postedAt` field.
     */
    override fun getPostedAt(): Date {
        return postedAt!!
    }

    /**
     * Setter for our `postedAt` field.
     *
     * @param postedAt value to set our `postedAt` field to
     */
    fun setPostedAt(postedAt: Date?) {
        this.postedAt = postedAt
    }

    /**
     * Our empty constructor, does nothing.
     */
    @Ignore
    constructor()

    /**
     * Version of our constructor which sets all our fields.
     *
     * @param id        value to set our `id` field to
     * @param productId value to set our `productId` field to
     * @param text      value to set our `text` field to
     * @param postedAt  value to set our `postedAt` field to
     */
    constructor(id: Int, productId: Int, text: String?, postedAt: Date?) {
        this.id = id
        this.productId = productId
        this.text = text
        this.postedAt = postedAt
    }
}