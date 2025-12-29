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

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database that contains the Users table, consisting of the @Entity annotated class
 * [User] which has one table "users"
 */
@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class UsersDatabase : RoomDatabase() {
    /**
     * Used to access the DAO of our database table "users"
     *
     * @return Our DAO
     */
    abstract val userDao: UserDao

    companion object {
        /**
         * Our singleton instance.
         */
        @Volatile
        private var INSTANCE: UsersDatabase? = null

        /**
         * Accessor for our singleton instance. If [INSTANCE] is `null` this is the first time we are
         * called so in a synchronized block if it is still `null` we create a [RoomDatabase.Builder]
         * for a persistent database for our @Database annotated [UsersDatabase] class with the
         * file name "Sample.db", and build it to initialize [INSTANCE]. In any case we then return
         * [INSTANCE] to the caller.
         *
         * @param context [Context] to use for the database, used to fetch the application context.
         * @return Our singleton instance of [UsersDatabase]
         */
        fun getInstance(context: Context): UsersDatabase {
            if (INSTANCE == null) {
                synchronized(UsersDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context = context.applicationContext,
                            klass = UsersDatabase::class.java,
                            name = "Sample.db"
                        ).build()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}