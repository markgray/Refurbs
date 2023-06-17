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
package com.example.android.slidingtabscolors

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager

/**
 * Simple Fragment used to display some meaningful content for each page in the sample's
 * [ViewPager].
 */
class ContentFragment : Fragment() {
    /**
     * Called to have the fragment instantiate its user interface view. We return the view that our
     * parameter `LayoutInflater inflater` inflates from our layout file R.layout.pager_item,
     * using our parameter `ViewGroup container` for the LayoutParams without attaching to it.
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
        return inflater.inflate(R.layout.pager_item, container, false)
    }

    /**
     * Called immediately after [.onCreateView] has returned,
     * but before any saved state has been restored in to the view. First we call our super's implementation
     * of `onViewCreated`. Then we initialize `Bundle args` with the arguments supplied when the
     * fragment was instantiated. If `args` is not null we then:
     * <il>
     *  *
     * Initialize `TextView title` by finding the view in our parameter `View view`
     * with ID R.id.item_title, and set its text to the string formed by concatenating the string
     * "Title: " to the string stored in `args` under the key KEY_TITLE/
     *
     *  *
     * Initialize `int indicatorColor` to the int stored in `args` under the key
     * KEY_INDICATOR_COLOR, initialize `TextView indicatorColorView` by finding the view
     * in `View view` with id R.id.item_indicator_color then set its text to the string formed
     * by concatenating the string "Indicator: #" to the hexString of `indicatorColor` and
     * set its text color to `indicatorColor`.
     *
     *  *
     * Initialize `int dividerColor` to the int stored in `args` under the key
     * KEY_DIVIDER_COLOR, initialize `TextView dividerColorView` by finding the view
     * in `View view` with id R.id.item_divider_color then set its text to the string formed
     * by concatenating the string "Divider: #" to the hexString of `dividerColor` and
     * set its text color to `dividerColor`.
     *
    </il> *
     *
     * @param view The View returned by [.onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments
        if (args != null) {
            val title = view.findViewById<TextView>(R.id.item_title)
            title.text = "Title: " + args.getCharSequence(KEY_TITLE)
            val indicatorColor = args.getInt(KEY_INDICATOR_COLOR)
            val indicatorColorView = view.findViewById<TextView>(R.id.item_indicator_color)
            indicatorColorView.text = "Indicator: #" + Integer.toHexString(indicatorColor)
            indicatorColorView.setTextColor(indicatorColor)
            val dividerColor = args.getInt(KEY_DIVIDER_COLOR)
            val dividerColorView = view.findViewById<TextView>(R.id.item_divider_color)
            dividerColorView.text = "Divider: #" + Integer.toHexString(dividerColor)
            dividerColorView.setTextColor(dividerColor)
        }
    }

    companion object {
        /**
         * Key that our fragments title is stored under in our argument bundle.
         */
        private const val KEY_TITLE = "title"

        /**
         * Key that our fragments indicator color is stored under in our argument bundle.
         */
        private const val KEY_INDICATOR_COLOR = "indicator_color"

        /**
         * Key that our fragments divider color is stored under in our argument bundle.
         */
        private const val KEY_DIVIDER_COLOR = "divider_color"

        /**
         * Factory method used to produce a new instance of `ContentFragment`. We initialize our
         * variable `Bundle bundle` with a new instance, add our parameter `CharSequence title`
         * to it under the key KEY_TITLE, add our parameter `int indicatorColor` to it under the key
         * KEY_INDICATOR_COLOR, and add our parameter `int dividerColor` to it under the key
         * KEY_DIVIDER_COLOR. We then initialize `ContentFragment fragment` with a new instance,
         * set `bundle` as its arguments bundle and return the fragment to the caller.
         *
         * @param title          Title to display in the TextView with ID R.id.item_title
         * @param indicatorColor Indicator color to display as an HexString in the TextView with id
         * R.id.item_indicator_color, and to use as its color
         * @param dividerColor   Divider color to display as an HexString in the TextView with id
         * R.id.item_divider_color, and to use as its color
         * @return a new instance of [ContentFragment], adding the parameters into a bundle and
         * setting them as arguments.
         */
        fun newInstance(title: CharSequence?, indicatorColor: Int, dividerColor: Int): ContentFragment {
            val bundle = Bundle()
            bundle.putCharSequence(KEY_TITLE, title)
            bundle.putInt(KEY_INDICATOR_COLOR, indicatorColor)
            bundle.putInt(KEY_DIVIDER_COLOR, dividerColor)
            val fragment = ContentFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}