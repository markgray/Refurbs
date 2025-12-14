/*
 * Copyright 2017, The Android Open Source Project
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
package com.example.android.persistence.ui

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import com.example.android.persistence.databinding.ListFragmentBinding
import com.example.android.persistence.databinding.ProductFragmentBinding

/**
 * This class contains the binding adapter for the attribute app:visibleGone, it is used by both
 * layout/product_fragment.xml, and layout/list_fragment.xml. It is accessed by using
 * [DataBindingUtil.inflate] to create either a [ProductFragmentBinding] or a
 * [ListFragmentBinding] and then using the `setIsLoading` method of that binding to
 * set it to true or false.
 */
object BindingAdapters {
    /**
     * This is the binding adapter for the attribute app:visibleGone
     *
     * @param view [View] whose visibility we are to control
     * @param show `true` to make the visibility VISIBLE, `false` to make it GONE. It is bound
     * during inflation to the value of the variable `isLoading` in the xml file.
     */
    @JvmStatic
    @BindingAdapter("visibleGone")
    fun showHide(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }
}