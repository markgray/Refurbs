package com.example.android.basicsyncadapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity for holding EntryListFragment.
 */
class EntryListActivity : AppCompatActivity() {
    /**
     * Called when the activity is starting. First we call through to our super's implementation of
     * `onCreate`, then we set our content view to our layout file R.layout.activity_entry_list.
     *
     * @param savedInstanceState we do not override `onSaveInstanceState` so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_list)
    }
}