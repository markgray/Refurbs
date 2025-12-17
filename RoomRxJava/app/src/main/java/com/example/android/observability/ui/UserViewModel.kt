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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.observability.ui

import androidx.lifecycle.ViewModel
import com.example.android.observability.UserDataSource
import com.example.android.observability.persistence.User
import io.reactivex.Completable
import io.reactivex.functions.Action
import io.reactivex.Flowable
import java.util.UUID

/**
 * View Model for the [UserActivity]
 */
class UserViewModel
/**
 * Our constructor, called from the [ViewModelFactory.create] method of [ViewModelFactory]. We just
 * save our [UserDataSource] parameter in our [UserDataSource] field [mDataSource]
 *
 * @param mDataSource the [UserDataSource] we are to use for our data
 */
(
    /**
     * `UserDataSource` we are constructed to use
     */
    private val mDataSource: UserDataSource
) : ViewModel() {
    /**
     * [User] we are observing and displaying
     */
    private var mUser: User? = null

    /**
     * Get the user name of the user. We call the [Flowable.map] method of the [Flowable] of [User]
     * returned from the [UserDataSource.getUser] method of our [UserDataSource] field [mDataSource]
     * to specify a lambda which will be executed every time that the [Flowable] of [User] emits an
     * item. This lambda will use the [User] `user` it receives to update our [User] field [mUser]
     * and will then return the string returned by the method [User.userName] propery of `user`.
     *
     * @return a [Flowable] that will emit every time the user name has been updated.
     */
    val userName: Flowable<String>
        get() = mDataSource.getUser() // for every emission of the user, get the user name
            .map { user: User ->
                mUser = user
                user.userName
            }

    /**
     * Update the user name. We return a [Completable] created by its [Completable.fromAction]
     * method using a lambda whose `run` [Action] sets our [User] field [mUser] depending on its
     * current value:
     *
     *  * `null` - creates a new [User] from the [String] value of a random [UUID] and our [String]
     *  parameter [userName].
     *
     *  * not `null` - creates a new [User] from our [String] parameter [userName] with the same ID
     *  as the old value of [mUser].
     *
     * The lambda finally calls the [UserDataSource.insertOrUpdateUser] method of [UserDataSource]
     * field [mDataSource] to insert (or update) [mUser] into the database.
     *
     * @param userName the new user name
     * @return a [Completable] that completes when the user name is updated
     */
    fun updateUserName(userName: String): Completable {
        return Completable.fromAction {

            // if there's no use, create a new user.
            // if we already have a user, then, since the user object is immutable,
            // create a new user, with the id of the previous user and the updated user name.
            mUser = if (mUser == null) {
                User(UUID.randomUUID().toString(), userName)
            } else {
                User(mUser!!.id, userName)
            }
            mDataSource.insertOrUpdateUser(mUser!!)
        }
    }
}