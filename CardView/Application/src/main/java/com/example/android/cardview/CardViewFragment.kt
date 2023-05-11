/*
* Copyright 2014 The Android Open Source Project
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
@file:Suppress("RedundantOverride", "ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.cardview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

/**
 * Fragment that demonstrates how to use CardView.
 */
class CardViewFragment
/**
 * Our constructor, nothing to do.
 */
    : Fragment() {
    /** The CardView widget.  */ //@VisibleForTesting
    @JvmField
    var mCardView: CardView? = null

    /**
     * SeekBar that changes the cornerRadius attribute for the [.mCardView] widget.
     */
    //@VisibleForTesting
    @JvmField
    var mRadiusSeekBar: SeekBar? = null

    /**
     * SeekBar that changes the Elevation attribute for the [.mCardView] widget.
     */
    //@VisibleForTesting
    @JvmField
    var mElevationSeekBar: SeekBar? = null

    /**
     * Called to do initial creation of a fragment. We just call through to our super's implementation
     * of `onCreate`.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Called to have the fragment instantiate its user interface view. We use our parameter
     * `LayoutInflater inflater` to inflate our layout file R.layout.fragment_card_view using
     * our parameter `ViewGroup container` for the LayoutParams without attaching to it, and
     * return the view created to our caller.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_card_view, container, false)
    }

    /**
     * Called immediately after [.onCreateView]
     * has returned, but before any saved state has been restored in to the view. First we call our
     * super's implementation of `onViewCreated`. We initialize our field `CardView mCardView`
     * by finding the view in `view` with ID R.id.cardview, and initialize our field
     * `SeekBar mRadiusSeekBar` by finding the view in `view` with ID R.id.cardview_radius_seekbar.
     * We then set the `OnSeekBarChangeListener` of `mRadiusSeekBar` to an anonymous class
     * whose `onProgressChanged` override updates the corner radius of the CardView `mCardView`
     * with the current progress level of `mRadiusSeekBar` (0-100). We then initialize our field
     * `SeekBar mElevationSeekBar` by finding the view in `view` with id R.id.cardview_elevation_seekbar
     * and set its `OnSeekBarChangeListener` to an anonymous class whose `onProgressChanged`
     * override sets the base elevation of `mCardView` to the current progress level of `mElevationSeekBar`
     * (0-100).
     *
     * @param view The View returned by [.onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mCardView = view.findViewById(R.id.cardview)
        mRadiusSeekBar = view.findViewById(R.id.cardview_radius_seekbar)
        mRadiusSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            /**
             * Notification that the progress level has changed. We log the value of our parameter
             * `progress` then set the corner radius of the CardView `mCardView` to
             * `progress`.
             *
             * @param seekBar The SeekBar whose progress has changed
             * @param progress The current progress level. This will be in the range min..max
             * (The default values for min is 0 and max is 100.)
             * @param fromUser True if the progress change was initiated by the user.
             */
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d(TAG, String.format("SeekBar Radius progress : %d", progress))
                mCardView!!.radius = progress.toFloat()
            }

            /**
             * Notification that the user has started a touch gesture. Clients may want to use this
             * to disable advancing the seekbar. We do nothing.
             *
             * @param seekBar The SeekBar in which the touch gesture began
             */
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }

            /**
             * Notification that the user has finished a touch gesture. Clients may want to use this
             * to re-enable advancing the seekbar. We do nothing.
             *
             * @param seekBar The SeekBar in which the touch gesture began
             */
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }
        })
        mElevationSeekBar = view.findViewById(R.id.cardview_elevation_seekbar)
        mElevationSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            /**
             * Notification that the progress level has changed. We log the value of our parameter
             * `progress` then set the base elevation of the CardView `mCardView` to
             * `progress`.
             *
             * @param seekBar The SeekBar whose progress has changed
             * @param progress The current progress level. This will be in the range min..max
             * (The default values for min is 0 and max is 100.)
             * @param fromUser True if the progress change was initiated by the user.
             */
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d(TAG, String.format("SeekBar Elevation progress : %d", progress))
                mCardView!!.elevation = progress.toFloat()
            }

            /**
             * Notification that the user has started a touch gesture. Clients may want to use this
             * to disable advancing the seekbar. We do nothing.
             *
             * @param seekBar The SeekBar in which the touch gesture began
             */
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }

            /**
             * Notification that the user has finished a touch gesture. Clients may want to use this
             * to re-enable advancing the seekbar. We do nothing.
             *
             * @param seekBar The SeekBar in which the touch gesture began
             */
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }
        })
    }

    companion object {
        /**
         * TAG used for logging
         */
        private val TAG = CardViewFragment::class.java.simpleName

        /**
         * Use this factory method to create a new instance of this fragment using the provided parameters.
         * We initialize `CardViewFragment fragment` with a new instance, configure it to be retained
         * across Activity re-creation (such as from a configuration change), and return `fragment`
         * to the caller.
         *
         * @return A new instance of fragment NotificationFragment.
         */
        fun newInstance(): CardViewFragment {
            val fragment = CardViewFragment()
            @Suppress("DEPRECATION")
            fragment.retainInstance = true
            return fragment
        }
    }
}