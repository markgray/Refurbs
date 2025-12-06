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

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import com.example.android.persistence.R
import com.example.android.persistence.db.entity.ProductEntity
import com.example.android.persistence.model.Product

/**
 * This sample showcases the following Architecture Components:
 *
 *  * `Room` - see [...](https://developer.android.com/topic/libraries/architecture/room.html)
 *
 *  * `ViewModels` - see https://developer.android.com/reference/android/arch/lifecycle/ViewModel.html
 *
 *  * `LiveData` - see https://developer.android.com/reference/android/arch/lifecycle/LiveData.html
 *
 * This sample contains two screens: a list of products and a detail view, that shows product reviews.
 */
class MainActivity : AppCompatActivity() {
    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge]
     * to enable edge to edge display, then we call our super's implementation
     * of `onCreate`, and set our content view to our layout file
     * `R.layout.main_activity`.
     *
     * We initialize our [FrameLayout] variable `rootView`
     * to the view with ID `R.id.fragment_container` then call
     * [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy
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
     * Then if our parameter [savedInstanceState] is `null` we initialize our [ProductListFragment]
     * variable `val fragment` with a new instance. We fetch the [FragmentManager] used for
     * interacting with fragments associated with this activity, begin a series of edit operations
     * on the Fragments associated with it, add `fragment` to the container with id
     * `R.id.fragment_container` (the [FrameLayout] in our layout file), using the TAG field of
     * [ProductListFragment] as the tag name for the fragment, and then we commit the fragment
     * transaction.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     * down (as happens for a rotation) then this [Bundle] contains the data most recently supplied
     * in [onSaveInstanceState] otherwise it is `null`. We use it only to tell whether this is our
     * first call (the [FragmentManager] uses it to restore the state of our fragments so it will
     * not be `null` if we have run once already).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val rootView = findViewById<FrameLayout>(R.id.fragment_container)
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

        // Add product list fragment if this is first creation
        if (savedInstanceState == null) {
            val fragment = ProductListFragment()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment, ProductListFragment.TAG)
                .commit()
        }
    }

    /**
     * Shows the product detail fragment, it is called from the `OnClickListener` that is
     * associated with the layout file layout/product_item.xml by the `onCreateViewHolder`
     * override in [ProductAdapter]. We initialize our [ProductFragment] variable
     * `val productFragment` with a new instance produced for the ID of our [Product] parameter
     * [product]. Then we fetch the [FragmentManager] used for interacting with fragments
     * associated with this activity, begin a series of edit operations on the Fragments associated
     * with it, add this transaction to the back stack with the name "product", replace the current
     * fragment with ID `R.id.fragment_container` with `productFragment` using `null` as the tag,
     * and then we commit the transaction.
     *
     * @param product the [ProductEntity] whose the product detail we are to display ([ProductEntity]
     * implements the [Product] interface).
     */
    fun show(product: Product) {
        val productFragment = ProductFragment.forProduct(product.getId())
        supportFragmentManager
            .beginTransaction()
            .addToBackStack("product")
            .replace(R.id.fragment_container, productFragment, null)
            .commit()
    }
}
