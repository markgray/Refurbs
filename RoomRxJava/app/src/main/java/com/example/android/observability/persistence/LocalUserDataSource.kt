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

import androidx.room.RoomDatabase
import com.example.android.observability.Injection
import com.example.android.observability.UserDataSource
import io.reactivex.Flowable

/**
 * Using the Room database as a data source. [UserDataSource] extends [RoomDatabase] and
 * is annotated with: Database(entities = {User.class}, version = 1)
 */
class LocalUserDataSource
/**
 * Our constructor, called from the `provideUserDataSource` method of the [Injection]
 * class. We simply save our parameter `UserDao userDao` in our field `UserDao mUserDao`.
 *
 * @param mUserDao the DAO we should use to access our database
 */(
    /**
     * The DAO instance we use, it is set by our constructor.
     */
    private val mUserDao: UserDao) : UserDataSource {
    /**
     * Uses our [UserDao] field [mUserDao] to retrieve the [User] object from the database wrapped
     * in a [Flowable] that will emit every time the user name has been updated
     *
     * @return [User] returned by the DAO field [mUserDao] wrapped in a flowable that will
     * emit every time the user name has been updated.
     */
    override fun getUser(): Flowable<User> {
        return mUserDao.user
    }

    /**
     * Inserts the user into the data source, or, if this is an existing user, updates it. We just
     * call the [UserDao.insertUser] method of our DAO field [mUserDao] to insert or update the user
     * entry in the database.
     *
     * @param user the user to be inserted or updated.
     */
    override fun insertOrUpdateUser(user: User) {
        mUserDao.insertUser(user)
    }

    /**
     * Deletes all users from the data source. Used only for testing. We just call the
     * [UserDao.deleteAllUsers] method of our DAO field [mUserDao] which runs the query
     * "DELETE FROM Users" to do the job.
     */
    override fun deleteAllUsers() {
        mUserDao.deleteAllUsers()
    }
}