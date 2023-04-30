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
package com.example.android.persistence.db

import com.example.android.persistence.db.entity.CommentEntity
import com.example.android.persistence.db.entity.ProductEntity
import java.util.Date
import java.util.Random
import java.util.concurrent.TimeUnit

/**
 * Generates data to pre-populate the database
 */
object DataGenerator {
    /**
     * First parts randomly used for the [ProductEntity.name] of the [ProductEntity].
     */
    private val FIRST = arrayOf("Special edition", "New", "Cheap", "Quality", "Used")

    /**
     * Second parts randomly used for the [ProductEntity.name] of the [ProductEntity].
     */
    private val SECOND = arrayOf("Three-headed Monkey", "Rubber Chicken", "Pint of Grog", "Monocle")

    /**
     * "description" randomly used for the [ProductEntity.description] of the [ProductEntity]
     */
    private val DESCRIPTION = arrayOf(
        "is finally here",
        "is recommended by Stan S. Stanman",
        "is the best sold product on Mêlée Island",
        "is \uD83D\uDCAF",
        "is ❤️",
        "is fine"
    )

    /**
     * "text" randomly used for the [CommentEntity.text] of the [CommentEntity].
     */
    private val COMMENTS = arrayOf(
        "Comment 1", "Comment 2", "Comment 3",
        "Comment 4", "Comment 5", "Comment 6"
    )

    /**
     * Generates and returns a [List] of [ProductEntity] objects which use random strings for the
     * name, description, and price.
     *
     * @return list of random [ProductEntity] objects
     */
    @JvmStatic
    fun generateProducts(): List<ProductEntity> {
        val products: MutableList<ProductEntity> = ArrayList(FIRST.size * SECOND.size)
        val rnd = Random()
        for (i in FIRST.indices) {
            for (j in SECOND.indices) {
                val product = ProductEntity()
                product.setName(FIRST[i] + " " + SECOND[j])
                product.setDescription(product.getName() + " " + DESCRIPTION[j])
                product.setPrice(rnd.nextInt(240))
                product.setId(FIRST.size * i + j + 1)
                products.add(product)
            }
        }
        return products
    }

    /**
     * Generates and returns a [List] of [CommentEntity] objects which use randomly chosen strings
     * for the text, and arbitrary times for the `postedAt` times, using each of the `ProductEntity`
     * objects in our [List] of [ProductEntity] parameter [products] in turn as a target parent.
     *
     * @param products list or [ProductEntity] objects we produce comments for.
     * @return list of randomly generated [CommentEntity] objects for the [ProductEntity] objects in
     * our our [List] of [ProductEntity] parameter [products].
     */
    @JvmStatic
    fun generateCommentsForProducts(
        products: List<ProductEntity>
    ): List<CommentEntity> {
        val comments: MutableList<CommentEntity> = ArrayList()
        val rnd = Random()
        for (product in products) {
            val commentsNumber = rnd.nextInt(5) + 1
            for (i in 0 until commentsNumber) {
                val comment = CommentEntity()
                comment.setProductId(product.getId())
                comment.setText(COMMENTS[i] + " for " + product.getName())
                comment.setPostedAt(Date(System.currentTimeMillis()
                    - TimeUnit.DAYS.toMillis((commentsNumber - i).toLong()) + TimeUnit.HOURS.toMillis(i.toLong())))
                comments.add(comment)
            }
        }
        return comments
    }
}