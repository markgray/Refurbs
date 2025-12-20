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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn", "MemberVisibilityCanBePrivate")

package com.example.android.interpolator

import android.animation.ObjectAnimator
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log
import java.util.Locale

/**
 * TODO: Continue here.
 * This sample demonstrates the use of animation interpolators and path animations for Material
 * Design. It shows how an [android.animation.ObjectAnimator] is used to animate two properties of a
 * view (scale X and Y) along a path.
 */
class InterpolatorFragment
/**
 * Required empty public constructor
 */
    : Fragment() {
    /**
     * View that is animated.
     */
    private var mView: View? = null

    /**
     * Spinner for selection of interpolator.
     */
    private var mInterpolatorSpinner: Spinner? = null

    /**
     * SeekBar for selection of duration of animation.
     */
    private var mDurationSeekbar: SeekBar? = null

    /**
     * TextView that shows animation selected in SeekBar.
     */
    private var mDurationLabel: TextView? = null

    /**
     * The array of loaded Interpolators available for animation in this Fragment
     */
    lateinit var interpolators: Array<Interpolator>
        private set

    /**
     * [Path] for in (shrinking) animation, from 100% scale to 20%.
     */
    var pathIn: Path? = null
        private set

    /**
     * Path for out (growing) animation, from 20% to 100%.
     */
    var pathOut: Path? = null
        private set

    /**
     * Set to `true` if [View] is animated out (is shrunk).
     */
    private var mIsOut = false

    /**
     * Names of the available interpolators.
     */
    private lateinit var mInterpolatorNames: Array<String>

    /**
     * Called to do initial creation of a fragment. First we call our super's implementation of
     * `onCreate`. We call our method [initInterpolators] to initialize our [Array] of [Interpolator]
     * field [interpolators] with interpolators built from system resource ids. We then initialize
     * our [Array] of [String] field [mInterpolatorNames] by reading the string array with resource
     * id `R.array.interpolator_names` ("Linear", "Fast Out Linear In", "Fast Out Slow In", and
     * "Linear Out Slow In"). Finally we call our method [initPaths] to initialize the paths
     * that are used by the [ObjectAnimator] to scale the view ([Path] field [pathIn], and [Path]
     * field [pathOut]).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initInterpolators()
        mInterpolatorNames = resources.getStringArray(R.array.interpolator_names)
        initPaths()
    }

    /**
     * Called to have the fragment instantiate its user interface view. We return the view that our
     * [LayoutInflater] parameter [inflater] inflates from our layout file
     * `R.layout.interpolator_fragment`, using our [ViewGroup] parameter [container] for the
     * LayoutParams without attaching to it.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or `null`.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.interpolator_fragment, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. We call our method [initAnimateButton] to set up the "Animate!"
     * button (when it is clicked the view is animated with the options selected: the [Interpolator],
     * duration and animation path). We initialize our [TextView] field [mDurationLabel] by finding
     * the view with id `R.id.durationLabel`, and initialize [Spinner] field [mInterpolatorSpinner]
     * by finding the view with id `R.id.interpolatorSpinner`. We initialize [ArrayAdapter] of
     * [String] variable `val spinnerAdapter` with a new instance that uses
     * [android.R.layout.simple_spinner_dropdown_item] as the layout file to use when instantiating
     * views, and [Array] of [String] field [mInterpolatorNames] as the objects to represent. We
     * then set the adapter of [mInterpolatorSpinner] to `spinnerAdapter`. We call our method
     * [initSeekbar] to set up the [SeekBar] that defines the duration of the animation, then
     * initialize our [View] field [mView] by finding the view in [View] parameter [view] with id
     * `R.id.square`. Finally we call our super's implementation of `onViewCreated`.
     *
     * @param view The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initAnimateButton(view)

        // Get the label to display the selected duration
        mDurationLabel = view.findViewById(R.id.durationLabel)

        // Set up the Spinner with the names of interpolators.
        mInterpolatorSpinner = view.findViewById(R.id.interpolatorSpinner)
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_dropdown_item,
            mInterpolatorNames
        )
        mInterpolatorSpinner!!.adapter = spinnerAdapter
        initSeekbar(view)

        // Get the view that will be animated
        mView = view.findViewById(R.id.square)
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Set up the "Animate!" button, when it is clicked the view is animated with the options
     * selected: the [Interpolator], duration and animation path. First we initialize [View]
     * variable `val button` by finding the view in our [View] parameter [view] with id
     * `R.id.animateButton` ("Animate!"), and then we set its [OnClickListener] to an anonymous
     * class which starts the animation selected running when its [OnClickListener.onClick]
     * override is called.
     *
     * @param view The [View] holding the button.
     */
    private fun initAnimateButton(view: View) {
        val button = view.findViewById<View>(R.id.animateButton)
        button.setOnClickListener { // Interpolator selected in the spinner
            val selectedItemPosition = mInterpolatorSpinner!!.selectedItemPosition
            val interpolator = interpolators[selectedItemPosition]
            // Duration selected in SeekBar
            val duration = mDurationSeekbar!!.progress.toLong()
            // Animation path is based on whether animating in or out
            val path: Path = if (mIsOut) pathIn!! else pathOut!!

            // Log animation details
            Log.i(
                TAG, String.format(
                    Locale.getDefault(),
                    "Starting animation: [%d ms, %s, %s]",
                    duration,
                    mInterpolatorSpinner!!.selectedItem,
                    if (mIsOut) "Out (growing)" else "In (shrinking)"
                )
            )

            // Start the animation with the selected options
            startAnimation(interpolator, duration, path)

            // Toggle direction of animation (path)
            mIsOut = !mIsOut
        }
    }

    /**
     * Set up SeekBar that defines the duration of the animation. First we initialize our [SeekBar]
     * field [mDurationSeekbar] by finding the view in our [View] parameter [view] with id
     * `R.id.durationSeek`, then we set its [OnSeekBarChangeListener] to an anonymous class whose
     * [OnSeekBarChangeListener.onProgressChanged] override sets the text of our [TextView] field
     * [mDurationLabel] to the string containing the value of the current progress level formatted
     * using the format string whose resource id is `R.string.animation_duration` ("Duration: %1$d ms").
     * Finally we set the progress of [mDurationSeekbar] to [INITIAL_DURATION_MS] (750).
     *
     * @param view The [View] holding the button.
     */
    private fun initSeekbar(view: View) {
        mDurationSeekbar = view.findViewById(R.id.durationSeek)

        // Register listener to update the text label when the SeekBar value is updated
        mDurationSeekbar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            /**
             * Notification that the progress level has changed. We set the text of our [TextView]
             * field [mDurationLabel] to the string containing the value of the current progress
             * level formatted using the format string whose resource id is
             * `R.string.animation_duration` ("Duration: %1$d ms").
             *
             * @param seekBar The [SeekBar] whose progress has changed
             * @param i       The current progress level.
             * @param b       `true` if the progress change was initiated by the user.
             */
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                mDurationLabel!!.text = resources.getString(R.string.animation_duration, i)
            }

            /**
             * Notification that the user has started a touch gesture. We ignore
             *
             * @param seekBar The [SeekBar] in which the touch gesture began
             */
            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            /**
             * Notification that the user has finished a touch gesture. We ignore.
             *
             * @param seekBar The [SeekBar] in which the touch gesture began
             */
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Set initial progress to trigger SeekBarChangeListener and update UI
        mDurationSeekbar!!.progress = INITIAL_DURATION_MS
    }

    /**
     * Start an animation on the sample view. The view is animated using an [ObjectAnimator] on the
     * [View.SCALE_X] and [View.SCALE_Y] properties, with its animation based on a path. The only
     * two paths defined here ([pathIn] and [pathOut]) scale the view uniformly.
     *
     * We initialize [ObjectAnimator] variable `val animator` with an [ObjectAnimator] that animates
     * the [View.SCALE_X] and [View.SCALE_Y] coordinates of our [View] field [mView] along the [Path]
     * parameter [path]. We then set the duration of `animator` to our [Long] parameter [duration]
     * and its interpolator to [Interpolator] parameter [interpolator]. We then start `animator`
     * running and return it to the caller.
     *
     * @param interpolator The interpolator to use for the animation.
     * @param duration Duration of the animation in ms.
     * @param path Path of the animation
     * @return The ObjectAnimator used for this animation
     * @see android.animation.ObjectAnimator.ofFloat
     */
    fun startAnimation(interpolator: Interpolator?, duration: Long, path: Path?): ObjectAnimator {
        // This ObjectAnimator uses the path to change the x and y scale of the mView object.
        val animator = ObjectAnimator.ofFloat(mView, View.SCALE_X, View.SCALE_Y, path)

        // Set the duration and interpolator for this animation
        animator.duration = duration
        animator.interpolator = interpolator
        animator.start()
        return animator
    }

    /**
     * Initialize [Array] of [Interpolator] field [interpolators] programmatically by loading them
     * from the XML definitions provided by the framework.
     */
    private fun initInterpolators() {
        interpolators = arrayOf(
            AnimationUtils.loadInterpolator(activity, android.R.interpolator.linear),
            AnimationUtils.loadInterpolator(activity, android.R.interpolator.fast_out_linear_in),
            AnimationUtils.loadInterpolator(activity, android.R.interpolator.fast_out_slow_in),
            AnimationUtils.loadInterpolator(activity, android.R.interpolator.linear_out_slow_in)
        )
    }

    /**
     * Initializes the paths that are used by the [ObjectAnimator] to scale the view. We initialize
     * our [Path] field [pathIn] with a new instance. We then position it at (0.2,0.2) then have it
     * move to (1,1). We initialize our [Path] field [pathOut] with a new instance. We then position
     * it at (1,1) then have it move to (0.2,0,2).
     */
    private fun initPaths() {
        // Path for 'in' animation: growing from 20% to 100%
        pathIn = Path()
        pathIn!!.moveTo(0.2f, 0.2f)
        pathIn!!.lineTo(1f, 1f)

        // Path for 'out' animation: shrinking from 100% to 20%
        pathOut = Path()
        pathOut!!.moveTo(1f, 1f)
        pathOut!!.lineTo(0.2f, 0.2f)
    }

    companion object {
        /**
         * Default duration of animation in ms.
         */
        private const val INITIAL_DURATION_MS = 750

        /**
         * String used for logging.
         */
        const val TAG: String = "InterpolatorPlaygroundFragment"
    }
}
