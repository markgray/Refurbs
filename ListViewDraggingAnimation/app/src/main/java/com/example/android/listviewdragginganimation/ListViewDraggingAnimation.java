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

package com.example.android.listviewdragginganimation;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;

/**
 * This application creates a {@code ListView} where the ordering of the data set
 * can be modified in response to user touch events.
 *
 * An item in the {@code ListView} is selected via a long press event and is then
 * moved around by tracking and following the movement of the user's finger.
 * When the item is released, it animates to its new position within the {@code ListView}.
 * See: https://www.youtube.com/watch?v=_BZIvjMgH-Q
 */
public class ListViewDraggingAnimation extends Activity {

    /**
     * Called when the activity is starting. First we call our super's implementation of {@code onCreate},
     * then we set our content view to our layout file R.layout.activity_list_view. We initialize
     * {@code ArrayList<String> mCheeseList} with a new instance, then add all of the strings in the
     * array {@code Cheeses.sCheeseStrings} to it. We initialize {@code StableArrayAdapter adapter} with
     * a new instance which will use {@code mCheeseList} as its dataset and the layout file R.layout.text_view
     * to display the data items in the {@code ListView} it is the adapter for. We initialize
     * {@code DynamicListView listView} by finding the view with id R.id.list_view, call its {@code setCheeseList}
     * method to set its dataset to {@code mCheeseList}, set its adapter to {@code adapter} and set its
     * choice mode to CHOICE_MODE_SINGLE.
     *
     * @param savedInstanceState we do not override {@code onSaveInstanceState} so do not use.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        ArrayList<String> mCheeseList = new ArrayList<>();
        Collections.addAll(mCheeseList, Cheeses.sCheeseStrings);

        StableArrayAdapter adapter = new StableArrayAdapter(this, R.layout.text_view, mCheeseList);
        DynamicListView listView = findViewById(R.id.list_view);

        listView.setCheeseList(mCheeseList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }
}
