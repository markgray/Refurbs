/*
 * Copyright (C) 2009 The Android Open Source Project
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
@file:Suppress("UNCHECKED_CAST", "KDocUnresolvedReference", "unused")

package com.example.android.basicmultitouch

/**
 * Helper class for creating pools of objects. An example use looks like this:
 * <pre>
 * public class MyPooledClass {
 *
 * private static final SynchronizedPool<MyPooledClass> sPool =
 * new SynchronizedPool<MyPooledClass>(10);
 *
 * public static MyPooledClass obtain() {
 * MyPooledClass instance = sPool.acquire();
 * return (instance != null) ? instance : new MyPooledClass();
 * }
 *
 * public void recycle() {
 * // Clear state if needed.
 * sPool.release(this);
 * }
 *
 * . . .
 * }
</MyPooledClass></MyPooledClass></pre> *
 */
class Pools
/**
 * Hidden constructor
 */
private constructor() {
    /**
     * Interface for managing a pool of objects.
     *
     * @param <T> The pooled type.
    </T> */
    interface Pool<T> {
        /**
         * @return An instance from the pool if such, null otherwise.
         */
        fun acquire(): T

        /**
         * Release an instance to the pool.
         *
         * @param instance The instance to release.
         * @return Whether the instance was put in the pool.
         *
         * @throws IllegalStateException If the instance is already in the pool.
         */
        fun release(instance: T): Boolean
    }

    /**
     * Simple (non-synchronized) pool of objects.
     *
     * @param <T> The pooled type.
    </T> */
    open class SimplePool<T>(maxPoolSize: Int) : Pool<T?> {
        /**
         * Our pool of objects, it is allocated in our constructor.
         */
        private val mPool: Array<Any?>

        /**
         * Current number of entries in our `Object[] mPool` pool
         */
        private var mPoolSize = 0

        /**
         * Creates a new instance. If our parameter `maxPoolSize` is less than or equal to zero
         * we through IllegalArgumentException. Otherwise we allocate `maxPoolSize` objects for
         * our field `Object[] mPool`.
         *
         * param maxPoolSize The max pool size.
         *
         * throws IllegalArgumentException If the max pool size is less than zero.
         */
        init {
            require(maxPoolSize > 0) { "The max pool size must be > 0" }
            mPool = arrayOfNulls(maxPoolSize)
        }

        /**
         * Tries to return a `T` object removed from our pool `Object[] mPool` if there are
         * any there. If `mPoolSize` is greater than 0, we set `int lastPooledIndex` to
         * `mPoolSize` minus 1, initialize `T instance` with the object at index `lastPooledIndex`
         * in our `Object[] mPool` pool, set that entry to null, decrement `mPoolSize` and
         * return `instance`. If `mPoolSize` is not greater than 0 we return null.
         *
         * @return a `T` object if there were any in our pool `Object[] mPool` or null if
         * there were none.
         */
        override fun acquire(): T? {
            if (mPoolSize > 0) {
                val lastPooledIndex = mPoolSize - 1
                val instance = mPool[lastPooledIndex] as T?
                mPool[lastPooledIndex] = null
                mPoolSize--
                return instance
            }
            return null
        }

        /**
         * Returns a `T` object to the pool (if there is room). If our method `isInPool`
         * finds our parameter `T instance` already in our pool we throw an IllegalStateException.
         * If our field `mPoolSize` is less than the length of our field `Object[] mPool`
         * (there is still room) we store `instance` in `mPool[mPoolSize]`, increment
         * `mPoolSize` and return true to the caller. Otherwise we return false.
         *
         * @param instance The instance to release.
         * @return true if we were able to add it to our pool, false if the pool is full.
         *
         * @throws IllegalStateException If the instance is already in the pool.
         */
        override fun release(instance: T?): Boolean {
            check(!isInPool(instance)) { "Already in the pool!" }
            if (mPoolSize < mPool.size) {
                mPool[mPoolSize] = instance
                mPoolSize++
                return true
            }
            return false
        }

        /**
         * Looks for our parameter `T instance` in our field `Object[] mPool`. We loop over
         * `int i` for the `mPoolSize` objects in `Object[] mPool` and if our parameter
         * `instance` is equal to `mPool[i]` we return true. If none of the objects in
         * `mPool` is equal to `instance` we return false.
         *
         * @param instance `T` instance to search for in our field `Object[] mPool`
         * @return true if our parameter `T instance` is in our field `Object[] mPool`,
         * false if it is not.
         */
        private fun isInPool(instance: T?): Boolean {
            for (i in 0 until mPoolSize) {
                if (mPool[i] === instance) {
                    return true
                }
            }
            return false
        }
    }

    /**
     * Synchronized) pool of objects.
     *
     * @param <T> The pooled type.
    </T> */
    class SynchronizedPool<T>
    /**
     * Creates a new instance. We just call our super's constructor.
     *
     * @param maxPoolSize The max pool size.
     *
     * @throws IllegalArgumentException If the max pool size is less than zero.
     */
    (maxPoolSize: Int) : SimplePool<T>(maxPoolSize) {
        /**
         * Lock we use to synchronise access to our pool
         */
        private val mLock = Any()

        /**
         * Synchronized on `mLock` we return the value returned by our super's implementation
         * of `acquire`.
         *
         * @return `T` object from our pool if any are available
         */
        override fun acquire(): T? {
            synchronized(mLock) { return super.acquire() }
        }

        /**
         * Synchronized on `mLock` we return the value returned by our super's implementation
         * of `acquire`.
         *
         * @param instance `T` object to return to our pool if there is room.
         * @return true if we were able to add it to our pool, false if the pool is full.
         */
        override fun release(instance: T?): Boolean {
            synchronized(mLock) { return super.release(instance) }
        }
    }
}