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

import com.example.android.persistence.model.Product

/**
 * Interface that we use to handle clicks in our layout file layout/product_item.xml (our RecyclerView
 * item view), it is set by setting the variable "callback" in that file, and that variable is used
 * by the attribute android:onClick in a lambda that calls the method `onClick` with the bound
 * variable "product".
 */
open class ProductClickCallback {
    /**
     * This method is used by the attribute android:onClick in the file layout/product_item.xml in a
     * lambda that calls us with the bound variable "product".
     *
     * @param product the `Product` item that has been clicked
     */
    open fun onClick(product: Product?) { }
}