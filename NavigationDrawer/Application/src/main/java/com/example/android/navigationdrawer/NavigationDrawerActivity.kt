/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.navigationdrawer

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.graphics.Insets
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

/**
 * This example illustrates a common usage of the DrawerLayout widget in the Android support
 * library. When a navigation (left) drawer is present, the host activity should detect presses of
 * the action bar's Up affordance as a signal to open and close the navigation drawer. The
 * [ActionBarDrawerToggle] facilitates this behavior. Items within the drawer should fall into one
 * of two categories:
 *  * **View switches**. A view switch follows the same basic policies as list or tab navigation in
 *  that a view switch does not create navigation history. This pattern should only be used at the
 *  root activity of a task, leaving some form of Up navigation active for activities further down
 *  the navigation hierarchy.
 *
 *  * **Selective Up**. The drawer allows the user to choose an alternate parent for Up navigation.
 *  This allows a user to jump across an app's navigation hierarchy at will. The application should
 *  treat this as it treats Up navigation from a different task, replacing the current task stack
 *  using `TaskStackBuilder` or similar. This is the only form of navigation drawer that should be
 *  used outside of the root activity of a task.
 *
 * Right side drawers should be used for actions, not navigation. This follows the pattern
 * established by the Action Bar that navigation should be to the left and actions to the right.
 * An action should be an operation performed on the current contents of the window, for example
 * enabling or disabling a data overlay on top of the current content.
 */
class NavigationDrawerActivity : FragmentActivity(), PlanetAdapter.OnItemClickListener {
    /**
     * View in our layout with ID `R.id.drawer_layout` (the entire layout file)
     */
    private var mDrawerLayout: DrawerLayout? = null

    /**
     * [RecyclerView] in our layout with ID `R.id.left_drawer`
     */
    private var mDrawerList: RecyclerView? = null

    /**
     * Our [ActionBarDrawerToggle], which is configured to tie together the sliding drawer and
     * the action bar app icon, so the "Up" button opens the sliding drawer
     */
    private var mDrawerToggle: ActionBarDrawerToggle? = null

    /**
     * title associated with this activity: "Navigation Drawer", used to reset the action bar title
     */
    private var mDrawerTitle: CharSequence? = null

    /**
     * Title of the currently selected planet of the drawer, set when the drawer closes and used to
     * set the action bar title.
     */
    private var mTitle: CharSequence? = null

    /**
     * Array of names of the planets, loaded from the string array resource `R.array.planets_array`,
     * and used as the data set that populates the list view of the drawer and to set the app title
     * when one of the planets is selected.
     */
    private lateinit var mPlanetTitles: Array<String>

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge
     * to edge display, then we call our super's implementation of `onCreate`, and set our
     * content view to our layout file `R.layout.activity_navigation_drawer`.
     *
     * We initialize our [DrawerLayout] field [mDrawerLayout] to the view with ID `R.id.drawer_layout`
     * then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to [mDrawerLayout], with the `listener` argument a lambda that accepts the [View]
     * passed the lambda in variable `v` and the [WindowInsetsCompat] passed the lambda in variable
     * `windowInsets`. It initializes its [Insets] variable `systemBars` to the
     * [WindowInsetsCompat.getInsets] of `windowInsets` with [WindowInsetsCompat.Type.systemBars] as
     * the argument. It then gets the insets for the IME (keyboard) using
     * [WindowInsetsCompat.Type.ime]. It then updates the layout parameters of `v` to be a
     * [ViewGroup.MarginLayoutParams] with the left margin set to `systemBars.left`, the right
     * margin set to `systemBars.right`, the top margin set to `systemBars.top`, and the bottom
     * margin set to the maximum of the system bars bottom inset and the IME bottom inset.
     * Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so that the window insets
     * will not keep passing down to descendant views).
     *
     * We initialize [CharSequence] field [mTitle] and [CharSequence] field [mDrawerTitle] with the
     * action bar title. We initialize [Array] of [String] field [mPlanetTitles] by using a
     * [Resources] instance for the application's package to read the string array associated with
     * the resource ID `R.array.planets_array` (a list of the planet names). We initialize
     * [RecyclerView] field [mDrawerList] by finding the view with id `R.id.left_drawer`. We set
     * the nine patch png with resource id `R.drawable.drawer_shadow` to be a custom shadow that
     * overlays the main content of [mDrawerLayout] when the drawer opens. We call the
     * [RecyclerView.setHasFixedSize] method of [RecyclerView] field [mDrawerList] with `true` to
     * improve performance by indicating that the list has fixed size. We set the adapter of
     * [mDrawerList] to be a new instance of [PlanetAdapter] constructed to use [Array] of [String]
     * field [mPlanetTitles] as its data set, and `this` as its [PlanetAdapter.OnItemClickListener].
     * We retrieve a reference to this activity's [ActionBar], to set home to be displayed as an
     * "up" affordance, and enable the "home" button. We initialize our [ActionBarDrawerToggle]
     * field [mDrawerToggle] with a new instance which uses our [DrawerLayout] field [mDrawerLayout]
     * as its [DrawerLayout] object, the icon with resource id `R.drawable.ic_drawer` as the drawer
     * image to replace 'Up' caret, the string with resource id `R.string.drawer_open` for the
     * "open drawer" description for accessibility, and the string with resource id
     * `R.string.drawer_close` as the "close drawer" description for accessibility. In this
     * [ActionBarDrawerToggle] we override [DrawerLayout.DrawerListener.onDrawerClosed] in order to
     * set the title of the action bar to [mTitle] and invalidate the options menu so that our
     * [onPrepareOptionsMenu] override will be called, and [DrawerLayout.DrawerListener.onDrawerOpened]
     * in order to set the title of the action bar to [mDrawerTitle] and invalidate the options menu.
     * We then set the [DrawerLayout.DrawerListener] of [DrawerLayout]field [mDrawerLayout] to
     * [mDrawerToggle]. If our [Bundle] parameter [savedInstanceState] is `null` this is the first
     * time we were called so we call our [selectItem] method to select position 0 to display (Mercury).
     *
     * @param savedInstanceState If this is `null`, it is the first time we have been called, so we
     * select item 0 in our drawer.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_drawer)
        mDrawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        ViewCompat.setOnApplyWindowInsetsListener(mDrawerLayout!!) { v: View, windowInsets: WindowInsetsCompat ->
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
        mDrawerTitle = title
        mTitle = mDrawerTitle
        mPlanetTitles = resources.getStringArray(R.array.planets_array)
        mDrawerList = findViewById(R.id.left_drawer)

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout!!.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        // improve performance by indicating that the list has fixed size.
        mDrawerList!!.setHasFixedSize(true)

        // set up the drawer's list view with items and click listener
        mDrawerList!!.adapter = PlanetAdapter(mPlanetTitles, this)
        // enable ActionBar app icon to behave as action to toggle nav drawer
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar!!.setHomeButtonEnabled(true)

        // ActionBarDrawerToggle ties together the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = object : ActionBarDrawerToggle(
            this,  /* host Activity */
            mDrawerLayout,  /* DrawerLayout object */
            R.string.drawer_open,  /* "open drawer" description for accessibility */
            R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            /**
             * [DrawerLayout.DrawerListener] callback method. If you do not use your
             * [ActionBarDrawerToggle] instance directly as your DrawerLayout's listener,
             * you should call through to this method from your own listener object.
             *
             * @param view Drawer [View] that is now closed
             */
            override fun onDrawerClosed(view: View) {
                actionBar!!.title = mTitle
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }

            /**
             * [DrawerLayout.DrawerListener] callback method. If you do not use your
             * [ActionBarDrawerToggle] instance directly as your DrawerLayout's listener,
             * you should call through to this method from your own listener object.
             *
             * @param drawerView Drawer [View] that is now open
             */
            override fun onDrawerOpened(drawerView: View) {
                actionBar!!.title = mDrawerTitle
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }
        }
        mDrawerLayout!!.addDrawerListener(mDrawerToggle!!)
        if (savedInstanceState == null) {
            selectItem(0)
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We use a [MenuInflater]
     * for this context to inflate our menu layout file `R.menu.navigation_drawer` into our [Menu]
     * parameter [menu] and return `true` to the caller so that the menu will be displayed.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return `true` for the menu to be displayed, which we do.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.navigation_drawer, menu)
        return true
    }

    /**
     * Prepare the Screen's standard options menu to be displayed. We initialize [Boolean] variable
     * `val drawerOpen` with the current open state of the "DrawerView" of [DrawerLayout] field
     * [mDrawerLayout], our [RecyclerView] field [mDrawerList]. Then we set the visibility of the
     * view with item id `R.id.action_websearch` in our [Menu] parameter [menu] to visibile if
     * `drawerOpen` is `false` or invisible if the drawer is open. Finally we return the value
     * returned by our super's implementation of `onPrepareOptionsMenu`.
     *
     * @param menu The options menu as last shown or first initialized by [onCreateOptionsMenu].
     * @return You must return `true` for the menu to be displayed
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // If the nav drawer is open, hide action items related to the content view
        val drawerOpen: Boolean = mDrawerLayout!!.isDrawerOpen(mDrawerList!!)
        menu.findItem(R.id.action_websearch).isVisible = !drawerOpen
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. First we call the
     * [ActionBarDrawerToggle.onOptionsItemSelected] method of [ActionBarDrawerToggle] field
     * [mDrawerToggle] and if it returns `true` (the action bar home/up action was selected) we
     * return `true` to the caller since it has taken care of the event. Otherwise we switch on
     * the item ID of our [MenuItem] parameter [item]:
     *
     *  * `R.id.action_websearch`: We construct [Intent] variable `val intent` with the action
     *  [Intent.ACTION_WEB_SEARCH], add the current title in the action bar as an extra under the
     *  key [SearchManager.QUERY], then if the package manager is able to resolve an app for the
     *  [Intent] `intent` we start the activity of `intent`, if not we toast a message ("Sorry,
     *  there's no web browser available"). Finally we return `true` to consume the event here.
     *
     *  * any other item ID: we return the value returned by our super's implementation of
     *  `onOptionsItemSelected`.
     *
     * @param item The menu item that was selected.
     * @return Return `false` to allow normal menu processing to proceed, `true` to consume it here.
     */
    @SuppressLint("QueryPermissionsNeeded")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle!!.onOptionsItemSelected(item)) {
            return true
        }
        // Handle action buttons
        if (item.itemId == R.id.action_websearch) { // create intent to perform web search for this planet
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.putExtra(SearchManager.QUERY, actionBar!!.title)
            // catch event that there's no activity to handle intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_LONG).show()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * The click listener for the [RecyclerView] in the navigation drawer. Each [TextView] in the
     * [PlanetAdapter.ViewHolder] for a list item in the [PlanetAdapter] has its [OnClickListener]
     * set to an anonymous class which calls this override (we implement
     * [PlanetAdapter.OnItemClickListener]). We just call our method [selectItem] with our [Int]
     * parameter [position].
     *
     * @param view [View] that was clicked
     * @param position position in the [PlanetAdapter] that was clicked.
     */
    override fun onClick(view: View, position: Int) {
        selectItem(position)
    }

    /**
     * Updates the main content to display the planet in the selected position. First we initialize
     * [Fragment] variable `val fragment` with a new instance of [PlanetFragment] for the planet in
     * the position of our [Int] parameter [position] of our array of planets. We initialize
     * [FragmentManager] variable `val fragmentManager` with the [FragmentManager] for interacting
     * with fragments associated with this activity, then use it to begin [FragmentTransaction]
     * variable `val ft`. We ask `ft` to replace the fragment occupying the view with id
     * `R.id.content_frame` with `fragment`, then request it to commit its transactions. We call
     * our method [setTitle] to change the title in the action bar to the string at index [position]
     * in our [Array] of [String] field [mPlanetTitles]`, then order [DrawerLayout] field
     * [mDrawerLayout] to close the drawer containing [RecyclerView] field [mDrawerList].
     *
     * @param position position of the planet name in the [Array] of [String] field [mPlanetTitles]
     * we are to display.
     */
    private fun selectItem(position: Int) {
        // update the main content by replacing fragments
        val fragment = PlanetFragment.newInstance(position)
        val fragmentManager: FragmentManager = supportFragmentManager
        val ft: FragmentTransaction = fragmentManager.beginTransaction()
        ft.replace(R.id.content_frame, fragment)
        ft.commit()

        // update selected item title, then close the drawer
        title = mPlanetTitles[position]
        mDrawerLayout!!.closeDrawer(mDrawerList!!)
    }

    /**
     * Sets the title of the action bar to our [CharSequence] parameter [title]. First we save
     * our parameter in the [CharSequence] field [mTitle], then we retrieve a reference to this
     * activity's [ActionBar] and command it to change its title to [mTitle].
     *
     * @param title string to set the title of the action bar to
     */
    override fun setTitle(title: CharSequence) {
        mTitle = title
        actionBar!!.title = mTitle
    }

    /**
     * Called when activity start-up is complete (after [onStart] and [onRestoreInstanceState] have
     * been called). When using the [ActionBarDrawerToggle], you must call it during [onPostCreate]
     * and [onConfigurationChanged]. First we call our super's implementation of `onPostCreate`,
     * then we call the [ActionBarDrawerToggle.syncState] method of our [ActionBarDrawerToggle]
     * field  [mDrawerToggle] to Synchronize the state of the drawer indicator/affordance with the
     * linked [DrawerLayout].
     *
     * @param savedInstanceState We do not override `onSaveInstanceState` so do not use.
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle!!.syncState()
    }

    /**
     * Called by the system when the device configuration changes while your activity is running.
     * First we call our super's implementation of `onConfigurationChanged`, then we call the
     * [ActionBarDrawerToggle.onConfigurationChanged] method of our [ActionBarDrawerToggle] field
     * [mDrawerToggle] with our [Configuration] parameter [newConfig] to reload drawables that can
     * change with configuration, and to Synchronize the state of the drawer indicator/affordance
     * with the linked [DrawerLayout].
     *
     * @param newConfig The new device configuration.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Pass any configuration change to the drawer toggles
        mDrawerToggle!!.onConfigurationChanged(newConfig)
    }

    /**
     * Fragment that appears in the "content_frame", shows a planet
     */
    class PlanetFragment
    /**
     * Empty constructor required for fragment subclasses
     */
        : Fragment() {
        /**
         * Called to have the fragment instantiate its user interface view. We initialize [View]
         * variable `val rootView` by using our [LayoutInflater] parameter [inflater] to inflate our
         * layout file `R.layout.fragment_planet` using our [ViewGroup] parameter [container] for
         * the LayoutParams without attaching to it. We initialize [Int] variable `val i` by fetching
         * the int stored under the key [ARG_PLANET_NUMBER] in our arguments bundle. We then initialize
         * [String] variable `val planet` to the [String] in position `i` of the string array with
         * resource id `R.array.planets_array`. We initialize [Int] variable `val imageId` by using
         * a [Resources] instance for the application's package to find a resource identifier
         * for the resource name formed from the lower-cased `planet` name and the type "drawable"
         * for the package name of our activity. We initialize [ImageView] variable `val iv` by
         * finding the view in `rootView` with id `R.id.image`, then set its image to the drawable
         * with resource id `imageId`. Next we call the [FragmentActivity.setTitle] method (kotlin
         * `title` property) of our activity to set the title of the action bar to `planet`. Finally
         * we return `rootView` to the caller.
         *
         * @param inflater The [LayoutInflater] object that can be used to inflate
         * any views in the fragment.
         * @param container If non-`null`, this is the parent view that the fragment's UI will be
         * attached to. The fragment should not add the view itself, but this can be used to
         * generate the LayoutParams of the view.
         * @param savedInstanceState If non-`null`, this fragment is being re-constructed
         * from a previous saved state as given here. We do not override [onSaveInstanceState]
         * so do not use.
         * @return Return the View for the fragment's UI.
         */
        @SuppressLint("DiscouragedApi")
        @Deprecated("Deprecated in Java")
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val rootView = inflater.inflate(R.layout.fragment_planet, container, false)
            val i = requireArguments().getInt(ARG_PLANET_NUMBER)
            val planet = resources.getStringArray(R.array.planets_array)[i]
            val imageId: Int = resources.getIdentifier(
                /* name = */ planet.lowercase(Locale.getDefault()),
                /* defType = */ "drawable",
                /* defPackage = */ requireActivity().packageName
            )
            val iv = rootView.findViewById<ImageView>(R.id.image)
            iv.setImageResource(imageId)
            requireActivity().title = planet
            return rootView
        }

        companion object {
            /**
             * Key in our argument bundle for the planet position number.
             */
            const val ARG_PLANET_NUMBER: String = "planet_number"

            /**
             * Factory method to construct an instance of [PlanetFragment] configured to display
             * the planet in the planets array whose index is our [Int] parameter [position]. We
             * initialize [Fragment] variable `val fragment` with a new instance of [PlanetFragment],
             * and [Bundle] variable `val args` with a new instance. We store our [Int] parameter
             * `position` under the key [ARG_PLANET_NUMBER] in `args` and set the argument
             * bundle of `fragment` to `args`. Finally we return `fragment` to the caller.
             *
             * @param position position of the planet in the planets array that we are to display
             * @return new instance of [PlanetFragment] configured to display the planet in the
             * planets array at position [position].
             */
            fun newInstance(position: Int): Fragment {
                val fragment: Fragment = PlanetFragment()
                val args = Bundle()
                args.putInt(ARG_PLANET_NUMBER, position)
                fragment.arguments = args
                return fragment
            }
        }
    }
}
