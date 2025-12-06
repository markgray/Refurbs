@file:Suppress("DEPRECATION", "KDocUnresolvedReference", "ReplaceNotNullAssertionWithElvisReturn", "ReplaceJavaStaticMethodWithKotlinAnalog", "MemberVisibilityCanBePrivate")

package com.example.android.horizontalpaging

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.FragmentTransaction
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import java.util.Locale

/**
 * This sample shows how to implement tabs, using Fragments and a ViewPager.
 */
class MainActivity : FragmentActivity(), ActionBar.TabListener {
    /**
     * The [PagerAdapter] that will provide fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every loaded fragment in memory.
     * If this becomes too memory intensive, it may be best to switch to a
     * [FragmentStatePagerAdapter].
     */
    var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    /**
     * The [ViewPager] that will host the section contents.
     */
    var mViewPager: ViewPager? = null

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge
     * to edge display, then we call our super's implementation of `onCreate`, and set our
     * content view to our layout file `R.layout.sample_main` (it holds only a [ViewPager]
     * widget).
     *
     * We initialize our [ViewPager] variable `rootView` to the view with ID `R.id.pager`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
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
     * Next we set up an [ActionBar] with tabs, and then configure the [ViewPager] contained inside
     * `R.layout.activity_main`. A [SectionsPagerAdapter] will be instantiated to hold the different
     * pages of fragments that are to be displayed. A [ViewPager.SimpleOnPageChangeListener] will
     * also be configured to receive callbacks when the user swipes between pages in the ViewPager.
     *
     * We initialize [ActionBar] variable `val actionBar` with the [Activity]'s [ActionBar],
     * and set its navigation mode to [ActionBar.NAVIGATION_MODE_TABS] (tab navigation mode, instead
     * of static title text this mode presents a series of tabs for navigation within the activity).
     * We initialize our [SectionsPagerAdapter] field [mSectionsPagerAdapter] with a new instance
     * constructed using the [FragmentManager] for interacting with fragments associated with this
     * activity for the various [FragmentTransaction] operations it uses to swap fragments. We
     * initialize our [ViewPager] field [mViewPager] by finding the view in our layout file with id
     * `R.id.pager` set its adapter to [mSectionsPagerAdapter], and set its [OnPageChangeListener]
     * to an anonymous [SimpleOnPageChangeListener] whose [SimpleOnPageChangeListener.onPageSelected]
     * override calls the [setSelectedNavigationItem] method of [ActionBar] variable `actionBar`
     * with the Position index of the newly selected page `position` in order to set its selected
     * tab to the selected page. Finally we loop over [Int] variable `var i` for all of the pages
     * in the data set of [SectionsPagerAdapter] field [mSectionsPagerAdapter] adding a new tab
     * to `actionBar` whose text consists of the page title for `i`, and whose [TabListener]
     * is `this`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Load the UI from layout/sample_main.xml
        setContentView(R.layout.sample_main)
        val rootView = findViewById<ViewPager>(R.id.pager)
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

        // Set up the action bar. The navigation mode is set to NAVIGATION_MODE_TABS, which will
        // cause the ActionBar to render a set of tabs. Note that these tabs are *not* rendered
        // by the ViewPager; additional logic is lower in this file to synchronize the ViewPager
        // state with the tab state. (See mViewPager.setOnPageChangeListener() and onTabSelected().)
        val actionBar = actionBar
        actionBar!!.navigationMode = ActionBar.NAVIGATION_MODE_TABS

        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = rootView
        mViewPager!!.adapter = mSectionsPagerAdapter

        // When swiping between different sections, select the corresponding tab. We can also use
        // ActionBar.Tab#select() to do this if we have a reference to the Tab.
        mViewPager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            /**
             * This method will be invoked when a new page becomes selected. Animation is not
             * necessarily complete. We call the `setSelectedNavigationItem` method of
             * `ActionBar actionBar` with the Position index of the newly selected page
             * `position` in order to set its selected tab to the selected page.
             *
             * @param position Position index of the newly selected page.
             */
            override fun onPageSelected(position: Int) {
                actionBar.setSelectedNavigationItem(position)
            }
        })

        // For each of the sections in the app, add a tab to the action bar.
        for (i in 0 until mSectionsPagerAdapter!!.count) {
            // Create a tab with text corresponding to the page title defined by the adapter. Also
            // specify this Activity object, which implements the TabListener interface, as the
            // callback (listener) for when this tab is selected.
            actionBar.addTab(
                actionBar.newTab()
                    .setText(mSectionsPagerAdapter!!.getPageTitle(i))
                    .setTabListener(this))
        }
    }

    /**
     * Update [ViewPager] after a tab has been selected in the [ActionBar]. We just call the
     * [ViewPager.setCurrentItem] method of our [ViewPager] field [mViewPager] to set the currently
     * selected page to the current position of our [ActionBar.Tab] parameter [tab] in the action
     * bar.
     *
     * @param tab                 Tab that was selected.
     * @param fragmentTransaction A [FragmentTransaction] for queuing fragment operations to execute
     * once this method returns. This [FragmentTransaction] does not support being added to the back
     * stack.
     */
    @Deprecated("Deprecated in Java", ReplaceWith("mViewPager!!.currentItem = tab.position"))
    override fun onTabSelected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) {
        // When the given tab is selected, tell the ViewPager to switch to the corresponding page.
        mViewPager!!.currentItem = tab.position
    }

    /**
     * Called when a tab exits the selected state. Unused. Required for the [TabListener] interface.
     *
     * @param tab The tab that was unselected
     * @param fragmentTransaction A [FragmentTransaction] for queuing fragment operations
     * to execute during a tab switch. This tab's unselect and the newly selected tab's
     * select will be executed in a single transaction. This [FragmentTransaction] does not
     * support being added to the back stack.
     */
    @Deprecated("Deprecated in Java")
    override fun onTabUnselected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) {}

    /**
     * Called when a tab that is already selected is chosen again by the user. Some applications may
     * use this action to return to the top level of a category. Unused. Required for [TabListener].
     *
     * @param tab The tab that was reselected.
     * @param fragmentTransaction A [FragmentTransaction] for queuing fragment operations to
     * execute once this method returns. This [FragmentTransaction] does not support being added
     * to the back stack.
     */
    @Deprecated("Deprecated in Java")
    override fun onTabReselected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) {}

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages. This provides the data for the [ViewPager].
     */
    inner class SectionsPagerAdapter
    /**
     * Our constructor, we just call our super's constructor.
     *
     * @param fm [FragmentManager] to use to perform any [FragmentTransaction] needed
     */
    (fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
        /**
         * Get fragment corresponding to a specific position. This will be used to populate the
         * contents of the [ViewPager]. We initialize [Fragment] variable `val fragment` with a new
         * instance of [DummySectionFragment]. We initialize [Bundle] variable `val args` with a new
         * instance, store the [Int] formed by adding one to our [Int] parameter [position] in it
         * under the key [ARG_SECTION_NUMBER], and set it to be the argument bundle of `fragment`.
         * Finally we return `fragment` to the caller.
         *
         * @param position Position to fetch fragment for.
         * @return [Fragment] for specified position.
         */
        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            val fragment: Fragment = DummySectionFragment()
            val args = Bundle()
            args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1)
            fragment.arguments = args
            return fragment
        }

        /**
         * Get number of pages the [ViewPager] should render. We return 3.
         *
         * @return Number of fragments to be rendered as pages.
         */
        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }

        /**
         * Get title for each of the pages. This will be displayed on each of the tabs. We initialize
         * [Locale] variable `val l` with the current value of the default locale for this instance
         * of the Java Virtual Machine, Then we `when` switch on the value of our [Int] parameter
         * [position]:
         *
         *  * 0: we return the string with resource id `R.string.title_section1` ("Section 1")
         *  with all of the characters in this [String] converted to upper case using the rules
         *  of [Locale] `l`.
         *
         *  * 1: we return the string with resource id `R.string.title_section2` ("Section 2")
         *  with all of the characters in this [String] converted to upper case using the rules
         * of [Locale] `l`.
         *
         *  * 2: we return the string with resource id `R.string.title_section3` ("Section 3")
         *  with all of the characters in this [String] converted to upper case using the rules
         * of [Locale] `l`.
         *
         * If it is not a position we use, we return `null`.
         *
         * @param position Page to fetch title for.
         * @return Title for specified page.
         */
        override fun getPageTitle(position: Int): CharSequence? {
            val l = Locale.getDefault()
            when (position) {
                0 -> return getString(R.string.title_section1).uppercase(l)
                1 -> return getString(R.string.title_section2).uppercase(l)
                2 -> return getString(R.string.title_section3).uppercase(l)
            }
            return null
        }
    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     * This would be replaced with your application's content.
     */
    class DummySectionFragment
    /**
     * Our constructor, we do nothing.
     */
        : Fragment() {
        /**
         * Called to have the fragment instantiate its user interface view. We initialize our [View]
         * variable `val rootView` with the view that our [LayoutInflater] parameter [inflater]
         * inflates from our layout file `R.layout.fragment_main_dummy` using our [ViewGroup]
         * parameter [container] for `LayoutParams` without attaching to it. We initialize [TextView]
         * variable `val dummyTextView` by finding the view in `rootView` with id `R.id.section_label`,
         * then set its text to the string formed by converting the [Int] stored in our argument
         * [Bundle] under the key [ARG_SECTION_NUMBER] to a string. Finally we return `rootView` to the caller.
         *
         * @param inflater The [LayoutInflater] object that can be used to inflate any views
         * in the fragment,
         * @param container If non-`null`, this is the parent view that the fragment's UI will be
         * attached to. The fragment should not add the view itself, but this can be used to
         * generate the LayoutParams of the view.
         * @param savedInstanceState If non-`null`, this fragment is being re-constructed
         * from a previous saved state as given here.
         * @return Return the [View] for the fragment's UI, or `null`.
         */
        @SuppressLint("SetTextI18n")
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val rootView = inflater.inflate(R.layout.fragment_main_dummy, container, false)
            val dummyTextView = rootView.findViewById<TextView>(R.id.section_label)
            dummyTextView.text = Integer.toString(requireArguments().getInt(ARG_SECTION_NUMBER))
            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            const val ARG_SECTION_NUMBER: String = "section_number"
        }
    }
}
