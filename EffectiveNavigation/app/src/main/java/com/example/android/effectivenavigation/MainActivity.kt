/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION", "ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.effectivenavigation

import android.app.ActionBar
import android.app.ActionBar.Tab
import android.app.ActionBar.TabListener
import android.app.FragmentTransaction
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener

/**
 * TODO: Add kdoc
 */
class MainActivity : FragmentActivity(), TabListener {
    /**
     * The [AppSectionsPagerAdapter] that will provide fragments for each of the
     * three primary sections of the app. We use a [FragmentPagerAdapter]
     * derivative, which will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a [FragmentStatePagerAdapter].
     */
    var mAppSectionsPagerAdapter: AppSectionsPagerAdapter? = null

    /**
     * The [ViewPager] that will display the three primary sections of the app, one at a
     * time.
     */
    var mViewPager: ViewPager? = null

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file R.layout.activity_main. Then we set our field
     * `AppSectionsPagerAdapter mAppSectionsPagerAdapter` to a new instance constructed to use
     * an instance of the `FragmentManager` for interacting with fragments associated with this
     * activity. We initialize our variable `ActionBar actionBar` with a reference to this
     * activity's `ActionBar`, use it to disable the Home/Up button (there is no hierarchical
     * parent), and set its navigation mode NAVIGATION_MODE_TABS. We initialize our field
     * `ViewPager mViewPager` by finding the view with id R.id.pager (our entire layout file
     * is that androidx.viewpager.widget.ViewPager), set its `PagerAdapter` to our field
     * `mAppSectionsPagerAdapter` (it will supply views for the pager as needed) and add an
     * anonymous `SimpleOnPageChangeListener` whose `onPageSelected` override will call
     * the `setSelectedNavigationItem` method of `actionBar` to set its selected navigation
     * item to the position index of the new selected page given in the `onPageSelected` parameter
     * `int position`. Finally we loop over `int i` for all the views available in the
     * adapter `mAppSectionsPagerAdapter` (3 is returned by its `getCount` method) adding
     * a new tab to `actionBar` whose text is given by the `getPageTitle` method of
     * `mAppSectionsPagerAdapter` when it is passed `i`, and whose `TabListener` is
     * set to 'this'.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mAppSectionsPagerAdapter = AppSectionsPagerAdapter(supportFragmentManager)

        // Set up the action bar.
        val actionBar = actionBar

        // Specify that the Home/Up button should not be enabled, since there is no hierarchical
        // parent.
        actionBar!!.setHomeButtonEnabled(false)

        // Specify that we will be displaying tabs in the action bar.
        actionBar.navigationMode = ActionBar.NAVIGATION_MODE_TABS

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = findViewById(R.id.pager)
        mViewPager!!.adapter = mAppSectionsPagerAdapter
        mViewPager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            /**
             * This method will be invoked when a new page becomes selected. Animation is not
             * necessarily complete. We just call the `setSelectedNavigationItem` method of
             * `actionBar` to set its selected navigation item to our parameter `position`.
             *
             * @param position Position index of the new selected page.
             */
            override fun onPageSelected(position: Int) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position)
            }
        })

        // For each of the sections in the app, add a tab to the action bar.
        for (i in 0 until mAppSectionsPagerAdapter!!.count) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                actionBar.newTab()
                    .setText(mAppSectionsPagerAdapter!!.getPageTitle(i))
                    .setTabListener(this))
        }
    }

    /**
     * Called when a tab exits the selected state. We ignore.
     *
     * @param tab                 The tab that was unselected
     * @param fragmentTransaction A [FragmentTransaction] for queuing fragment operations to
     * execute during a tab switch. This tab's unselect and the newly
     * selected tab's select will be executed in a single transaction.
     * This FragmentTransaction does not support being added to the back stack.
     */
    @Deprecated("Deprecated in Java")
    override fun onTabUnselected(tab: Tab, fragmentTransaction: FragmentTransaction) {}

    /**
     * Called when a tab enters the selected state. We just call the `setCurrentItem` method of
     * `ViewPager mViewPager` to set the currently selected page to the current position of our
     * parameter `ActionBar.Tab tab` in the action bar.
     *
     * @param tab                 The tab that was selected
     * @param fragmentTransaction A [FragmentTransaction] for queuing fragment operations to
     * execute during a tab switch. The previous tab's unselect and this
     * tab's select will be executed in a single transaction. This
     * FragmentTransaction does not support being added to the back stack.
     */
    @Deprecated("Deprecated in Java", ReplaceWith("mViewPager!!.currentItem = tab.position"))
    override fun onTabSelected(tab: Tab, fragmentTransaction: FragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager!!.currentItem = tab.position
    }

    /**
     * Called when a tab that is already selected is chosen again by the user. We ignore.
     *
     * @param tab                 The tab that was reselected.
     * @param fragmentTransaction A [FragmentTransaction] for queuing fragment operations to
     * execute once this method returns. This FragmentTransaction does not
     * support being added to the back stack.
     */
    @Deprecated("Deprecated in Java")
    override fun onTabReselected(tab: Tab, fragmentTransaction: FragmentTransaction) {}

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    class AppSectionsPagerAdapter
    /**
     * Our constructor. We just call our super's constructor.
     *
     * @param fm An an instance of the `FragmentManager` for interacting with fragments
     * associated with this activity
     */
    (fm: FragmentManager?) : FragmentPagerAdapter(fm) {
        /**
         * Returns the Fragment associated with a specified position. We switch on the value of our
         * parameter `int i`:
         *
         *  *
         * 0: We return a new instance of `LaunchpadSectionFragment`.
         *
         *  *
         * default: We initialize `Fragment fragment` with a new instance, and initialize
         * `Bundle args` with a new instance. We store the value `i` plus 1 in
         * `args` under the key ARG_SECTION_NUMBER, and set the arguments of `fragment`
         * to `args`. Finally we return `fragment` to the caller.
         *
         *
         *
         * @param i the position in our adapter of the Fragment that is needed
         * @return  Return the Fragment associated with a specified position.
         */
        override fun getItem(i: Int): Fragment {
            return when (i) {
                0 ->                     // The first section of the app is the most interesting -- it offers
                    // a launchpad into the other demonstrations in this example application.
                    LaunchpadSectionFragment()

                else -> {
                    // The other sections of the app are dummy placeholders.
                    val fragment: Fragment = DummySectionFragment()
                    val args = Bundle()
                    args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1)
                    fragment.arguments = args
                    fragment
                }
            }
        }

        /**
         * Returns the number of views available, in our case 3.
         *
         * @return the number of views available.
         */
        override fun getCount(): Int {
            return 3
        }

        /**
         * This method may be called by the ViewPager to obtain a title string to describe the
         * specified page. We return the string formed by concatenating the string "Section " to
         * the string value of our parameter `position` plus 1.
         *
         * @param position The position of the title requested
         * @return A title for the requested page
         */
        override fun getPageTitle(position: Int): CharSequence {
            return "Section " + (position + 1)
        }
    }

    /**
     * A fragment that launches other parts of the demo application.
     */
    class LaunchpadSectionFragment : Fragment() {
        /**
         * Called to have the fragment instantiate its user interface view. We initialize `View rootView`
         * by using our parameter `LayoutInflater inflater` to inflate our layout file
         * R.layout.fragment_section_launchpad using our parameter `ViewGroup container` for its
         * layout params without attaching to it. We find the view in `rootView` with the id
         * R.id.demo_collection_button and set its `OnClickListener` to an anonymous class whose
         * `onClick` override creates an `Intent intent` intended to launch the activity
         * `CollectionDemoActivity` and starts it running. Then we find the view in `rootView`
         * with the id R.id.demo_external_activity and set its `OnClickListener` to an anonymous
         * class whose `onClick` override creates an `Intent externalActivityIntent` whose
         * action is ACTION_PICK, sets its mimetype to "image/ *", adds the flag FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
         * to it (ensures that relaunching our application from the device home screen does not return
         * to the external activity) and then starts it running. Finally we return `rootView` to
         * the caller.
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
            val rootView = inflater.inflate(R.layout.fragment_section_launchpad, container, false)

            // Demonstration of a collection-browsing activity.
            rootView.findViewById<View>(R.id.demo_collection_button)
                .setOnClickListener {
                    val intent = Intent(activity, CollectionDemoActivity::class.java)
                    startActivity(intent)
                }

            // Demonstration of navigating to external activities.
            rootView.findViewById<View>(R.id.demo_external_activity)
                .setOnClickListener { // Create an intent that asks the user to pick a photo, but using
                    // FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET, ensures that relaunching
                    // the application from the device home screen does not return
                    // to the external activity.
                    val externalActivityIntent = Intent(Intent.ACTION_PICK)
                    externalActivityIntent.type = "image/*"
                    externalActivityIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                    startActivity(externalActivityIntent)
                }
            return rootView
        }
    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     */
    class DummySectionFragment : Fragment() {
        /**
         * Called to have the fragment instantiate its user interface view. We initialize `View rootView`
         * by using our parameter `LayoutInflater inflater` to inflate our layout file
         * R.layout.fragment_section_dummy using our parameter `ViewGroup container` for its
         * layout params without attaching to it. We initialize `Bundle args` to the argument
         * `Bundle` supplied when our fragment was instantiated. We find the view in `rootView`
         * with the id android.R.id.text1 and set its text to the string created by using the format
         * whose resource id is R.string.dummy_section_text ("Section %1d is just a dummy section.")
         * to format the int stored in `args` under the key ARG_SECTION_NUMBER ("section_number").
         * Finally we return `rootView` to the caller.
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
            val rootView = inflater.inflate(R.layout.fragment_section_dummy, container, false)
            val args = arguments
            (rootView.findViewById<View>(android.R.id.text1) as TextView).text = getString(R.string.dummy_section_text, args!!.getInt(ARG_SECTION_NUMBER))
            return rootView
        }

        companion object {
            /**
             * Key used to store the section number we represent in our arguments.
             */
            const val ARG_SECTION_NUMBER: String = "section_number"
        }
    }
}