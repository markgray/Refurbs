/*
 * Copyright 2014, The Android Open Source Project
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

package com.example.android.floatingactionbuttonbasic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log
import com.example.android.floatingactionbuttonbasic.FloatingActionButton.OnCheckedChangeListener

/**
 * This [Fragment] inflates a layout with two Floating Action Buttons and acts as a listener to
 * changes on them.
 */
class FloatingActionButtonBasicFragment : Fragment(), OnCheckedChangeListener {
    /**
     * Called to have the [Fragment] instantiate its user interface view. We initialize [View]
     * variable `val rootView` to the [View] that our [LayoutInflater] parameter [inflater] inflates
     * from our layout file `R.layout.fab_layout` using our [ViewGroup] parameter [container] for
     * LayoutParams without attaching to it. We initialize [FloatingActionButton] variable `val fab1`
     * to the view with id `R.id.fab_1` and set its [OnCheckedChangeListener] to this, and we
     * initialize [FloatingActionButton] variable `val fab2` by finding the view in `rootView` with
     * id `R.id.fab_2` and set its [OnCheckedChangeListener] to this. Finally we return `rootView`
     * to the caller.
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
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(
            /* resource = */ R.layout.fab_layout,
            /* root = */ container,
            /* attachToRoot = */ false
        )

        // Make this {@link Fragment} listen for changes in both FABs.
        val fab1 = rootView.findViewById<FloatingActionButton>(R.id.fab_1)
        fab1.setOnCheckedChangeListener(this)
        val fab2 = rootView.findViewById<FloatingActionButton>(R.id.fab_2)
        fab2.setOnCheckedChangeListener(this)
        return rootView
    }

    /**
     * Called when the checked state of a FAB has changed. We switch on the id of our
     * [FloatingActionButton] parameter [fabView]:
     *
     *  * `R.id.fab_1`: We log the formatted string "FAB 1 was checked" if our [Boolean] parameter
     *  [isChecked] is `true`, or the string "FAB 1 was unchecked" is our parameter [isChecked] is
     *  false.
     *
     *  * `R.id.fab_2`: We log the formatted string "FAB 2 was checked" is our [Boolean] parameter
     *  [isChecked] is `true`, or the string "FAB 2 was unchecked" is our parameter [isChecked] is
     *  false.
     *
     * @param fabView The [FloatingActionButton] view whose state has changed.
     * @param isChecked The new checked state of [fabView].
     */
    override fun onCheckedChanged(fabView: FloatingActionButton?, isChecked: Boolean) {
        // When a FAB is toggled, log the action.
        when (fabView!!.id) {
            R.id.fab_1 -> Log.d(TAG, String.format("FAB 1 was %s.", if (isChecked) "checked" else "unchecked"))
            R.id.fab_2 -> Log.d(TAG, String.format("FAB 2 was %s.", if (isChecked) "checked" else "unchecked"))
            else -> {}
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "FloatingActionButtonBasicFragment"
    }
}
