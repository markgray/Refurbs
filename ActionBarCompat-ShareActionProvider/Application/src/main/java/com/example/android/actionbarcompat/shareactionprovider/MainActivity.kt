/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.actionbarcompat.shareactionprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.view.MenuItemCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.example.android.actionbarcompat.shareactionprovider.content.ContentItem

/**
 * This sample shows you how a provide a [ShareActionProvider] with ActionBarCompat,
 * backwards compatible to API v7.
 *
 *
 * The sample contains a [ViewPager] which displays content of differing types: image and
 * text. When a new item is selected in the ViewPager, the ShareActionProvider is updated with
 * a share intent specific to that content.
 *
 *
 * This Activity extends from [AppCompatActivity], which provides all of the function
 * necessary to display a compatible Action Bar on devices running Android v2.1+.
 */
class MainActivity : AppCompatActivity() {
    /**
     * The items to be displayed in the ViewPager
     */
    private val mItems = sampleContent

    /**
     * Keep reference to the ShareActionProvider from the menu
     */
    private var mShareActionProvider: ShareActionProvider? = null

    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file R.layout.sample_main. We
     * find the view with id R.id.viewpager to set `ViewPager vp`, and add our field
     * `OnPageChangeListener mOnPageChangeListener` as an `OnPageChangeListener`.
     * Finally we set the adapter of `vp` to our field `PagerAdapter mPagerAdapter`.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set content view (which contains a CheeseListFragment)
        setContentView(R.layout.sample_main)

        // Retrieve the ViewPager from the content view
        val vp = findViewById<ViewPager>(R.id.viewpager)

        // Add an OnPageChangeListener so we are notified when a new item is selected
        vp.addOnPageChangeListener(mOnPageChangeListener)

        // Finally set the adapter so the ViewPager can display items
        vp.adapter = mPagerAdapter
    }

    /**
     * Initialize the contents of the Activity's standard options menu. First we fetch a
     * `MenuInflater` for this context and use it to inflate our menu R.menu.main_menu into
     * our parameter `Menu menu`. We initialize `MenuItem shareItem` by finding the menu
     * item with id R.id.menu_share in `menu`. We initialize `ShareActionProvider mShareActionProvider`
     * by finding the `ActionProvider` defined for `shareItem` using the support:actionProviderClass
     * attribute ("android.support.v7.widget.ShareActionProvider" in our case). We locate the ViewPager
     * by finding the view with id R.id.viewpager in order to get the current item position in order
     * to set `int currentViewPagerItem` and call our method `setShareIntent` in order to
     * set the share intent for the item to one for the `currentViewPagerItem` item. Finally we
     * return the value returned by our super's implementation of `onCreateOptionsMenu` to the
     * caller.
     *
     * @param menu The options menu in which we place our items.
     * @return You must return true for the menu to be displayed, we return what calling our super's
     * implementation returns.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu resource
        menuInflater.inflate(R.menu.main_menu, menu)

        // Retrieve the share menu item
        val shareItem = menu.findItem(R.id.menu_share)

        // Now get the ShareActionProvider from the item
        mShareActionProvider = MenuItemCompat.getActionProvider(shareItem) as ShareActionProvider?

        // Get the ViewPager's current item position and set its ShareIntent.
        val currentViewPagerItem = (findViewById<View>(R.id.viewpager) as ViewPager).currentItem
        setShareIntent(currentViewPagerItem)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * A PagerAdapter which instantiates views based on the ContentItem's content type.
     */
    private val mPagerAdapter: PagerAdapter = object : PagerAdapter() {
        /**
         * `LayoutInflater` we use to inflate views when `instantiateItem` is called
         */
        var mInflater: LayoutInflater? = null

        /**
         * Returns the number of views available. We just return the size of our list
         * `ArrayList<ContentItem> mItems`
         *
         * @return the number of `ContentItem` objects in our list `mItems`
         */
        override fun getCount(): Int {
            return mItems.size
        }

        /**
         * Determines whether a page View is associated with a specific key object
         * as returned by [.instantiateItem]. This method is
         * required for a PagerAdapter to function properly. We just return the result
         * of comparing our parameters `View view` and `View view` for
         * equality.
         *
         * @param view Page View to check for association with `object`
         * @param o Object to check for association with `view`
         * @return true if `view` is associated with the key object `object`
         */
        override fun isViewFromObject(view: View, o: Any): Boolean {
            return view === o
        }

        /**
         * Remove a page for the given position.  The adapter is responsible
         * for removing the view from its container, although it only must ensure
         * this is done by the time it returns from [.finishUpdate].
         * We just call the `removeView` method of our parameter `ViewGroup container`
         * to remove the `View` cast `Object object`.
         *
         * @param container The containing View from which the page will be removed.
         * @param position The page position to be removed.
         * @param object The same object that was returned by
         * [.instantiateItem].
         */
        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            // Just remove the view from the ViewPager
            container.removeView(`object` as View)
        }

        /**
         * Create the page for the given position. We first check to make sure our field
         * `LayoutInflater mInflater` is initialized, and if not we initialize it with
         * the `LayoutInflater` for the context of this `MainActivity`. We then
         * set `ContentItem item` to the item in position `position` in our list
         * `mItems`. We switch on the value of the `contentType` field of
         * `ContentItem`:
         *
         *  *
         * CONTENT_TYPE_TEXT - We use `mInflater` to inflate the layout file R.layout.item_text
         * into `TextView tv`, set its text to the string resource indicated by the field
         * `contentResourceId`, and add `tv` to `ViewGroup container`. Finally
         * we return `tv` to the caller.
         *
         *  *
         * CONTENT_TYPE_IMAGE - We use `mInflater` to inflate the layout file R.layout.item_image
         * into `ImageView iv`, set its image to the bitmap decoded from the content URI returned
         * from the `getContentUri` method of `item`, and add `iv` to `ViewGroup container`.
         * Finally we return `iv` to the caller.
         *
         *
         * If the `contentType` field is unrecognised we return an empty `Object` to the
         * caller (although we probably should throw an exception).
         *
         * @param container The containing View in which the page will be shown.
         * @param position The page position to be instantiated.
         * @return Returns an Object representing the new page.  This does not
         * need to be a View, but can be some other container of the page.
         */
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            // Ensure that the LayoutInflater is instantiated
            if (mInflater == null) {
                mInflater = LayoutInflater.from(this@MainActivity)
            }

            // Get the item for the requested position
            val item = mItems[position]
            when (item.contentType) {
                ContentItem.CONTENT_TYPE_TEXT -> {

                    // Inflate item layout for text
                    val tv = mInflater!!.inflate(R.layout.item_text, container, false) as TextView

                    // Set text content using it's resource id
                    tv.setText(item.contentResourceId)

                    // Add the view to the ViewPager
                    container.addView(tv)
                    return tv
                }

                ContentItem.CONTENT_TYPE_IMAGE -> {

                    // Inflate item layout for images
                    val iv = mInflater!!.inflate(R.layout.item_image, container, false) as ImageView

                    // Load the image from it's content URI
                    iv.setImageURI(item.contentUri)

                    // Add the view to the ViewPager
                    container.addView(iv)
                    return iv
                }
            }
            return Any()
        }
    }

    /**
     * Sets the share intent of `ShareActionProvider mShareActionProvider` to one which will
     * share the `ContentItem` object at position `position` in our list `mItems`.
     * If `mShareActionProvider` is not null, we fetch the `ContentItem` at position
     * `position` in our list `mItems` to set `ContentItem item`, then we call the
     * `getShareIntent` method of `item` to create a share intent which we save in
     * `Intent shareIntent`. Finally we call the `setShareIntent` method of
     * `mShareActionProvider` to set its share intent to `shareIntent`.
     *
     * @param position position in our list of `ContentItem` objects we want to share.
     */
    private fun setShareIntent(position: Int) {
        // BEGIN_INCLUDE(update_sap)
        if (mShareActionProvider != null) {
            // Get the currently selected item, and retrieve it's share intent
            val item = mItems[position]
            val shareIntent = item.getShareIntent(this@MainActivity)

            // Now update the ShareActionProvider with the new share intent
            mShareActionProvider!!.setShareIntent(shareIntent)
        }
        // END_INCLUDE(update_sap)
    }

    /**
     * A OnPageChangeListener used to update the ShareActionProvider's share intent when a new item
     * is selected in the ViewPager.
     */
    private val mOnPageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         * We ignore.
         *
         * @param position Position index of the first page currently being displayed.
         * Page position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            // NO-OP
        }

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete. We just call our method `setShareIntent` to update the
         * ShareActionProvider's share intent when a new `position` is selected in the
         * ViewPager.
         *
         * @param position Position index of the new selected page.
         */
        override fun onPageSelected(position: Int) {
            setShareIntent(position)
        }

        /**
         * Called when the scroll state changes. Useful for discovering when the user
         * begins dragging, when the pager is automatically settling to the current page,
         * or when it is fully stopped/idle. We ignore.
         *
         * @param state The new scroll state.
         * @see ViewPager.SCROLL_STATE_IDLE
         *
         * @see ViewPager.SCROLL_STATE_DRAGGING
         *
         * @see ViewPager.SCROLL_STATE_SETTLING
         */
        override fun onPageScrollStateChanged(state: Int) {
            // NO-OP
        }
    }

    companion object {
        /**
         * Fills an `ArrayList` of `ContentItem` objects with sample data, and returns it
         * to the caller.
         *
         * @return An ArrayList of ContentItem's to be displayed in this sample
         */
        val sampleContent: ArrayList<ContentItem>
            get() {
                val items = ArrayList<ContentItem>()
                items.add(ContentItem(ContentItem.CONTENT_TYPE_IMAGE, "photo_1.jpg"))
                items.add(ContentItem(ContentItem.CONTENT_TYPE_TEXT, R.string.quote_1))
                items.add(ContentItem(ContentItem.CONTENT_TYPE_TEXT, R.string.quote_2))
                items.add(ContentItem(ContentItem.CONTENT_TYPE_IMAGE, "photo_2.jpg"))
                items.add(ContentItem(ContentItem.CONTENT_TYPE_TEXT, R.string.quote_3))
                items.add(ContentItem(ContentItem.CONTENT_TYPE_IMAGE, "photo_3.jpg"))
                return items
            }
    }
}