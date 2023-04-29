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
import com.example.android.persistence.db.entity.CommentEntity

/**
 * This class is the Data Access Object for the [CommentEntity] table "comments" as specified
 * by the Dao annotation.
 */
@Dao
interface CommentDao {
    /**
     * Returns a [List] of all the [CommentEntity] objects in the table "comments" of the
     * database whose `productId` column is equal to our parameter [productId] wrapped
     * in a [LiveData] object.
     *
     * @param productId value of the `productId` column we are interested in
     * @return a [LiveData] wrapped [List] of all [CommentEntity] whose `productId`
     * column matches our parameter [productId].
     */
    @Query("SELECT * FROM comments where productId = :productId")
    fun loadComments(productId: Int): LiveData<List<CommentEntity>>

    /**
     * Returns a list of all the [CommentEntity] objects in the table "comments" of the
     * database whose `productId` column is equal to our parameter [productId].
     *
     * @param productId value of the `productId` column we are interested in
     * @return a [List] of all [CommentEntity] whose `productId` column matches our parameter
     * [productId].
     */
    @Query("SELECT * FROM comments where productId = :productId")
    fun loadCommentsSync(productId: Int): List<CommentEntity>

    /**
     * Inserts a [List] of [CommentEntity] objects into the database, with a conflict strategy
     * of replacing the old data.
     *
     * @param comments [List] of [CommentEntity] objects to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(comments: List<CommentEntity>)
}