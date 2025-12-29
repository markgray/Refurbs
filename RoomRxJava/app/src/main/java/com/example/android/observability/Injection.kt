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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.example.android.observability

import android.content.Context
import com.example.android.observability.persistence.LocalUserDataSource
import com.example.android.observability.persistence.UserDao
import com.example.android.observability.persistence.UsersDatabase
import com.example.android.observability.ui.ViewModelFactory

/**
 * Enables injection of data sources.
 */
object Injection {
    /**
     * Creates a new [LocalUserDataSource]. First we get the singleton instance of our database
     * to initialize [UsersDatabase] variable `val database`, then we return a new instance of
     * [LocalUserDataSource] constructed using the DAO of `database`.
     *
     * @param context Context to use for the database.
     * @return new instance of [LocalUserDataSource] which uses the instance of [UserDao] returned
     * by the [UsersDatabase.userDao] property of the singleton [UsersDatabase] as the DAO of our
     * database table "users"
     */
    fun provideUserDataSource(context: Context): UserDataSource {
        val database = UsersDatabase.getInstance(context = context)
        return LocalUserDataSource(mUserDao = database.userDao)
    }

    /**
     * Creates a new [ViewModelFactory] by using our apps instance of [UserDataSource] to
     * construct one.
     *
     * @param context the [Context] to use for the database.
     * @return an instance of [ViewModelFactory] constructed to use our [LocalUserDataSource].
     */
    fun provideViewModelFactory(context: Context): ViewModelFactory {
        val dataSource = provideUserDataSource(context = context)
        return ViewModelFactory(mDataSource = dataSource)
    }
}