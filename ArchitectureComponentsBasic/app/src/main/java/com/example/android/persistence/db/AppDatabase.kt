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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.persistence.db

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.android.persistence.AppExecutors
import com.example.android.persistence.db.DataGenerator.generateCommentsForProducts
import com.example.android.persistence.db.DataGenerator.generateProducts
import com.example.android.persistence.db.converter.DateConverter
import com.example.android.persistence.db.dao.CommentDao
import com.example.android.persistence.db.dao.ProductDao
import com.example.android.persistence.db.entity.CommentEntity
import com.example.android.persistence.db.entity.ProductEntity
import java.util.Date
import java.util.concurrent.Executor

/**
 * Our [RoomDatabase] class, with a "version" of 1, and two tables containing [ProductEntity]
 * and [CommentEntity] objects in tables "product" and "comments" respectively. We use the two
 * Dao (data access objects) [ProductDao] and [CommentDao] to access the database. We also
 * use the type converter [DateConverter] to convert [Date] objects to and from milliseconds
 * since January 1, 1970, 00:00:00 GMT.
 */
@Database(entities = [ProductEntity::class, CommentEntity::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Provides access to the Dao (data access object) methods defined for the "products" table of
     * the database.
     *
     * @return An instance of [ProductDao] to use to access the "products" table of database
     */
    abstract fun productDao(): ProductDao

    /**
     * Provides access to the Dao (data access object) methods defined for the "comments" table of
     * the database.
     *
     * @return An instance of [CommentDao] to use to access the "comments" table of database
     */
    abstract fun commentDao(): CommentDao

    /**
     * Flag used to tell if the database has been created, set by the method [setDatabaseCreated]
     * and retrieved using the property [databaseCreated].
     */
    private val mIsDatabaseCreated = MutableLiveData<Boolean>()

    /**
     * Check whether the database already exists and expose it via [databaseCreated]. We use our
     * [Context] parameter [context] to retrieve the absolute path on the filesystem where our
     * database [DATABASE_NAME] ("basic-sample-db") should be located and if it exists there we call
     * our method [setDatabaseCreated] to set our [MutableLiveData] wrapped [Boolean] field
     * [mIsDatabaseCreated] to `true`.
     *
     * @param context application [Context] to use to find the absolute path on the filesystem
     * where our database should be located
     */
    private fun updateDatabaseCreated(context: Context) {
        if (context.getDatabasePath(DATABASE_NAME).exists()) {
            setDatabaseCreated()
        }
    }

    /**
     * Called to set our [MutableLiveData] wrapped [Boolean] field [mIsDatabaseCreated] to `true`
     * (which we do by calling its [MutableLiveData.postValue] method with `true`).
     */
    private fun setDatabaseCreated() {
        mIsDatabaseCreated.postValue(true)
    }

    /**
     * Getter for our [MutableLiveData] wrapped [Boolean] field [mIsDatabaseCreated].
     *
     * @return the value of our [MutableLiveData] wrapped [Boolean] field [mIsDatabaseCreated].
     */
    val databaseCreated: LiveData<Boolean>
        get() = mIsDatabaseCreated

    companion object {
        /**
         * Our cached instance of [AppDatabase], created by our [getInstance] method by calling the
         * method [buildDatabase].
         */
        private var sInstance: AppDatabase? = null

        /**
         * Name of the data base file.
         */
        @VisibleForTesting
        val DATABASE_NAME: String = "basic-sample-db"

        /**
         * Returns the [AppDatabase] instance used for the application, creating it if need be. If
         * our [AppDatabase] field [sInstance] is `null`, we synchronize on the class of [AppDatabase], and
         * if it is still `null`, we initialize it with the value returned from our method [buildDatabase]
         * (which will open our [AppDatabase] or create it if it does not already exist). Then we call
         * the [updateDatabaseCreated] to set to `true` our [MutableLiveData] wrapped [Boolean] flag
         * [mIsDatabaseCreated]. Finally we return [sInstance] to our caller.
         *
         * @param context [Context] to use to retrieve the application context (usually the application
         * context to begin with).
         * @param executors Instance of [AppExecutors] to use to access the Global executor pool
         * for the whole application.
         * @return our singleton instance of [AppDatabase].
         */
        fun getInstance(context: Context, executors: AppExecutors): AppDatabase? {
            if (sInstance == null) {
                synchronized(AppDatabase::class.java) {
                    if (sInstance == null) {
                        sInstance = buildDatabase(context.applicationContext, executors)
                        sInstance!!.updateDatabaseCreated(context.applicationContext)
                    }
                }
            }
            return sInstance
        }

        /**
         * Build the database. Calling the [RoomDatabase.Builder.build] method of the [RoomDatabase.Builder]
         * returned by [databaseBuilder] only sets up the database configuration and creates a new
         * instance of the database. The SQLite database is only created when it's accessed for the
         * first time. We create a [RoomDatabase.Builder] designed to build a database with the name
         * DATABASE_NAME ("basic-sample-db") using [AppDatabase] for the class which is annotated
         * with `Database` and extends [RoomDatabase]. We then add an anonymous class as a
         * [RoomDatabase.Callback] to it whose `onOpen` override simply logs the fact that it has
         * been called, and whose `onCreate` override uses the [Executor] of [AppExecutors.diskIO]
         * to run a lambda on its single threaded thread pool which generates data for our database
         * and inserts it (`onCreate` is only called the first time). Finally we build the
         * [RoomDatabase.Builder] and return the [AppDatabase] it builds to the caller.
         *
         * @param appContext application [Context] for the database.
         * @param executors  our instance of [AppExecutors] to use to access the thread pool
         * @return the [AppDatabase] our application will use to access the database.
         */
        private fun buildDatabase(
            appContext: Context,
            executors: AppExecutors
        ): AppDatabase {
            return databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(object : Callback() {
                    /**
                     * Called when the database we are the [RoomDatabase.Callback] for is opened.
                     * It is called after `onCreate` if the database did not exist already. We just
                     * log the fact that we were called.
                     *
                     * @param db the [SupportSQLiteDatabase] database that has been opened
                     */
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        Log.i("AppDataBase", "Database is now open: $db")
                    }

                    /**
                     * Called only when the database is first created. First we call our super's
                     * implementation of `onCreate` (it does nothing, but what the heck). Then we
                     * post a [Runnable] lambda to the [AppExecutors.diskIO] executor's queue which
                     * calls our method [addDelay] to simulate a delay, then fetches our singleton
                     * instance to [AppDatabase] variable `val database`, calls the method
                     * [DataGenerator.generateProducts] to fill our [List] of [ProductEntity] variable
                     * `val products` with fake [ProductEntity] data, calls the method
                     * [DataGenerator.generateCommentsForProducts] with `products` to fill our
                     * [List] of [CommentEntity] variable `val comments` with fake [CommentEntity]
                     * data, calls [insertData] with `database`, `products`, and `comments` to insert
                     * both `products` and `comments` into `database`, and finally calls the
                     * [AppDatabase.setDatabaseCreated] method of `database` to flag that the
                     * database has been created.
                     *
                     * @param db the [SupportSQLiteDatabase] database that has been created.
                     */
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        executors.diskIO().execute {

                            // Add a delay to simulate a long-running operation
                            addDelay()
                            // Generate the data for pre-population
                            val database: AppDatabase? = getInstance(appContext, executors)
                            val products: List<ProductEntity> = generateProducts()
                            val comments: List<CommentEntity> = generateCommentsForProducts(products)
                            insertData(database = database, products = products, comments = comments)
                            // notify that the database was created and it's ready to be used
                            database!!.setDatabaseCreated()
                        }
                    }
                }).build()
        }

        /**
         * Inserts the [List] of [ProductEntity] objects in its parameter [products] and the [List]
         * of [CommentEntity] objects in in its parameter [comments] into its [AppDatabase] parmeter
         * [database]. We use the [AppDatabase.runInTransaction] method of [database] to run a
         * [Runnable] lambda in a transaction. The [Runnable] uses the [ProductDao.insertAll] method
         * of the [ProductDao] Dao we retrieve from the [AppDatabase.productDao] method of [database]
         * to insert all of the [ProductEntity] objects in [products] into the "products" table of
         * [database], and the [CommentDao.insertAll] method of the [CommentDao] Dao we retrieve
         * from the [AppDatabase.commentDao] method of [database] to insert all of the [CommentEntity]
         * objects in [comments] into the "comments" table of [database].
         *
         * @param database our instance of [AppDatabase]
         * @param products the [List] of [ProductEntity] objects to insert in the "products" table
         * of the database.
         * @param comments the [List] of [CommentEntity] objects to insert in the "comments" table
         * of the database.
         */
        private fun insertData(
            database: AppDatabase?,
            products: List<ProductEntity>,
            comments: List<CommentEntity>
        ) {
            database!!.runInTransaction {
                database.productDao().insertAll(products)
                database.commentDao().insertAll(comments)
            }
        }

        /**
         * Sleeps for 4000ms to simulate a slow process.
         */
        private fun addDelay() {
            try {
                Thread.sleep(4000)
            } catch (_: InterruptedException) {
            }
        }
    }
}