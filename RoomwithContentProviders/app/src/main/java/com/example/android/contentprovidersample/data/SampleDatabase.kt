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
@file:Suppress("unused")

package com.example.android.contentprovidersample.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.room.RoomDatabase

/**
 * The Room database, with only one table: the Entity annotated [Cheese] class.
 */
@Database(entities = [Cheese::class], version = 1, exportSchema = false)
abstract class SampleDatabase : RoomDatabase() {
    /**
     * @return The DAO for the Cheese table.
     */
    abstract fun cheese(): CheeseDao

    /**
     * Inserts the dummy data into the database if it is currently empty. If the DAO for the cheese
     * table has 0 elements in it, we initialize [Cheese] variable `val cheese` with a new instance
     * then we begin a transaction in EXCLUSIVE mode. Then wrapped in a try block we loop through
     * all the strings in the array [Cheese.CHEESES] setting the [Cheese.name] field of `cheese`
     * to the string and using the cheese table DAO, insert `cheese` into the database. When
     * done with the cheese names we mark the current transaction as successful. The finally block
     * then calls the [endTransaction] method to end the transaction.
     */
    private fun populateInitialData() {
        if (cheese().count() == 0) {
            val cheese = Cheese()
            @Suppress("DEPRECATION") // TODO: Replace With runInTransaction(Runnable)
            beginTransaction()
            try {
                for (i in Cheese.CHEESES.indices) {
                    cheese.name = Cheese.CHEESES[i]
                    cheese().insert(cheese)
                }
                @Suppress("DEPRECATION") // TODO: Replace With runInTransaction(Runnable)
                setTransactionSuccessful()
            } finally {
                @Suppress("DEPRECATION") // TODO: Replace With runInTransaction(Runnable)
                endTransaction()
            }
        }
    }

    companion object {
        /**
         * The only instance
         */
        private var sInstance: SampleDatabase? = null

        /**
         * Gets the singleton instance of SampleDatabase. This synchronized method first checks to
         * see if our [SampleDatabase] field [sInstance] is `null`, and if it is, it creates a
         * [RoomDatabase.Builder] using the [SampleDatabase] class as the class annotated with
         * @[Database], "ex" as the file, and builds it to initialize [sInstance]. It then calls the
         * [populateInitialData] method of [sInstance] to insert the dummy data into the database
         * if it is currently empty. Now that [sInstance] is known to exist we return it to the
         * caller.
         *
         * @param context The [Context] that the `Provider` is running in.
         * @return The singleton instance of SampleDatabase.
         */
        @Synchronized
        fun getInstance(context: Context): SampleDatabase {
            if (sInstance == null) {
                sInstance = databaseBuilder(
                    context = context.applicationContext,
                    klass = SampleDatabase::class.java,
                    name = "ex"
                ).build()
                sInstance!!.populateInitialData()
            }
            return sInstance!!
        }

        /**
         * Switches the internal implementation with an empty in-memory database. Used only for
         * testing. We just initialize [sInstance] with the result of building a [RoomDatabase.Builder]
         * for an in memory database.
         *
         * @param context The [Context].
         */
        @VisibleForTesting
        fun switchToInMemory(context: Context) {
            sInstance = inMemoryDatabaseBuilder(context.applicationContext,
                SampleDatabase::class.java).build()
        }
    }
}