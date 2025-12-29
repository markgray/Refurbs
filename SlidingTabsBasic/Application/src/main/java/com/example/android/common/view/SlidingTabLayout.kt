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
@file:Suppress(
    "ReplaceNotNullAssertionWithElvisReturn",
    "MemberVisibilityCanBePrivate",
    "UnusedImport"
)

package com.example.android.common.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.example.android.common.view.SlidingTabLayout.TabColorizer

/**
 * To be used with [ViewPager] to provide a tab indicator component which gives constant feedback as
 * to the user's scroll progress. To use the component, simply add it to your view hierarchy. Then
 * in your [android.app.Activity] or [Fragment] call [setViewPager] providing it the ViewPager this
 * layout is being used for.
 *
 * The colors can be customized in two ways. The first and simplest is to provide an array of colors
 * via [setSelectedIndicatorColors] and [setDividerColors]. The alternative is via the [TabColorizer]
 * interface which provides you complete control over which color is used for any individual position.
 *
 * The views used as tabs can be customized by calling [setCustomTabView], providing the layout ID
 * of your custom layout.
 */
open class SlidingTabLayout
/**
 * Perform inflation from XML and apply a class-specific base style from a
 * theme attribute or style resource.
 *
 * @param context The [Context] the view is running in, through which it can
 * access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyle An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for
 * the view. Can be 0 to not look for defaults.
 */
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : HorizontalScrollView(context, attrs, defStyle) {
    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     * [setCustomTabColorizer].
     */
    interface TabColorizer {
        /**
         * Returns the color of the indicator for the given [Int] parameter [position] position.
         *
         * @param position position we are to provide the color for.
         * @return return the color of the indicator used when `position` is selected.
         */
        fun getIndicatorColor(position: Int): Int

        /**
         * Returns the color of the divider drawn to the right of [Int] parameter [position].
         *
         * @param position position we are to provide the color for.
         * @return return the color of the divider drawn to the right of `position`.
         */
        fun getDividerColor(position: Int): Int
    }

    /**
     * Offset of the title in pixels ([TITLE_OFFSET_DIPS] times the display density).
     */
    private val mTitleOffset: Int

    /**
     * Resource ID of a custom layout to inflate for our tab view instead of the default one.
     */
    private var mTabViewLayoutId = 0

    /**
     * ID of the tab TextView inside the tab view when a custom layout is used.
     */
    private var mTabViewTextViewId = 0

    /**
     * [ViewPager] that this [SlidingTabLayout] is associated with, set by calling our
     * [setViewPager] method.
     */
    private var mViewPager: ViewPager? = null

    /**
     * [OnPageChangeListener] whose [OnPageChangeListener.onPageScrolled],
     * [OnPageChangeListener.onPageScrollStateChanged], and [OnPageChangeListener.onPageSelected]
     * overrides we are to call when our [OnPageChangeListener] overrides are called. Set in our
     * [setOnPageChangeListener] method. Never used in this sample.
     */
    private var mViewPagerPageChangeListener: OnPageChangeListener? = null

    /**
     * The [LinearLayout] which contains all our tabs.
     */
    private val mTabStrip: SlidingTabStrip

    init {
        // Disable the Scroll Bar
        isHorizontalScrollBarEnabled = false
        // Make sure that the Tab Strips fills this View
        isFillViewport = true
        mTitleOffset = (TITLE_OFFSET_DIPS * resources.displayMetrics.density).toInt()
        mTabStrip = SlidingTabStrip(context)
        @Suppress("LeakingThis")
        addView(mTabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    /**
     * Set the custom [TabColorizer] to be used. We just call the [SlidingTabStrip.setCustomTabColorizer]
     * method of our [SlidingTabStrip] field [mTabStrip]. If you only require simple customization
     * then you can use [setSelectedIndicatorColors] and [setDividerColors] to achieve similar
     * effects.
     *
     * @param tabColorizer the [TabColorizer] instance that our [SlidingTabStrip] field [mTabStrip]
     * should use.
     */
    fun setCustomTabColorizer(tabColorizer: TabColorizer?) {
        mTabStrip.setCustomTabColorizer(tabColorizer)
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a
     * circular array. Providing one color will mean that all tabs are indicated with the same color.
     * We just call the [SlidingTabStrip.setSelectedIndicatorColors] method of our [SlidingTabStrip]
     * field [mTabStrip].
     *
     * @param colors array (or  Varargs) of colors
     */
    fun setSelectedIndicatorColors(vararg colors: Int) {
        mTabStrip.setSelectedIndicatorColors(*colors)
    }

    /**
     * Sets the colors to be used for tab dividers. These colors are treated as a circular array.
     * Providing one color will mean that all tabs are indicated with the same color. We just call
     * the [SlidingTabStrip.setDividerColors] method of our [SlidingTabStrip] field [mTabStrip].
     *
     * @param colors array (or  Varargs) of colors
     */
    fun setDividerColors(vararg colors: Int) {
        mTabStrip.setDividerColors(*colors)
    }

    /**
     * Set the [ViewPager.OnPageChangeListener]. When using [SlidingTabLayout] you are
     * required to set any [ViewPager.OnPageChangeListener] through this method. This is so
     * that the layout can update it's scroll position correctly. We just save our parameter in our
     * [OnPageChangeListener] field [mViewPagerPageChangeListener]
     *
     * @param listener [OnPageChangeListener] we are to use
     * @see ViewPager.setOnPageChangeListener
     */
    fun setOnPageChangeListener(listener: OnPageChangeListener?) {
        mViewPagerPageChangeListener = listener
    }

    /**
     * Set the custom layout to be inflated for the tab views. We save our [Int] parameter
     * [layoutResId] in our [Int] field [mTabViewLayoutId], and our [Int] parameter [textViewId]
     * in our [Int] field [mTabViewTextViewId].
     *
     * @param layoutResId Layout id to be inflated
     * @param textViewId id of the [TextView] in the inflated view
     */
    fun setCustomTabView(layoutResId: Int, textViewId: Int) {
        mTabViewLayoutId = layoutResId
        mTabViewTextViewId = textViewId
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made. We call the
     * [SlidingTabStrip.removeAllViews] method of our [SlidingTabStrip] field [mTabStrip] to remove
     * all child views from the [ViewGroup], then save our [ViewPager] parameter [viewPager] in our
     * [ViewPager] field [mViewPager]. If [viewPager] is not `null`, we add a new instance of
     * [InternalViewPagerListener] as the [OnPageChangeListener] of [viewPager], then call our
     * method [populateTabStrip] to populate our [SlidingTabStrip] field [mTabStrip] with tab views
     * whose text is set to the title of the pages in [ViewPager] field [mViewPager] as determined
     * by calling the [PagerAdapter.getPageTitle] method of the adapter for each page it contains.
     *
     * @param viewPager our associated view pager.
     */
    fun setViewPager(viewPager: ViewPager?) {
        mTabStrip.removeAllViews()
        mViewPager = viewPager
        if (viewPager != null) {
            viewPager.addOnPageChangeListener(InternalViewPagerListener())
            populateTabStrip()
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set via
     * [setCustomTabView]. We initialize [TextView] variable `val textView` with a new instance,
     * set its gravity to [Gravity.CENTER], set its text size to [TAB_VIEW_TEXT_SIZE_SP] (12) in
     * [TypedValue.COMPLEX_UNIT_SP] units, and set its type face to [Typeface.DEFAULT_BOLD]. We
     * initialize [TypedValue] variable `val outValue` with a new instance, retrieve the attribute
     * value for [android.R.attr.selectableItemBackground] from the Theme object associated with our
     * [Context] into it, walking the resource references, and then set the background of `textView`
     * to the resource whose id is now stored in the [TypedValue.resourceId] field of `outValue`.
     * If the SDK version of our device is greater than or equal to `ICE_CREAM_SANDWICH` we set
     * `textView` to transform input to ALL CAPS. We calculate [Int] variable `val padding` by
     * multiplying [TAB_VIEW_PADDING_DIPS] by the logical density of the display, then set the
     * padding of `textView` to it on all four sides. Finally we return `textView` to the caller.
     *
     * @param context the context of the [SlidingTabLayout] that the view is running in
     * @return a [TextView] configured to be used as a tab view in a `SlidingTabStrip`
     */
    @SuppressLint("ObsoleteSdkInt")
    protected fun createDefaultTabView(context: Context?): TextView {
        val textView = TextView(context)
        textView.gravity = Gravity.CENTER
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP.toFloat())
        textView.typeface = Typeface.DEFAULT_BOLD

        // If we're running on Honeycomb or newer, then we can use the Theme's
        // selectableItemBackground to ensure that the View has a pressed state
        val outValue = TypedValue()
        getContext().theme.resolveAttribute(
            /* resid = */ android.R.attr.selectableItemBackground,
            /* outValue = */ outValue,
            /* resolveRefs = */ true
        )
        textView.setBackgroundResource(outValue.resourceId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // If we're running on ICS or newer, enable all-caps to match the Action Bar tab style
            textView.isAllCaps = true
        }
        val padding: Int = (TAB_VIEW_PADDING_DIPS * resources.displayMetrics.density).toInt()
        textView.setPadding(padding, padding, padding, padding)
        return textView
    }

    /**
     * Populates our [SlidingTabStrip] field [mTabStrip] with [TextView]s representing each of the
     * pages in the adapter of our [ViewPager] field [mViewPager]. We initialize our [PagerAdapter]
     * variable `val adapter` to the adapter of our [ViewPager] field [mViewPager], and
     * initialize [TabClickListener] variable `val tabClickListener` with a new instance of
     * [TabClickListener]. Then we loop over [Int] variable `var i` for all of the views available
     * in `adapter`:
     *
     *  1. We initialize [View] variable `var tabView` and [TextView] variable `var tabTitleView`
     *  to null
     *
     *  1. If our [Int] field [mTabViewLayoutId] is not 0, we set `tabView` to the [View] that the
     *  [LayoutInflater] from our context inflates from the layout file with resource id
     *  [mTabViewLayoutId] using [SlidingTabStrip] field [mTabStrip] for its LayoutParams without
     *  attaching to it, and we set `tabTitleView` by finding the [View] in `tabView` with id
     *  [Int] field [mTabViewTextViewId].
     *
     *  1. If `tabView` is still `null` we set it to the [View] created by our method
     *  [createDefaultTabView] for our context.
     *
     *  1. If `tabTitleView` is still `null`, and `tabView` is an instance of [TextView], we set
     *  `tabTitleView` by casting `tabView` to a [TextView].
     *
     *  1. We now set the text of `tabTitleView` to the title of the page in `adapter` at position
     *  `i`, and set its [View.OnClickListener] to `tabClickListener`.
     *
     *  1. Finally we add `tabView` to our [SlidingTabStrip] field [mTabStrip] and loop around for
     *  the next page `i`.
     */
    private fun populateTabStrip() {
        val adapter: PagerAdapter? = mViewPager!!.adapter
        val tabClickListener = TabClickListener()
        for (i in 0 until adapter!!.count) {
            var tabView: View? = null
            var tabTitleView: TextView? = null
            if (mTabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate it
                tabView = LayoutInflater.from(context).inflate(
                    /* resource = */ mTabViewLayoutId,
                    /* root = */ mTabStrip,
                    /* attachToRoot = */ false
                )
                tabTitleView = tabView.findViewById(mTabViewTextViewId)
            }
            if (tabView == null) {
                tabView = createDefaultTabView(context)
            }
            if (tabTitleView == null && TextView::class.java.isInstance(tabView)) {
                tabTitleView = tabView as TextView?
            }
            tabTitleView!!.text = adapter.getPageTitle(i)
            tabView.setOnClickListener(tabClickListener)
            mTabStrip.addView(tabView)
        }
    }

    /**
     * This is called when the view is attached to a window. First we call our super's implementation
     * of `onAttachedToWindow`, then if our [ViewPager] field [mViewPager] is not `null` we call our
     * method [scrollToTab] to scroll our sliding tabs to the tab for the currently displayed page
     * of [mViewPager].
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mViewPager != null) {
            scrollToTab(tabIndex = mViewPager!!.currentItem, positionOffset = 0)
        }
    }

    /**
     * Scrolls our sliding tab to represent the position of the currently displayed page of our
     * [ViewPager] field [mViewPager]. First we initialize [Int] variable `val tabStripChildCount`
     * with the number of children in our [SlidingTabStrip] field [mTabStrip]. If `tabStripChildCount`
     * is 0, or our [Int] parameter [tabIndex] is less than 0 or greater than `tabStripChildCount`
     * we return having done nothing. We initialize [View] variable `val selectedChild` by retrieving
     * the child of our [SlidingTabStrip] field [mTabStrip] at the position of [Int] parameter
     * [tabIndex]. If `selectedChild` is not `null` we calculate [Int] variable `var targetScrollX`
     * by adding our [Int] parameter [positionOffset] to the Left position of `selectedChild`
     * relative to its parent. If [tabIndex] is greater than 0, or [positionOffset] is greater than
     * 0 we subtract [Int] field [mTitleOffset] (Offset of the title in pixels) from `targetScrollX`.
     * Finally we call the [scrollTo] method of our super ([HorizontalScrollView]) to scroll
     * ourselves to `targetScrollX`.
     *
     * @param tabIndex index of the tab we are to scroll to.
     * @param positionOffset offset to use when the page is not centered on the screen at the moment.
     */
    private fun scrollToTab(tabIndex: Int, positionOffset: Int) {
        val tabStripChildCount: Int = mTabStrip.childCount
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return
        }
        val selectedChild: View? = mTabStrip.getChildAt(/* index = */ tabIndex)
        if (selectedChild != null) {
            var targetScrollX: Int = selectedChild.left + positionOffset
            if (tabIndex > 0 || positionOffset > 0) {
                // If we're not at the first child and are mid-scroll, make sure we obey the offset
                targetScrollX -= mTitleOffset
            }
            scrollTo(/* x = */ targetScrollX, /* y = */ 0)
        }
    }

    /**
     * Our implementation of [OnPageChangeListener] which we use as the [OnPageChangeListener]
     * for our [ViewPager] field [mViewPager].
     */
    private inner class InternalViewPagerListener : OnPageChangeListener {
        private var mScrollState = 0

        /**
         * This method will be invoked when the current page is scrolled, either as part of a
         * programmatically initiated smooth scroll or a user initiated touch scroll. We initialize
         * [Int] variable `val tabStripChildCount` to the child count of our [SlidingTabStrip] field
         * [mTabStrip]. If `tabStripChildCount` is 0, or `position` is less than 0 or greater than
         * or equal to `tabStripChildCount` we return having done nothing. Otherwise we call the
         * [SlidingTabStrip.onViewPagerPageChanged] method of our [SlidingTabStrip] field [mTabStrip]
         * to inform it of the the new [position] and [positionOffset] of the [ViewPager]. We
         * initialize [View] variable `val selectedTitle` by retrieving the child of `mTabStrip` at
         * position [position]. We calculate [Int] variable `val extraOffset` by multiplying
         * [positionOffset] by the width of `selectedTitle` (or 0 if `selectedTitle` is `null`)
         * and call our method [scrollToTab] to scroll to the tab in position [position] offset by
         * `extraOffset`. If our [OnPageChangeListener] field [mViewPagerPageChangeListener] is not
         * `null` we call its [OnPageChangeListener.onPageScrolled] method to pass on the new info.
         *
         * @param position Position index of the first page currently being displayed.
         * Page position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset Value from [0, 1) indicating the offset from the page at [position].
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val tabStripChildCount = mTabStrip.childCount
            if (tabStripChildCount == 0 || position < 0 || position >= tabStripChildCount) {
                return
            }
            mTabStrip.onViewPagerPageChanged(position, positionOffset)
            val selectedTitle: View? = mTabStrip.getChildAt(position)
            val extraOffset: Int = if (selectedTitle != null) {
                (positionOffset * selectedTitle.width).toInt()
            } else {
                0
            }
            scrollToTab(tabIndex = position, positionOffset = extraOffset)
            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener!!.onPageScrolled(
                    /* position = */ position,
                    /* positionOffset = */ positionOffset,
                    /* positionOffsetPixels = */ positionOffsetPixels
                )
            }
        }

        /**
         * Called when the scroll state changes. Useful for discovering when the user begins
         * dragging, when the pager is automatically settling to the current page, or when it
         * is fully stopped/idle. We save our [Int] parameter [state] in our [Int] field
         * [mScrollState], and if our [OnPageChangeListener] field [mViewPagerPageChangeListener]
         * is not `null` we call its [OnPageChangeListener.onPageScrollStateChanged] method to
         * pass on the new info.
         *
         * @param state The new scroll state.
         */
        override fun onPageScrollStateChanged(state: Int) {
            mScrollState = state
            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener!!.onPageScrollStateChanged(state)
            }
        }

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete. If our [Int] field [mScrollState] is equal to
         * [ViewPager.SCROLL_STATE_IDLE], we call the [SlidingTabStrip.onViewPagerPageChanged]
         * method of our [SlidingTabStrip] field [mTabStrip] to report the new position of the
         * [ViewPager], then call our method [scrollToTab] to scroll our [SlidingTabLayout]
         * custom [HorizontalScrollView] to tab [Int] parameter [position]. If our
         * [OnPageChangeListener] field [mViewPagerPageChangeListener] is not `null` we call its
         * [OnPageChangeListener.onPageSelected] method to pass on the new [position].
         *
         * @param position Position index of the new selected page.
         */
        override fun onPageSelected(position: Int) {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
                mTabStrip.onViewPagerPageChanged(position = position, positionOffset = 0f)
                scrollToTab(tabIndex = position, positionOffset = 0)
            }
            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener!!.onPageSelected(position)
            }
        }
    }

    /**
     * [TabClickListener] that we use for each of the tab views in our [SlidingTabStrip]
     */
    private inner class TabClickListener : OnClickListener {
        /**
         * Called when a view has been clicked. We loop over [Int] variable `var i` for each of the
         * children in [SlidingTabStrip] field [mTabStrip], and if our [View] parameter [v] is equal
         * to child view `i` of [mTabStrip] we call the [ViewPager.setCurrentItem] method (kotlin
         * `currentItem` property) of our [ViewPager] field [mViewPager] to set its currently
         * selected page to `i` and then return.
         *
         * @param v The [View] that was clicked.
         */
        override fun onClick(v: View) {
            for (i in 0 until mTabStrip.childCount) {
                if (v === mTabStrip.getChildAt(i)) {
                    mViewPager!!.currentItem = i
                    return
                }
            }
        }
    }

    companion object {
        /**
         * Constant specifying the offset of the title in dips.
         */
        private const val TITLE_OFFSET_DIPS = 24

        /**
         * Padding of the TextView for the tab in dips.
         */
        private const val TAB_VIEW_PADDING_DIPS = 16

        /**
         * Text size of the text drawn in the tab's TextView in sp.
         */
        private const val TAB_VIEW_TEXT_SIZE_SP = 12
    }
}