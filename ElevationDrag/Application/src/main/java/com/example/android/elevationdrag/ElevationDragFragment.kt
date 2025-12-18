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
package com.example.android.elevationdrag

import android.graphics.Outline
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log
import java.util.Locale

/**
 * [Fragment] containing a [DragFrameLayout] in its layout file layout/ztranslation.xml
 */
class ElevationDragFragment : Fragment() {
    /**
     * The circular outline provider.
     */
    private var mOutlineProviderCircle: ViewOutlineProvider? = null

    /**
     * The current elevation of the floating view.
     */
    private var mElevation = 0f

    /**
     * The step in elevation when changing the Z value
     */
    private var mElevationStep = 0

    /**
     * Called to do initial creation of a fragment. This is called after [onAttach] and before
     * [onCreateView]. Note that this can be called while the fragment's activity is still in the
     * process of being created. As such, you can not rely on things like the activity's content
     * view hierarchy being initialized at this point. If you want to do work once the activity
     * itself is created, add a [androidx.lifecycle.LifecycleObserver] on the activity's Lifecycle,
     * removing it when it receives the `Lifecycle.State.CREATED` callback.
     *
     * First we call our super's implementation of `onCreate`. Then we initialize our
     * [ViewOutlineProvider] field [mOutlineProviderCircle] with a new instance. Finally we
     * initialize [mElevationStep] with the raw pixel size that a Resources instance for the
     * application's package resolves for the resource with id `R.dimen.elevation_step` (8dp) .
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mOutlineProviderCircle = CircleOutlineProvider()
        mElevationStep = resources.getDimensionPixelSize(R.dimen.elevation_step)
    }

    /**
     * Called to have the fragment instantiate its user interface view. We initialize [View] variable
     * `val rootView` by using our [LayoutInflater] parameter [inflater] to inflate our layout file
     * `R.layout.ztranslation` using our [ViewGroup] parameter [container] for the LayoutParams
     * without attaching to it. We initialize [View] variable `val floatingShape` by finding the
     * view in `rootView` with id `R.id.circle`, set its [ViewOutlineProvider] to our
     * [ViewOutlineProvider] field [mOutlineProviderCircle] (generates the Outline that defines the
     * shape of the shadow it casts, and enables outline clipping), and call its [View.setClipToOutline]
     * method with `true` to indicate that the [View]'s Outline should be used to clip the contents
     * of the [View].
     *
     * We initialize [DragFrameLayout] variable `val dragLayout` by finding the view in `rootView`
     * with id `R.id.main_layout` and set its [DragFrameLayout.DragFrameLayoutController] to an
     * anonymous class whose [DragFrameLayout.DragFrameLayoutController.onDragDrop] override
     * animates the [View.setTranslationZ] (aka kotlin `translationZ` property) of `floatingShape`
     * depending on whether its `captured` parameter is `true` (view is raised by 50 pixels while
     * captured), or `false` (`translationZ` is returned to 0 when dropped). We then add
     * `floatingShape` to the list of views that are draggable within the container `dragLayout`.
     *
     * We set the [OnClickListener] of the view in `rootView` with id `R.id.raise_bt` ("Z+")
     * to an anonymous class whose [OnClickListener.onClick] override adds [mElevationStep] to
     * [mElevation], logs the new elevation, and sets the base elevation of `floatingShape` to
     * [mElevation], and we set the [OnClickListener] of the view in `rootView` with id `R.id.lower_bt`
     * ("Z-") to an anonymous class whose [OnClickListener.onClick] override subtracts [mElevationStep]
     * from [mElevation] (setting to 0 if the result is less than 0), logs the new elevation, and sets
     * the base elevation of `floatingShape` to [mElevation].
     *
     * Finally we return `rootView` to the caller.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(
            /* resource = */ R.layout.ztranslation,
            /* root = */ container,
            /* attachToRoot = */ false
        )

        /**
         * The [View] to apply z-translation to
         */
        val floatingShape = rootView.findViewById<View>(R.id.circle)

        /* Define the shape of the `View`'s shadow by setting one of the `Outline`'s. */
        floatingShape.outlineProvider = mOutlineProviderCircle

        /* Clip the `View` with its outline. */
        floatingShape.clipToOutline = true
        val dragLayout = rootView.findViewById<DragFrameLayout>(R.id.main_layout)
        dragLayout.setDragFrameController(object : DragFrameLayout.DragFrameLayoutController {
            /**
             * Animate the translation of the [View]. Note that the translation
             * is being modified, not the elevation.
             */
            override fun onDragDrop(captured: Boolean) {
                    floatingShape
                        .animate()
                        .translationZ((if (captured) 50 else 0).toFloat())
                        .duration = 100
                    Log.d(TAG, if (captured) "Drag" else "Drop")
            }

        })
        dragLayout.addDragView(floatingShape)

        /* Raise the circle in z when the "z+" button is clicked. */
        rootView.findViewById<View>(R.id.raise_bt).setOnClickListener {
            mElevation += mElevationStep.toFloat()
            Log.d(TAG, String.format(Locale.US, "Elevation: %.1f", mElevation))
            floatingShape.elevation = mElevation
        }

        /* Lower the circle in z when the "z-" button is clicked. */
        rootView.findViewById<View>(R.id.lower_bt).setOnClickListener {
            mElevation -= mElevationStep.toFloat()
            // Don't allow for negative values of Z.
            if (mElevation < 0) {
                mElevation = 0f
            }
            Log.d(TAG, String.format(Locale.US, "Elevation: %.1f", mElevation))
            floatingShape.elevation = mElevation
        }
        return rootView
    }

    /**
     * [ViewOutlineProvider] which sets the outline to be an oval which fits the [View] bounds.
     */
    private class CircleOutlineProvider : ViewOutlineProvider() {
        /**
         * Called to get the provider to populate the [Outline]. This method will be called by a [View]
         * when its owned Drawables are invalidated, when the [View]'s size changes, or if
         * [View.invalidateOutline] is called explicitly. The input outline is empty and has
         * an alpha of `1.0f`. We call the [Outline.setOval] method of our [Outline] parameter
         * [outline] to set the outline to the oval defined by input rect given by a left side at 0,
         * a top at 0, the width of our [View] parameter [view] as the right side, and the height of
         * our [View] parameter [view] as the bottom side.
         *
         * @param view The view building the outline.
         * @param outline The empty outline to be populated.
         */
        override fun getOutline(view: View, outline: Outline) {
            outline.setOval(0, 0, view.width, view.height)
        }
    }

    companion object {
        /**
         * TAG used for logging
         */
        const val TAG: String = "ElevationDragFragment"
    }
}
