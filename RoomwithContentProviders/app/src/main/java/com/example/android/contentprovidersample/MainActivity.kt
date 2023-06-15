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

package com.example.android.contentprovidersample

import android.annotation.SuppressLint
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.contentprovidersample.data.Cheese
import com.example.android.contentprovidersample.provider.SampleContentProvider

/**
 * Not very relevant to Room. This just shows data from [SampleContentProvider].
 *
 *
 *
 * Since the data is exposed through the ContentProvider, other apps can read and write the
 * content in a similar manner to this.
 */
class MainActivity : AppCompatActivity() {
    /**
     * The `CheeseAdapter` which supplies data for our `RecyclerView`
     */
    private var mCheeseAdapter: CheeseAdapter? = null

    /**
     * Called when our `AppCompatActivity` is starting. First we call our super's implementation
     * of `onCreate`, then we set our content view to our layout file R.layout.main_activity
     * (it consists of a single `RecyclerView`). We initialize `RecyclerView list` by
     * finding the view with id R.id.list, and set its layout manager to a new instance of
     * `LinearLayoutManager` created to use the context of `list`. We initialize our field
     * `CheeseAdapter mCheeseAdapter` with a new instance and set the adapter of `list` to
     * it. Finally we call the `initLoader` method of the activity's `LoaderManager` to
     * initialize (or reuse) a loader with id LOADER_CHEESES using our `LoaderCallbacks` field
     * `mLoaderCallbacks` as the interface for the the `LoaderManager` to use to report
     * changes in the state of the loader.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(list.context)
        mCheeseAdapter = CheeseAdapter()
        list.adapter = mCheeseAdapter
        LoaderManager.getInstance(this).initLoader(LOADER_CHEESES, null, mLoaderCallbacks)
    }

    /**
     * `LoaderCallbacks` instance we use when initializing our loader.
     */
    private val mLoaderCallbacks: LoaderManager.LoaderCallbacks<Cursor> = object : LoaderManager.LoaderCallbacks<Cursor> {
        /**
         * Instantiate and return a new Loader for the given ID. We switch on our parameter
         * id in order to throw an IllegalArgumentException if it is not LOADER_CHEESES. If
         * it is we create a new instance of `CursorLoader` created using our application
         * context accessing the uri SampleContentProvider.URI_CHEESE (which is the string
         * content://com.example.android.contentprovidersample.provider/cheeses) requesting
         * the column Cheese.COLUMN_NAME ("name"), with a null for the selection, selection
         * args, and sort order.
         *
         * @param id   The ID whose loader is to be created.
         * @param args Any arguments supplied by the caller.
         * @return Return a new Loader instance that is ready to start loading.
         */
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            if (id == LOADER_CHEESES) {
                val uriCheese: Uri = SampleContentProvider.URI_CHEESE
                return CursorLoader(applicationContext,
                    uriCheese, arrayOf(Cheese.COLUMN_NAME),
                    null, null, null)
            }
            throw IllegalArgumentException()
        }

        /**
         * Called when a previously created loader has finished its load. We switch on the
         * parameter `id` just in case, and if it is LOADER_CHEESES we call the
         * `setCheeses` method of our field `CheeseAdapter mCheeseAdapter` with
         * our parameter `Cursor data` so that it will begin to display the new data
         * in its `RecyclerView`.
         *
         * @param loader The Loader that has finished.
         * @param data   The data generated by the Loader.
         */
        override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
            if (loader.id == LOADER_CHEESES) {
                mCheeseAdapter!!.setCheeses(data)
            }
        }

        /**
         * Called when a previously created loader is being reset. We switch on the id of
         * our parameter `loader` in order to ignore all but LOADER_CHEESES, but if
         * it is LOADER_CHEESES we call the `setCheeses` method of our field
         * `CheeseAdapter mCheeseAdapter` with null so that it will set its cursor to
         * null and stop displaying the old data.
         *
         * @param loader The Loader that is being reset.
         */
        override fun onLoaderReset(loader: Loader<Cursor>) {
            if (loader.id == LOADER_CHEESES) {
                mCheeseAdapter!!.setCheeses(null)
            }
        }
    }

    /**
     * The `Adapter` class we use to fill our `RecyclerView` with cheeses.
     */
    private class CheeseAdapter : RecyclerView.Adapter<CheeseAdapter.ViewHolder>() {
        /**
         * `Cursor` we use to read our data from
         */
        private var mCursor: Cursor? = null

        /**
         * Called when RecyclerView needs a new `ViewHolder` of the given type to represent
         * an item. We simply return a new instance of our class `ViewHolder`
         *
         * @param parent   The ViewGroup into which the new View will be added after it is bound to
         * an adapter position.
         * @param viewType The view type of the new View. UNUSED
         * @return A new ViewHolder that holds a View of the given view type.
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent)
        }

        /**
         * Called by RecyclerView to display the data at the specified position. We move our field
         * `Cursor mCursor` to the position `position` and if that succeeds, we call the
         * `setText` method of the `TextView mText` field of our parameter `holder`
         * to set its text to the string value of the column in `mCursor` whose column index
         * is named Cheese.COLUMN_NAME ("name") (throwing IllegalArgumentException if that column
         * does not exist).
         *
         * @param holder   The ViewHolder which should be updated to represent the contents of the
         * item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (mCursor!!.moveToPosition(position)) {
                holder.mText.text = mCursor!!.getString(
                    mCursor!!.getColumnIndexOrThrow(Cheese.COLUMN_NAME))
            }
        }

        /**
         * Returns the total number of items in the data set held by the adapter. If our field
         * `Cursor mCursor` is null we return 0, otherwise we return the numbers of rows
         * in `Cursor mCursor`.
         *
         * @return The total number of items in this adapter.
         */
        override fun getItemCount(): Int {
            return if (mCursor == null) 0 else mCursor!!.count
        }

        /**
         * Setter for our field `Cursor mCursor`. We save our parameter `Cursor cursor`
         * in our field `Cursor mCursor` and call `notifyDataSetChanged` to notify any
         * registered observers that the data set has changed.
         *
         * @param cursor `Cursor` we are to read our data from
         */
        @SuppressLint("NotifyDataSetChanged")
        fun setCheeses(cursor: Cursor?) {
            mCursor = cursor
            notifyDataSetChanged()
        }

        /**
         * The `ViewHolder` subclass we use to display our data
         * Our constructor. First we call our super's constructor with the inflated layout file
         * android.R.layout.simple_list_item_1. Then we initialize our field `TextView mText`
         * by finding the view with id android.R.id.text1 in our super's field `View itemView`
         * (it is set to the view we used when we called our super's constructor (cute))
         *
         * @param parent The ViewGroup into which the new View will be added after it is bound to
         * an adapter position.
         */
        class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(
            android.R.layout.simple_list_item_1, parent, false)) {
            /**
             * the `TextView` we display our data in
             */
            var mText: TextView = itemView.findViewById(android.R.id.text1)

        }
    }

    companion object {
        /**
         * Unique identifier for our loader
         */
        private const val LOADER_CHEESES = 1
    }
}