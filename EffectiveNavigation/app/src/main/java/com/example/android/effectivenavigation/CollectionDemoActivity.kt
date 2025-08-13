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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "ReplaceJavaStaticMethodWithKotlinAnalog", "MemberVisibilityCanBePrivate",
    "UnusedImport"
)

package com.example.android.effectivenavigation

import android.annotation.SuppressLint
import android.app.ActionBar
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.android.effectivenavigation.CollectionDemoActivity.DemoObjectFragment

/**
 * Displays a collection of 100 [DemoObjectFragment] tabs in a [PagerAdapter]
 */
class CollectionDemoActivity : FragmentActivity() {
    /**
     * The [PagerAdapter] that will provide fragments representing each object in a collection. We
     * use a [FragmentStatePagerAdapter] derivative, which will destroy and re-create fragments as
     * needed, saving and restoring their state in the process. This is important to conserve memory
     * and is a best practice when allowing navigation between objects in a potentially large
     * collection.
     */
    var mDemoCollectionPagerAdapter: DemoCollectionPagerAdapter? = null

    /**
     * The [ViewPager] that will display the object collection.
     */
    var mViewPager: ViewPager? = null

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.activity_collection_demo`. Then we
     * initialize our [DemoCollectionPagerAdapter] field [mDemoCollectionPagerAdapter] with a new
     * instance constructed to use a handle to the [FragmentManager] for interacting with fragments
     * associated with this activity. We then initialize [ActionBar] variable `val actionBar` with a
     * reference to our activity's [ActionBar], and specify that the Home button should show an "Up"
     * caret, indicating that touching the button will take the user one step up in the application's
     * hierarchy. We initialize our field [ViewPager] field [mViewPager] by finding the view with id
     * `R.id.pager` and set its [PagerAdapter] to [mDemoCollectionPagerAdapter].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection_demo)

        // Create an adapter that when requested, will return a fragment representing an object in
        // the collection.
        // 
        // ViewPager and its adapters use support library fragments, so we must use
        // getSupportFragmentManager.
        mDemoCollectionPagerAdapter = DemoCollectionPagerAdapter(supportFragmentManager)

        // Set up action bar.
        val actionBar: ActionBar? = actionBar

        // Specify that the Home button should show an "Up" caret, indicating that touching the
        // button will take the user one step up in the application's hierarchy.
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        // Set up the ViewPager, attaching the adapter.
        mViewPager = findViewById(R.id.pager)
        mViewPager!!.adapter = mDemoCollectionPagerAdapter
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We `when` switch on
     * the value returned by the [MenuItem.getItemId] method (aka kotlin `itemId` property)of our
     * [MenuItem] parameter [item]:
     *
     *  * [android.R.id.home]: (This is called when the Home (Up) button is pressed in the action
     *  bar) We initialize [Intent] variable `val upIntent` with a new instance intended to launch
     *  our [MainActivity] activity. If the [NavUtils.shouldUpRecreateTask] method returns `true`,
     *  we need to synthesize a new task stack by using [TaskStackBuilder] to perform up navigation,
     *  which we do adding `upIntent` to the task stack and then chaining a call to the
     *  [TaskStackBuilder.startActivities] method to the [TaskStackBuilder] in order to start the
     *  task stack constructed by this builder, and we then call [finish] to end this activity. If
     *  the [NavUtils.shouldUpRecreateTask] method returns `false` this activity is part of the
     *  application's task, we so simply navigate up to the hierarchical parent activity by calling
     *  the [NavUtils.navigateUpTo] method to navigate from `this` up to the activity given in
     *  `upIntent` ([MainActivity]) and to finish `this` activity. In either case we return `true`
     *  to the caller to consume the event here.
     *
     * If the item selected was not [android.R.id.home] we return the value returned by our super's
     * implementation of `onOptionsItemSelected`.
     *
     * @param item The [MenuItem] that was selected.     *
     * @return Return `false` to allow normal menu processing to proceed, `true` to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // This is called when the Home (Up) button is pressed in the action bar.
                // Create a simple intent that starts the hierarchical parent activity and
                // use NavUtils in the Support Package to ensure proper handling of Up.
                val upIntent = Intent(this, MainActivity::class.java)
                if (this.shouldUpRecreateTask(upIntent)) {
                    // This activity is not part of the application's task, so create a new task
                    // with a synthesized back stack.
                    TaskStackBuilder.create(this) // If there are ancestor activities, they should be added here.
                        .addNextIntent(upIntent)
                        .startActivities()
                    finish()
                } else {
                    // This activity is part of the application's task, so simply
                    // navigate up to the hierarchical parent activity.
                    this.navigateUpTo(upIntent)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * A [FragmentStatePagerAdapter] that returns a fragment representing an object in the
     * collection.
     */
    class DemoCollectionPagerAdapter
    /**
     * Our constructor, we just call our super's constructor.
     *
     * @param fm a reference to the [FragmentManager] for interacting with fragments associated
     * with this activity.
     */
    (fm: FragmentManager?) : FragmentStatePagerAdapter(fm) {
        /**
         * Return the [Fragment] associated with a specified position. We initialize our [Fragment]
         * variable `val fragment` with a new instance of [DemoObjectFragment]. We initialize
         * [Bundle] variable `val args` with a new instance and store our [Int] parameter [i] plus
         * 1 under the key [DemoObjectFragment.ARG_OBJECT] ("object") in it. We then set the
         * argument bundle of `fragment` to `args` and return `fragment` to the caller.
         *
         * @param i the position of the [Fragment] our caller is interested in.
         * @return the Fragment associated with position [i].
         */
        override fun getItem(i: Int): Fragment {
            val fragment: Fragment = DemoObjectFragment()
            val args = Bundle()
            args.putInt(DemoObjectFragment.ARG_OBJECT, i + 1) // Our object is just an integer :-P
            fragment.arguments = args
            return fragment
        }

        /**
         * Return the number of views available. We just return 100.
         *
         * @return number of views available, 100 in our case
         */
        override fun getCount(): Int {
            // For this contrived example, we have a 100-object collection.
            return 100
        }

        /**
         * This method may be called by the ViewPager to obtain a title string to describe the
         * specified page. We just return the string formed by concatenating the string value of
         * our [Int] parameter [position] plus 1 to the end of the string "OBJECT ".
         *
         * @param position The position of the title requested
         * @return A title for the requested page
         */
        override fun getPageTitle(position: Int): CharSequence {
            return "OBJECT " + (position + 1)
        }
    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     */
    class DemoObjectFragment : Fragment() {
        /**
         * Called to have the fragment instantiate its user interface view. We initialize [View]
         * variable `val rootView` by using our [LayoutInflater] parameter [inflater] to inflate
         * our layout file `R.layout.fragment_collection_object` using our [ViewGroup] parameter
         * [container] for layout params without attaching to it. We then initialize [Bundle]
         * variable `val args` by fetching our argument [Bundle]. We find the view in `rootView`
         * with the id [android.R.id.text1] and set its text to the string value of the [Int]
         * stored in `args` under the key [ARG_OBJECT]. Finally we return `rootView` to the caller.
         *
         * @param inflater The [LayoutInflater] object that can be used to inflate any
         * views in the fragment,
         * @param container If non-`null`, this is the parent view that the fragment's
         * UI will be attached to. The fragment should not add the view itself, but this
         * can be used to generate the LayoutParams of the view.
         * @param savedInstanceState If non-`null`, this fragment is being re-constructed
         * from a previous saved state as given here.
         *
         * @return Return the [View] for the fragment's UI, or null.
         */
        @SuppressLint("SetTextI18n")
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val rootView = inflater.inflate(
                R.layout.fragment_collection_object,
                container,
                false
            )
            val args = arguments
            (rootView.findViewById<View>(android.R.id.text1) as TextView)
                .text = Integer.toString(args!!.getInt(ARG_OBJECT))
            return rootView
        }

        companion object {
            /**
             * Key used to store the page number we represent in our argument `Bundle`
             */
            const val ARG_OBJECT: String = "object"
        }
    }
}
