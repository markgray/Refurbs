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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn") // Returning would just hide the bug

package com.example.android.actionbarcompat.shareactionprovider

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ContentFrameLayout
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.graphics.Insets
import androidx.core.view.ActionProvider
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.example.android.actionbarcompat.shareactionprovider.content.ContentItem

/**
 * This sample shows you how a provide a [ShareActionProvider] with `ActionBarCompat`,
 * backwards compatible to SDK 16.
 *
 * The sample contains a [ViewPager] which displays content of differing types: image and
 * text. When a new item is selected in the ViewPager, the [ShareActionProvider] is updated with
 * a share intent specific to that content.
 *
 * This Activity extends from [AppCompatActivity], which provides all of the function
 * necessary to display a compatible Action Bar on devices running Android v2.1+.
 */
class MainActivity : AppCompatActivity() {
    /**
     * The items to be displayed in the ViewPager
     */
    private val mItems: ArrayList<ContentItem> = sampleContent

    /**
     * Keep reference to the ShareActionProvider from the menu
     */
    private var mShareActionProvider: ShareActionProvider? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file `R.layout.sample_main`.
     *
     * We initialize our [ContentFrameLayout] variable `rootView`
     * to the view with ID `android.R.id.content` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
     * for applying window insets to `rootView`, with the `listener`
     * argument a lambda that accepts the [View] passed the lambda
     * in variable `v` and the [WindowInsetsCompat] passed the lambda
     * in variable `windowInsets`. It initializes its [Insets] variable
     * `systemBars` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument. It then gets the insets for the
     * IME (keyboard) using [WindowInsetsCompat.Type.ime]. It then updates
     * the layout parameters of `v` to be a [ViewGroup.MarginLayoutParams]
     * with the left margin set to `systemBars.left`, the right margin set to
     * `systemBars.right`, the top margin set to `systemBars.top`, and the bottom margin
     * set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED]
     * to the caller (so that the window insets will not keep passing down to
     * descendant views).
     *
     * We find the view with id `R.id.viewpager` to set our [ViewPager] variable `val vp`,
     * and add our [OnPageChangeListener] field [mOnPageChangeListener] as an
     * [OnPageChangeListener]. Finally we set the adapter of `vp` to our [PagerAdapter]
     * field [mPagerAdapter].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Set content view (which contains a CheeseListFragment)
        setContentView(R.layout.sample_main)
        val rootView = window.decorView.findViewById<ContentFrameLayout>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, windowInsets: WindowInsetsCompat ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                topMargin = systemBars.top
                bottomMargin = systemBars.bottom.coerceAtLeast(ime.bottom)
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        // Retrieve the ViewPager from the content view
        val vp: ViewPager = findViewById(R.id.viewpager)

        // Add an OnPageChangeListener so we are notified when a new item is selected
        vp.addOnPageChangeListener(mOnPageChangeListener)

        // Finally set the adapter so the ViewPager can display items
        vp.adapter = mPagerAdapter
    }

    /**
     * Initialize the contents of the Activity's standard options menu. First we fetch a
     * [MenuInflater] for this context and use it to inflate our menu `R.menu.main_menu` into
     * our [Menu] parameter [menu]. We initialize our [MenuItem] variable `val shareItem` by
     * finding the menu item with id `R.id.menu_share` in [menu]. We initialize our
     * [ShareActionProvider] field [mShareActionProvider] by finding the [ActionProvider] defined
     * for `shareItem` using the `support:actionProviderClass` attribute (in our case
     * "android.support.v7.widget.ShareActionProvider"). We locate the [ViewPager] by finding
     * the view with id `R.id.viewpager` in order to get the current item position in order
     * to set our [Int] variable `val currentViewPagerItem` and call our method [setShareIntent]
     * in order to set the share intent for the item to one for the `currentViewPagerItem` item.
     * Finally we return the value returned by our super's implementation of `onCreateOptionsMenu`
     * to the caller.
     *
     * @param menu The options [Menu] in which we place our items.
     * @return You must return `true` for the menu to be displayed, we return what our super's
     * implementation returns.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu resource
        menuInflater.inflate(R.menu.main_menu, menu)

        // Retrieve the share menu item
        val shareItem: MenuItem = menu.findItem(R.id.menu_share)

        // Now get the ShareActionProvider from the item
        mShareActionProvider = MenuItemCompat.getActionProvider(shareItem) as ShareActionProvider?

        // Get the ViewPager's current item position and set its ShareIntent.
        val currentViewPagerItem = (findViewById<View>(R.id.viewpager) as ViewPager).currentItem
        setShareIntent(currentViewPagerItem)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * A [PagerAdapter] which instantiates views based on the [ContentItem]'s content type.
     */
    private val mPagerAdapter: PagerAdapter = object : PagerAdapter() {
        /**
         * [LayoutInflater] we use to inflate views when [instantiateItem] is called
         */
        var mInflater: LayoutInflater? = null

        /**
         * Returns the number of views available. We just return the size of our [ArrayList] of
         * [ContentItem]'s field [mItems]
         *
         * @return the number of [ContentItem] objects in our list [mItems]
         */
        override fun getCount(): Int {
            return mItems.size
        }

        /**
         * Determines whether a page View is associated with a specific key object as returned by
         * [instantiateItem]. This method is required for a [PagerAdapter] to function properly.
         * We just return the result of comparing our [View] parameter [view] and [Any] parameter
         * [o] for referential equality.
         *
         * @param view Page View to check for association with `object`
         * @param o Object to check for association with `view`
         * @return true if `view` is associated with the key object `object`
         */
        override fun isViewFromObject(view: View, o: Any): Boolean {
            return view === o
        }

        /**
         * Remove a page for the given position. The adapter is responsible for removing the view
         * from its container, although it only must ensure this is done by the time it returns
         * from [finishUpdate]. We just call the [ViewGroup.removeView] method of our [ViewGroup]
         * parameter [container] to remove the [View] cast [Any] parameter [oldItem].
         *
         * @param container The containing View from which the page will be removed.
         * @param position The page position to be removed.
         * @param oldItem The same object that was returned by [instantiateItem].
         */
        override fun destroyItem(container: ViewGroup, position: Int, oldItem: Any) {
            // Just remove the view from the ViewPager
            container.removeView(oldItem as View)
        }

        /**
         * Create the page for the given position. We first check to make sure our [LayoutInflater]
         * field [mInflater] is initialized, and if not we initialize it with the [LayoutInflater]
         * for the [Context] of this [MainActivity]. We then set [ContentItem] variable `val item`
         * to the item in position [position] in our list of [ContentItem] field [mItems]. We switch
         * on the value of the [ContentItem.contentType] field of `item`:
         *  * `CONTENT_TYPE_TEXT` - We use [mInflater] to inflate the layout file with resource ID
         *  `R.layout.item_text` into our [TextView] variable `val tv`, set its `text` to the string
         *  resource indicated by the [ContentItem.contentResourceId] field of `item`, and add `tv`
         *  to our [ViewGroup] parameter [container]. Finally we return `tv` to the caller.
         *
         *  * `CONTENT_TYPE_IMAGE` - We use `mInflater` to inflate the layout file with resource ID
         *  `R.layout.item_image` into [ImageView] variable `val iv`, set its image to the bitmap
         *  decoded from the content [Uri] returned from the [ContentItem.contentUri] property of
         *  `item`, and add `iv` to [ViewGroup] parameter [container]. Finally we return `iv` to
         *  the caller.
         *
         * If the [ContentItem.contentType] field or `item` is unrecognised we return an empty [Any]
         * object to the caller (although we probably should throw an exception).
         *
         * @param container The containing [ViewGroup] in which the page will be shown.
         * @param position The page position to be instantiated.
         * @return Returns an [Any] representing the new page. This does not need to be a [View],
         * but can be some other container of the page.
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
     * Sets the share intent of [ShareActionProvider] field [mShareActionProvider] to one which will
     * share the [ContentItem] object at position [position] in our list of [ContentItem] field
     * [mItems]. If [mShareActionProvider] is not `null`, we fetch the [ContentItem] at position
     * [position] in our list [mItems] to set [ContentItem] variable `val item`, then we call the
     * [ContentItem.getShareIntent] method of `item` to create a share intent which we save in our
     * [Intent] variable `val shareIntent`. Finally we call the [ShareActionProvider.setShareIntent]
     * method of [mShareActionProvider] to set its share intent to `shareIntent`.
     *
     * @param position position in our list of `ContentItem` objects we want to share.
     */
    private fun setShareIntent(position: Int) {
        if (mShareActionProvider != null) {
            // Get the currently selected item, and retrieve it's share intent
            val item = mItems[position]
            val shareIntent: Intent = item.getShareIntent(this@MainActivity)

            // Now update the ShareActionProvider with the new share intent
            mShareActionProvider!!.setShareIntent(shareIntent)
        }
    }

    /**
     * A [OnPageChangeListener] used to update the [ShareActionProvider]'s share intent when a new
     * item is selected in the [ViewPager].
     */
    private val mOnPageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         * We ignore.
         *
         * @param position Position index of the first page currently being displayed.
         * Page [position]+1 will be visible if [positionOffset] is nonzero.
         * @param positionOffset Value from [0, 1) indicating the offset from the page at [position].
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            // NO-OP
        }

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete. We just call our method [setShareIntent] to update the
         * [ShareActionProvider]'s share intent when a new [position] is selected in the
         * [ViewPager].
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
