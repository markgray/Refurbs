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
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.example.android.observability.ui.UserViewModel
import java.util.UUID

/**
 * Immutable model class for a User
 */
@Entity(tableName = "users")
class User {
    /**
     * The unique id we use as our primary key
     */
    @PrimaryKey
    @ColumnInfo(name = "userid")
    val id: String

    /**
     * Column in the data base where we store the current user name.
     */
    @ColumnInfo(name = "username")
    val userName: String

    /**
     * Our constructor. The Ignore annotation tells the Room processor to ignore this constructor.
     * Called from the [UserViewModel.updateUserName] method of [UserViewModel] when the database is
     * first created. We initialize our [String] field [id] (the "userid" column) with a randomly
     * generated UUID converted to a string, and set our [String] field [userName] (the "username"
     * column) to our [String] parameter [userName].
     *
     * @param userName user name to store in our database
     */
    @Ignore
    constructor(userName: String) {
        id = UUID.randomUUID().toString()
        this.userName = userName
    }

    /**
     * Our two argment constructor. Called from the [UserViewModel.updateUserName] method of
     * [UserViewModel] when the entry for the user name is being updated. We set our [String] field
     * [id] (the "userid" column) to our [String] parameter [id] and our [String] field [newUserName]
     * (the "username" column) to our [String] parameter [newUserName]
     *
     * @param id       UUID to use for the "userid" column.
     * @param newUserName user name to use for the "username" column
     */
    constructor(id: String, newUserName: String) {
        this.id = id
        this.userName = newUserName
    }
}