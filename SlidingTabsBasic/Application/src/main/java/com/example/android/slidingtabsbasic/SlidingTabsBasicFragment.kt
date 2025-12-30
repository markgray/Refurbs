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

package com.example.android.slidingtabsbasic

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.android.common.logger.Log
import com.example.android.common.view.SlidingTabLayout

/**
 * A basic sample which shows how to use [SlidingTabLayout] to display a custom [ViewPager] title
 * strip which gives continuous feedback to the user when scrolling.
 */
class SlidingTabsBasicFragment : Fragment() {
    /**
     * A custom [ViewPager] title strip which looks much like Tabs present in Android v4.0 and
     * above, but is designed to give continuous feedback to the user when scrolling.
     */
    private var mSlidingTabLayout: SlidingTabLayout? = null

    /**
     * A [ViewPager] which will be used in conjunction with the [SlidingTabLayout] above.
     */
    private var mViewPager: ViewPager? = null

    /**
     * Inflates the [View] which will be displayed by this [Fragment], from the app's
     * resources. We return the [View] inflated by our [LayoutInflater] parameter [inflater]
     * from our layout file `R.layout.fragment_sample` using our [ViewGroup] parameter [container]
     * for the LayoutParams without attaching to it.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here, we do not use.
     * @return Return the [View] for the fragment's UI
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(
            /* resource = */ R.layout.fragment_sample,
            /* root = */ container,
            /* attachToRoot = */ false
        )
    }

    /**
     * This is called after the [onCreateView] method has finished. Here we can pick out the [View]s
     * we need to configure from the content view. We set the [ViewPager]'s adapter to be an instance
     * of [SamplePagerAdapter]. The [SlidingTabLayout] is then given the [ViewPager] so that it can
     * populate itself.
     *
     * @param view View created in [onCreateView]
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = view.findViewById(R.id.viewpager)
        mViewPager!!.adapter = SamplePagerAdapter()

        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = view.findViewById(R.id.sliding_tabs)
        mSlidingTabLayout!!.setViewPager(mViewPager)
    }

    /**
     * The [PagerAdapter] used to display pages in this sample. The individual pages are simple and
     * just display two lines of text. The important section of this class is the [getPageTitle]
     * method which controls what is displayed in the [SlidingTabLayout].
     */
    internal inner class SamplePagerAdapter : PagerAdapter() {
        /**
         * Return the number of views available, we return 10.
         *
         * @return the number of pages to display, we return 10
         */
        override fun getCount(): Int {
            return 10
        }

        /**
         * Determines whether a page View is associated with a specific key object as returned by
         * [instantiateItem]. This method is required for a [PagerAdapter] to function properly.
         * We return `true` if our [Any] parameter [o] points to same object as our [View] parameter
         * [view].
         *
         * @param view Page [View] to check for association with [o]
         * @param o Object to check for association with [view]
         * @return `true` if the value returned from [instantiateItem] is the
         * same object as the [View] added to the [ViewPager].
         */
        override fun isViewFromObject(view: View, o: Any): Boolean {
            return o === view
        }

        /**
         * This method may be called by the [ViewPager] to obtain a title string to describe the
         * specified page. This method may return `null` indicating no title for this page. The
         * default implementation returns `null`. Return the title of the item at [Int] parameter
         * [position]. This is important as what this method returns is what is displayed in the
         * [SlidingTabLayout].
         *
         * Here we construct one using the [Int] parameter [position] value, but for real
         * application the title should refer to the item's contents.
         *
         * @param position The position of the title requested
         * @return A title for the requested page
         */
        override fun getPageTitle(position: Int): CharSequence {
            return "Item " + (position + 1)
        }

        /**
         * Instantiate the [View] which should be displayed at [Int] parameter [position]. Here we
         * inflate a layout from the apps resources and then change the text view to signify the
         * position. We initialize [View] variable `val view` by using the [LayoutInflater] instance
         * that our activity used to inflate its Window to inflate our layout file `R.layout.pager_item`
         * using our [ViewGroup] parameter [container] for the LayoutParams with our attaching to it.
         * We then add `view` to our parameter [container]. We initialize [TextView] variable
         * `val title` by finding the view in `view` with id `R.id.item_title` and set the text of
         * `title` to the string value of our [Int] parameter [position] plus 1, and we then log
         * what we just did. Finally we return `view` to the caller.
         *
         * @param container The containing View in which the page will be shown.
         * @param position The page position to be instantiated.
         * @return Returns an Object representing the new page. This does not
         * need to be a [View], but can be some other container of the page.
         */
        @SuppressLint("SetTextI18n")
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            // Inflate a new layout from our resources
            val view: View = activity!!.layoutInflater.inflate(
                /* resource = */ R.layout.pager_item,
                /* root = */ container,
                /* attachToRoot = */ false
            )
            // Add the newly created View to the ViewPager
            container.addView(view)

            // Retrieve a TextView from the inflated View, and update it's text
            val title: TextView = view.findViewById(R.id.item_title)
            title.text = (position + 1).toString()
            Log.i(LOG_TAG, "instantiateItem() [position: $position]")

            // Return the View
            return view
        }

        /**
         * Remove a page for the given position. The adapter is responsible for removing the view
         * from its container, although it only must ensure this is done by the time it returns from
         * [finishUpdate].
         *
         * Destroy the item from the [ViewPager]. In our case this is simply removing the [Any]
         * parameter [pageToRemove] (cast to [View]) from our [ViewGroup] parameter [container],
         * and logging which [Int] parameter [position] we were asked to remove.
         *
         * @param container The containing View from which the page will be removed.
         * @param position The page position to be removed.
         * @param [pageToRemove] The same object that was returned by [instantiateItem].
         */
        override fun destroyItem(container: ViewGroup, position: Int, pageToRemove: Any) {
            container.removeView(pageToRemove as View)
            Log.i(LOG_TAG, "destroyItem() [position: $position]")
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val LOG_TAG: String = "SlidingTabsBasicFragment"
    }
}
