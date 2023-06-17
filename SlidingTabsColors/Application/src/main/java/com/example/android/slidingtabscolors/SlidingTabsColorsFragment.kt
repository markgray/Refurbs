/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.slidingtabscolors

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.android.common.view.SlidingTabLayout
import com.example.android.common.view.SlidingTabLayout.TabColorizer

/**
 * A basic sample which shows how to use [com.example.android.common.view.SlidingTabLayout]
 * to display a custom [ViewPager] title strip which gives continuous feedback to the user
 * when scrolling.
 */
class SlidingTabsColorsFragment : Fragment() {
    /**
     * This class represents a tab to be displayed by [ViewPager] and it's associated
     * [SlidingTabLayout].
     */
    internal class SamplePagerItem
    /**
     * Our constructor, we just use our parameters to initialize our fields.
     *
     * @param title Title of the tab
     * @param indicatorColor Indicator color of the tab
     * @param dividerColor Right divider color of the tab
     */(
        /**
         * Title of the tab, set in our constructor, retrieved using our
         * `getTitle` method.
         */
        val title: CharSequence,
        /**
         * Indicator color of the tab, set in our constructor, retrieved using our
         * `getIndicatorColor` method.
         */
        val indicatorColor: Int,
        /**
         * Right divider color of the tab, set in our constructor, retrieved using our
         * `getDividerColor` method.
         */
        val dividerColor: Int) {
        /**
         * Getter for our `CharSequence mTitle` field.
         *
         * @return the title which represents this tab. In this sample this is used directly by
         * [PagerAdapter.getPageTitle]
         */
        /**
         * Getter for our `int mIndicatorColor` field.
         *
         * @return the color to be used for indicator on the [SlidingTabLayout]
         */
        /**
         * Getter for our `int mDividerColor` field.
         *
         * @return the color to be used for right divider on the [SlidingTabLayout]
         */

        /**
         * Factory method to create a new instance of `ContentFragment` to display our fields.
         *
         * @return A new [Fragment] to be displayed by a [ViewPager]
         */
        fun createFragment(): Fragment {
            return ContentFragment.newInstance(title, indicatorColor, dividerColor)
        }
    }

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
     * List of [SamplePagerItem] which represent this sample's tabs.
     */
    private val mTabs: MutableList<SamplePagerItem> = ArrayList()

    /**
     * Called when this `Fragment` is starting. We add four `SamplePagerItem` objects to
     * our field `List<SamplePagerItem> mTabs`:
     *
     *  *
     * Title: R.string.tab_stream ("Stream"), Indicator color: BLUE, Divider color: GRAY
     *
     *  *
     * Title: R.string.tab_messages ("Messages"), Indicator color: RED, Divider color: GRAY
     *
     *  *
     * Title: R.string.tab_photos ("Photos"), Indicator color: YELLOW, Divider color: GRAY
     *
     *  *
     * Title: R.string.tab_notifications ("Notifications"), Indicator color: GREEN, Divider color: GRAY
     *
     *
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Populate our tab list with tabs. Each item contains a title, indicator color and divider
         * color, which are used by {@link SlidingTabLayout}.
         */mTabs.add(SamplePagerItem(
            getString(R.string.tab_stream),  // Title
            Color.BLUE,  // Indicator color
            Color.GRAY // Divider color
        ))
        mTabs.add(SamplePagerItem(
            getString(R.string.tab_messages),  // Title
            Color.RED,  // Indicator color
            Color.GRAY // Divider color
        ))
        mTabs.add(SamplePagerItem(
            getString(R.string.tab_photos),  // Title
            Color.YELLOW,  // Indicator color
            Color.GRAY // Divider color
        ))
        mTabs.add(SamplePagerItem(
            getString(R.string.tab_notifications),  // Title
            Color.GREEN,  // Indicator color
            Color.GRAY // Divider color
        ))
    }

    /**
     * Called to have the fragment instantiate its user interface view. Inflates the [View]
     * which will be displayed by this [Fragment] from the app's resources. We just return the
     * view which our parameter `LayoutInflater inflater` inflates from our layout file
     * R.layout.fragment_sample using our parameter `ViewGroup container` for the LayoutParams
     * without attaching to it.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sample, container, false)
    }

    /**
     * Called immediately after [.onCreateView] has returned,
     * but before any saved state has been restored in to the view. We initialize our field
     * `ViewPager mViewPager` by finding the view in our parameter `View view` with id
     * R.id.viewpager, and set its adapter to a new instance of `SampleFragmentPagerAdapter`
     * constructed using a private FragmentManager for placing and managing Fragments inside of our
     * Fragment. We initialize our field `SlidingTabLayout mSlidingTabLayout` by finding the
     * view in `view` with id R.id.sliding_tabs, and give it the `ViewPager mViewPager`
     * so that it may populate itself. We set an anonymous `TabColorizer` as the custom tab
     * colorizer of `SlidingTabLayout mSlidingTabLayout` whose `getIndicatorColor`, and
     * `getDividerColor` overrides return the indicator color and divider color of the
     * `SamplePagerItem` respectively of the tab in position `position` of our field
     * `List<SamplePagerItem> mTabs`.
     *
     * @param view The View returned by [.onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = view.findViewById(R.id.viewpager)
        mViewPager!!.adapter = SampleFragmentPagerAdapter(childFragmentManager)

        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = view.findViewById(R.id.sliding_tabs)
        mSlidingTabLayout!!.setViewPager(mViewPager)

        // Set a TabColorizer to customize the indicator and divider colors. Here we just retrieve
        // the tab at the position, and return it's set color
        mSlidingTabLayout!!.setCustomTabColorizer(object : TabColorizer {
            /**
             * Returns the color of the indicator for the given `position` position.
             *
             * @param position position we are to provide the color for.
             * @return return the color of the indicator used when `position` is selected.
             */
            override fun getIndicatorColor(position: Int): Int {
                return mTabs[position].indicatorColor
            }

            /**
             * Returns the color of the divider drawn to the right of `position`.
             *
             * @param position position we are to provide the color for.
             * @return return the color of the divider drawn to the right of `position`.
             */
            override fun getDividerColor(position: Int): Int {
                return mTabs[position].dividerColor
            }
        })
    }

    /**
     * The [FragmentPagerAdapter] used to display pages in this sample. The individual pages
     * are instances of [ContentFragment] which just display three lines of text. Each page is
     * created by the relevant [SamplePagerItem] for the requested position.
     *
     *
     * The important section of this class is the [.getPageTitle] method which controls
     * what is displayed in the [SlidingTabLayout].
     */
    internal inner class SampleFragmentPagerAdapter
    /**
     * Our constructor, we just call our super's constructor
     *
     * @param fm private FragmentManager instance for placing and managing Fragments inside of our
     * parent Fragment, created using the `getChildFragmentManager` method
     */
    (fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
        /**
         * Return the [Fragment] to be displayed at position `i`/
         *
         *
         * Here we return the value returned from [SamplePagerItem.createFragment] by the
         * `SamplePagerItem` in position `i` of our field `List<SamplePagerItem> mTabs`
         *
         * @param i position of the item in our data set.
         * @return a `ContentFragment` for the item in question.
         */
        override fun getItem(i: Int): Fragment {
            return mTabs[i].createFragment()
        }

        /**
         * Return the number of views available, we return the size of our dataset
         * `List<SamplePagerItem> mTabs`
         *
         * @return the number of views available.
         */
        override fun getCount(): Int {
            return mTabs.size
        }

        /**
         * Return the title of the item at `position`. This is important as what this method
         * returns is what is displayed in the [SlidingTabLayout].
         *
         *
         * Here we return the value returned from the [SamplePagerItem.title] method of
         * the item at position `position` in our dataset `List<SamplePagerItem> mTabs`.
         *
         * @param position position of the item whose title we need.
         * @return the title of the item in position `position`
         */
        override fun getPageTitle(position: Int): CharSequence {
            return mTabs[position].title
        }
    }
}