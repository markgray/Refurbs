/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.example.android.observability.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Immutable model class for a User
 */
@Entity(tableName = "users")
data class User(
    /**
     * The unique id we use as our primary key
     */
    @PrimaryKey
    @ColumnInfo(name = "userid")
    val id: String,
    /**
     * Column in the data base where we store the current user name.
     */
    @ColumnInfo(name = "username")
    val userName: String
)
