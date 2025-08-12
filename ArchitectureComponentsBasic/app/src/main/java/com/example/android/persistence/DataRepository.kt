package com.example.android.persistence

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.example.android.persistence.db.AppDatabase
import com.example.android.persistence.db.dao.ProductDao
import com.example.android.persistence.db.entity.CommentEntity
import com.example.android.persistence.db.entity.ProductEntity

/**
 * Singleton Repository handling the work with products and comments. Our private constructor is
 * called only by our [getInstance] method. we create a new instance for our [MediatorLiveData]
 * wrapped [List] of [ProductEntity] field [mObservableProducts]. Then in our `init` block we add
 * as its source the [LiveData] wrapped [List] of [ProductEntity] returned by the method
 * [ProductDao.loadAllProducts] (using the [ProductDao] returned by the [AppDatabase.productDao]
 * method of [mDatabase]), with a lambda listener whose `onChanged` observer will post the [LiveData]
 * wrapped [List] of [ProductEntity] objects received from the `loadAllProducts` method to
 * [mObservableProducts] when it changes value.
 *
 * @param mDatabase the [AppDatabase] instance used for the application
 */
class DataRepository private constructor(
    /**
     * Our [AppDatabase] instance, which is a `RoomDatabase` with two tables "product"
     * and "comments"
     */
    private val mDatabase: AppDatabase
) {
    /**
     * The list of [ProductEntity] objects loaded from the database
     */
    private val mObservableProducts: MediatorLiveData<List<ProductEntity>> = MediatorLiveData()

    init {
        mObservableProducts.addSource(
            mDatabase.productDao().loadAllProducts()
        ) { productEntities: List<ProductEntity> ->
            if (mDatabase.databaseCreated.value != null) {
                mObservableProducts.postValue(productEntities)
            }
        }
    }

    /**
     * Get the list of products from the database and get notified when the data changes. We simply
     * return our [MediatorLiveData] wrapped [List] of [ProductEntity] field [mObservableProducts].
     *
     * @return our [MediatorLiveData] wrapped [List] of [ProductEntity] field [mObservableProducts].
     */
    val products: LiveData<List<ProductEntity>>
        get() = mObservableProducts

    /**
     * Retrieves the [LiveData] wrapped [ProductEntity] whose product ID is given by our
     * [Int] parameter [productId] from the database.
     *
     * @param productId product ID of the [ProductEntity] we are to retrieve
     * @return the [LiveData] wrapped [ProductEntity] requested
     */
    fun loadProduct(productId: Int): LiveData<ProductEntity> {
        return mDatabase.productDao().loadProduct(productId)
    }

    /**
     * Retrieves the [LiveData] wrapped list of [CommentEntity] objects whose product ID
     * is given by our [Int] parameter [productId] from the database.
     *
     * @param productId value of the "productId" column we are interested in
     * @return a [LiveData] wrapped list of all [CommentEntity] whose "productId"
     * column matches our [Int] parameter [productId].
     */
    fun loadComments(productId: Int): LiveData<List<CommentEntity>> {
        return mDatabase.commentDao().loadComments(productId)
    }

    companion object {
        /**
         * The cached instance of [DataRepository] that will be used by the application.
         */
        private var sInstance: DataRepository? = null

        /**
         * Accessor for our singleton instance of [DataRepository]. If our field [sInstance] is
         * `null`, we synchronize on the class of [DataRepository] and if it is still `null` we create a
         * new instance of [DataRepository] using our [AppDatabase] parameter [database] to
         * initialize it. Finally we return [sInstance] to the caller.
         *
         * @param database the [AppDatabase] instance used for the application
         * @return our singleton instance of [DataRepository]
         */
        fun getInstance(database: AppDatabase): DataRepository? {
            if (sInstance == null) {
                synchronized(DataRepository::class.java) {
                    if (sInstance == null) {
                        sInstance = DataRepository(database)
                    }
                }
            }
            return sInstance
        }
    }
}