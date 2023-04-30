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

import com.example.android.persistence.db.entity.CommentEntity
import java.util.Date

/**
 * Interface class used by our [CommentEntity] class
 */
interface Comment {
    /**
     * Returns a unique id for the row instance
     *
     * @return a unique id for the row
     */
    fun getId(): Int

    /**
     * Returns the product id we are the child of
     *
     * @return product id we are the child of
     */
    fun getProductId(): Int

    /**
     * Returns the text of the comment
     *
     * @return the text of the comment
     */
    fun getText(): String?

    /**
     * Returns the [Date] the comment was posted
     *
     * @return the [Date] the comment was posted
     */
    fun getPostedAt(): Date?
}