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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * The dynamic ListView is an extension of ListView that supports cell dragging
 * and swapping.
 *
 * This layout is in charge of positioning the hover cell in the correct location
 * on the screen in response to user touch events. It uses the position of the
 * hover cell to determine when two cells should be swapped. If two cells should
 * be swapped, all the corresponding data set and layout changes are handled here.
 *
 * If no cell is selected, all the touch events are passed down to the ListView
 * and behave normally. If one of the items in the ListView experiences a
 * long press event, the contents of its current visible state are captured as
 * a bitmap and its visibility is set to INVISIBLE. A hover cell is then created and
 * added to this layout as an overlaying BitmapDrawable above the ListView. Once the
 * hover cell is translated some distance to signify an item swap, a data set change
 * accompanied by animation takes place. When the user releases the hover cell,
 * it animates into its corresponding position in the ListView.
 *
 * When the hover cell is either above or below the bounds of the ListView, this
 * ListView also scrolls on its own so as to reveal additional content.
 */
@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
public class DynamicListView extends ListView {

    final static String TAG = "DynamicListView";
    /**
     * DP amount to scroll by, it is divided by the density of the screen in order to determine how
     * many pixels to smoothly scroll the {@code ListView} when the item being dragged has been
     * dragged above or below the bounds of the {@code ListView}.
     */
    private final int SMOOTH_SCROLL_AMOUNT_AT_EDGE = 15;
    /**
     * Duration of the animation of item views which are moved when the view being dragged passes
     * over them.
     */
    private final int MOVE_DURATION = 150;
    /**
     * Stroke width of the {@code Paint} used by the {@code getBitmapWithBorder} method to draw
     * a black border over the screenshot of the view passed in
     */
    private final int LINE_THICKNESS = 15;

    /**
     * Our dataset of cheese names.
     */
    public ArrayList<String> mCheeseList;

    /**
     * Y coordinate of the current ACTION_MOVE event being processed by our {@code onTouchEvent}
     * override.
     */
    private int mLastEventY = -1;

    /**
     * Y coordinate of the ACTION_DOWN event which initiated the current item drag, it is also
     * updated to {@code mLastEventY} whenever the drag causes a item cell to switch positions
     * in our {@code handleCellSwitch} method.
     */
    private int mDownY = -1;
    /**
     * X coordinate of the ACTION_DOWN event which initiated the current item drag, it is used in
     * a call to the {@code pointToPosition} method to find the position of the item which is
     * long clicked in our {@code onItemLongClick} override.
     */
    private int mDownX = -1;

    /**
     * Total amount the dragged cell in the Y direction from its initial position on the screen.
     */
    private int mTotalOffset = 0;

    /**
     * Flag to indicate that a cell has been long clicked and is now being dragged somewhere.
     */
    private boolean mCellIsMobile = false;
    /**
     * Flag indicating that this {@code ListView} is in a scrolling state invoked by the fact that
     * the hover cell is out of the bounds of the {@code ListView}, it is set to the value returned
     * by {@code handleMobileCellScroll} method for the value in {@code Rect mHoverCellCurrentBounds}
     * by our zero argument version of {@code handleMobileCellScroll}, and set to false whenever the
     * current touch event suggests the scrolling state should end.
     */
    private boolean mIsMobileScrolling = false;
    /**
     * Number of pixels to smooth scroll the {@code ListView} when the hover item is at the edge of
     * the {@code ListView}, it is calculated by dividing SMOOTH_SCROLL_AMOUNT_AT_EDGE (15) by the
     * logical density of the display.
     */
    private int mSmoothScrollAmountAtEdge = 0;

    /**
     * Value to indicate that the id of the item in question is invalid.
     */
    private final int INVALID_ID = -1;
    /**
     * ID of the item that is currently above the hover cell being dragged.
     */
    private long mAboveItemId = INVALID_ID;
    /**
     * ID of the item that is currently being dragged.
     */
    private long mMobileItemId = INVALID_ID;
    /**
     * ID of the item that is currently below the hover cell being dragged.
     */
    private long mBelowItemId = INVALID_ID;

    /**
     * {@code BitmapDrawable} created from the cell that has been long clicked, it is used as the
     * view that the user drags, the original view remains in the {@code ListView} but is invisible
     * until the drag ends.
     */
    private BitmapDrawable mHoverCell;
    /**
     * Current location of the hover cell on the screen as it is being dragged.
     */
    private Rect mHoverCellCurrentBounds;
    /**
     * Original location of the hover cell on the screen when it is first long clicked.
     */
    private Rect mHoverCellOriginalBounds;

    /**
     * Value to indicate that a point id is not valid.
     */
    private final int INVALID_POINTER_ID = -1;
    /**
     * Pointer identifier associated with the pointer data index 0 of the ACTION_DOWN event captured
     * by our {@code onTouchEvent} override, it is used to determine is the ACTION_POINTER_UP event
     * later received is for the same 'touch' of a multi-touch event (apparently).
     */
    private int mActivePointerId = INVALID_POINTER_ID;

    /**
     * Flag indicating that the {@code ListView} is currently scrolling
     */
    private boolean mIsWaitingForScrollFinish = false;
    /**
     * Current scroll state as received by the {@code onScrollStateChanged} override of our
     * {@code OnScrollListener mScrollListener}
     */
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    /**
     * Our one argument constructor. First we call our super's constructor, then we call our method
     * {@code init} to initialize this instance. UNUSED.
     *
     * @param context The Context the view is running in, through which it can access the current
     *                theme, resources, etc.
     */
    public DynamicListView(Context context) {
        super(context);
        init(context);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource. First we call our super's constructor, then we call our method {@code init}
     * to initialize this instance. UNUSED.
     *
     * @param context  The Context the view is running in, through which it can access the current
     *                 theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style resource
     *                 that supplies default values for the view.
     */
    public DynamicListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * Perform inflation from XML. First we call our super's constructor, then we call our method
     * {@code init} to initialize this instance. This is the constructor that is used.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public DynamicListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Called by our constructors to initialize this instance. First we set our {@code OnItemLongClickListener}
     * to our field {@code OnItemLongClickListener mOnItemLongClickListener}, then we set our
     * {@code OnScrollListener} to our field {@code OnScrollListener mScrollListener}. We initialize
     * {@code DisplayMetrics metrics} with the current display metrics that are in effect for a
     * {@code Resources} instance for the application's package. We then initialize our field
     * {@code int mSmoothScrollAmountAtEdge} by dividing SMOOTH_SCROLL_AMOUNT_AT_EDGE by the {@code density}
     * field of {@code metrics} (the {@code density} field is the logical density of the display, and
     * we use it here to scale the DIP value in SMOOTH_SCROLL_AMOUNT_AT_EDGE to pixels).
     *
     * @param context The Context the view is running in, through which it can access the current
     *                theme, resources, etc.
     */
    public void init(Context context) {
        setOnItemLongClickListener(mOnItemLongClickListener);
        setOnScrollListener(mScrollListener);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mSmoothScrollAmountAtEdge = (int)(SMOOTH_SCROLL_AMOUNT_AT_EDGE / metrics.density);
    }

    /**
     * Listens for long clicks on any items in the ListView. When a cell has been selected, the hover
     * cell is created and set up.
     */
    private AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        /**
         * Callback method to be invoked when an item in this view has been clicked and held. First
         * we initialize our field {@code int mTotalOffset} to 0. We initialize {@code int position}
         * to the position of the item which contains the point (mDownX, mDownY) (this is the coordinates
         * of the {@code onTouch} event which initiated this long click and {@code position} should
         * be the same as our parameter {@code pos} since any scrolling of the {@code ListView} eats
         * the long click events until the user lifts his finger, then long clicks with the scrolling
         * stopped, but might as well make sure I guess). We then initialize {@code int itemNum} by
         * subtracting the position number of the first visible item on the screen from {@code position}.
         * We initialize {@code View selectedView} by fetching our {@code itemNum}'th child, initialize
         * our field {@code mMobileItemId} with the item id of the item at {@code position} in our dataset,
         * and initialize our field {@code BitmapDrawable mHoverCell} with the {@code BitmapDrawable}
         * that our method {@code getAndAddHoverView} creates from {@code selectedView} (this is the
         * {@code dispatchDraw} override draw on the screen when it is called). We then set the visibility
         * of {@code selectedView} to INVISIBLE (note that then need to set it to VISIBLE again before
         * it is recycled by our adapter). We not set our field {@code mCellIsMobile} and call our method
         * {@code updateNeighborViewsForID} to store a reference to the views above and below the item
         * currently corresponding to the hover cell in our fields {@code mAboveItemId} and {@code mBelowItemId}.
         * Finally we return true to consume the long click.
         *
         * @param arg0 The AbsListView where the click happened
         * @param arg1 The view within the AbsListView that was clicked
         * @param pos  The position of the view in the list
         * @param id   The row id of the item that was clicked
         *
         * @return true if the callback consumed the long click, false otherwise
         */
        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
            mTotalOffset = 0;

            int position = pointToPosition(mDownX, mDownY);
            int itemNum = position - getFirstVisiblePosition();

            View selectedView = getChildAt(itemNum);
            mMobileItemId = getAdapter().getItemId(position);
            mHoverCell = getAndAddHoverView(selectedView);
            selectedView.setVisibility(INVISIBLE);

            mCellIsMobile = true;

            updateNeighborViewsForID(mMobileItemId);

            return true;
        }
    };

    /**
     * Creates the hover cell with the appropriate bitmap and of appropriate size. The hover cell's
     * {@code BitmapDrawable} is drawn on top of the {@code ListView} every single time an invalidate
     * call is made. We initialize {@code int w} with the width of our parameter {@code View v}, and
     * {@code int h} with its height. We initialize {@code int top} with the top Y coordinate of {@code v}
     * relative to its parent, and {@code int left} with the left X coordinate (both in pixels). We
     * initialize {@code Bitmap b} with the black line bordered {@code Bitmap} version of {@code v}
     * returned by our method {@code getBitmapWithBorder}. We create {@code BitmapDrawable drawable}
     * from {@code b}. We initialize our field {@code Rect mHoverCellOriginalBounds} with a {@code Rect}
     * whose left top corner is at (left, top) and whose right bottom corner is at (left+w, top+h),
     * which is of course the location of {@code v} on the screen when we were called. We then initialize
     * our field {@code Rect mHoverCellCurrentBounds} with a copy of {@code mHoverCellOriginalBounds}.
     * Finally we set the bounding rectangle of {@code drawable} to {@code mHoverCellCurrentBounds}
     * (this is where the drawable will draw when its draw() method is called), and return {@code drawable}
     * to the caller.
     *
     * @param v the view that has been long clicked, and needs to start hovering.
     * @return a {@code BitmapDrawable} version of our parameter {@code View v}
     */
    private BitmapDrawable getAndAddHoverView(View v) {

        int w = v.getWidth();
        int h = v.getHeight();
        int top = v.getTop();
        int left = v.getLeft();

        Bitmap b = getBitmapWithBorder(v);

        BitmapDrawable drawable = new BitmapDrawable(getResources(), b);

        mHoverCellOriginalBounds = new Rect(left, top, left + w, top + h);
        mHoverCellCurrentBounds = new Rect(mHoverCellOriginalBounds);

        drawable.setBounds(mHoverCellCurrentBounds);

        return drawable;
    }

    /**
     * Draws a black border over the screenshot of the view passed in. First we initialize our variable
     * {@code Bitmap bitmap} with the {@code Bitmap} created from our parameter {@code View v} by our
     * method {@code getBitmapFromView}. We then initialize {@code Canvas can} with an instance which
     * will draw into {@code bitmap}. We initialize {@code Rect rect} with an instance that is the
     * same size as {@code bitmap}. We initialize {@code Paint paint}, set its style to STROKE, set its
     * stroke width to LINE_THICKNESS (15), and set its color to BLACK. We then draw the rectangle
     * {@code rect} on {@code can} using {@code paint} as the {@code Paint}. Finally we return {@code bitmap}
     * to the caller.
     *
     * @param v {@code View} we want to create a screenshot of.
     * @return screenshot of our parameter {@code View v} with a black border.
     */
    private Bitmap getBitmapWithBorder(View v) {
        Bitmap bitmap = getBitmapFromView(v);
        Canvas can = new Canvas(bitmap);

        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(LINE_THICKNESS);
        paint.setColor(Color.BLACK);

        // can.drawBitmap(bitmap, 0, 0, null); this is not necessary!
        can.drawRect(rect, paint);

        return bitmap;
    }

    /**
     * Returns a bitmap showing a screenshot of the view passed in. We initialize {@code Bitmap bitmap}
     * with an instance which is the same width and height as our parameter {@code View v} using a config
     * of ARGB_8888, then initialize {@code Canvas canvas} with an instance which will draw into
     * {@code bitmap}. We call the {@code draw} method of {@code v} to have it draw itself onto
     * {@code canvas} (and thus onto {@code bitmap}), then return {@code bitmap} to the caller.
     *
     * @param v {@code View} whose screenshot we want.
     * @return a bitmap showing a screenshot of our parameter {@code View v}
     */
    private Bitmap getBitmapFromView(View v) {
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas (bitmap);
        v.draw(canvas);
        return bitmap;
    }

    /**
     * Stores a reference to the views above and below the item currently corresponding to the hover
     * cell. It is important to note that if this item is either at the top or bottom of the list,
     * {@code mAboveItemId} or {@code mBelowItemId} may be set to the invalid value. First we set
     * {@code int position} to the position that our method {@code getPositionForID} returns for the
     * item id of the hover cell in our parameter {@code long itemID}. Then we initialize our variable
     * {@code StableArrayAdapter adapter} by retrieving the adapter for this {@code ListView}. We then
     * set our field {@code mAboveItemId} to the item id of the item in the position that is one less
     * than {@code position}, and our field {@code mBelowItemId} to the item id of the item in the
     * position that is one more than {@code position}.
     *
     * @param itemID item id of the hover cell.
     */
    private void updateNeighborViewsForID(long itemID) {
        int position = getPositionForID(itemID);
        StableArrayAdapter adapter = ((StableArrayAdapter)getAdapter());
        mAboveItemId = adapter.getItemId(position - 1);
        mBelowItemId = adapter.getItemId(position + 1);
    }

    /**
     * Retrieves the view in the list corresponding to itemID. We initialize {@code int firstVisiblePosition}
     * with the position in the dataset of the first visible item on the screen. We then initialize
     * {@code StableArrayAdapter adapter} by retrieving our {@code ListView}'s adapter. We loop over
     * {@code int i} for all of our children:
     * <ul>
     *     <li>
     *         We initialize {@code View v} with our {@code i}'th child.
     *     </li>
     *     <li>
     *         We set {@code int position} to {@code firstVisiblePosition} plus {@code i} (this is
     *         the position in our dataset of this {@code i}'th child)
     *     </li>
     *     <li>
     *         We initialize {@code long id} with the item id that the {@code getItemId} method of
     *         {@code adapter} returns for the position {@code position}
     *     </li>
     *     <li>
     *         If {@code id} is equal to {@code itemID} we return {@code v} to the caller (otherwise
     *         we loop around to check the next child.
     *     </li>
     * </ul>
     * If our loop fails to find a child with the item id {@code itemID} we return null to the caller.
     *
     * @param itemID item id of the view we want
     * @return {@code View} that has item id {@code long itemID}
     */
    public View getViewForID (long itemID) {
        int firstVisiblePosition = getFirstVisiblePosition();
        StableArrayAdapter adapter = ((StableArrayAdapter)getAdapter());
        for(int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int position = firstVisiblePosition + i;
            long id = adapter.getItemId(position);
            if (id == itemID) {
                return v;
            }
        }
        return null;
    }

    /**
     * Retrieves the position in the list corresponding to our parameter {@code itemID}. First we
     * initialize {@code View v} with the view that our method {@code getViewForID} returns for the
     * item id {@code itemID}. If {@code v} is null we return -1, otherwise we return the position
     * returned by the method {@code getPositionForView} for {@code v}.
     *
     * @param itemID item id we are searching for
     * @return the position in the list corresponding to our parameter {@code itemID}
     */
    public int getPositionForID (long itemID) {
        View v = getViewForID(itemID);
        if (v == null) {
            return -1;
        } else {
            return getPositionForView(v);
        }
    }

    /**
     *  {@code dispatchDraw} gets invoked when all the child views are about to be drawn.
     *  By overriding this method, the hover cell's {@code BitmapDrawable} can be drawn
     *  over the {@code ListView}'s items whenever the {@code ListView} is redrawn. First
     *  we call our super's implementation of {@code dispatchDraw} to draw all of its items.
     *  Then if our field {@code BitmapDrawable mHoverCell} is not null we call its {@code draw}
     *  method to draw itself on our parameter {@code Canvas canvas}.
     *
     *  @param canvas the canvas on which to draw the view
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mHoverCell != null) {
            mHoverCell.draw(canvas);
        }
    }

    /**
     * We implement this method to handle touch screen motion events. We switch on the masked off
     * action field of our parameter {@code MotionEvent event}:
     * <ul>
     *     <li>
     *         ACTION_DOWN: We initialize our field {@code mDownX} with the X coordinate {@code event}
     *         for first pointer index, and {@code mDownY} with the Y coordinate. We initialize our
     *         field {@code mActivePointerId} with the pointer identifier associated with the 0'th
     *         pointer data index, and then break.
     *     </li>
     *     <li>
     *         ACTION_MOVE: If our field {@code mActivePointerId} is equal to INVALID_POINTER_ID we
     *         just break. Otherwise we initialize {@code int pointerIndex} with the index of the
     *         data in {@code event} for pointer id {@code mActivePointerId}, then set our field
     *         {@code mLastEventY} to the Y coordinate in {@code event} for pointer index
     *         {@code pointerIndex}. We initialize {@code int deltaY} to {@code mLastEventY} minus
     *         {@code mDownY}. If our field {@code mCellIsMobile} is true (a cell has been long
     *         clicked and is being dragged) we offset {@code Rect mHoverCellCurrentBounds} to the
     *         X coordinate given in the {@code left} field of {@code mHoverCellOriginalBounds} (the
     *         hover cell does not move horizontally), and the Y coordinate calculated by adding the
     *         {@code top} field of {@code mHoverCellOriginalBounds} to {@code deltaY} plus our field
     *         {@code mTotalOffset}. We then specify {@code mHoverCellCurrentBounds} as the bounding
     *         rectangle for {@code mHoverCell} (this is where the drawable will draw when its draw()
     *         method is called), and call {@code invalidate} to invalidate our whole view so that
     *         {@code draw} will be called sometime in the future. We then call our method
     *         {@code handleCellSwitch} to check whether the hover cell has been shifted far enough
     *         to invoke a cell swap, and if so, the respective cell swap candidate is determined
     *         and the data set is changed (the layout invoked by a call to {@code notifyDataSetChanged}
     *         will place the cells in the right place). We set our field {@code mIsMobileScrolling}
     *         to false, then call our method {@code handleMobileCellScroll} to determine if the hover
     *         cell is above or below the bounds of the ListView, and if so, has the ListView do an
     *         appropriate upward or downward smooth scroll so as to reveal new items. Finally we return
     *         false to the caller so that regular touch event processing will proceed. On the other
     *         hand if {@code mCellIsMobile} is false we just break.
     *     </li>
     *     <li>
     *         ACTION_UP: We call our method {@code touchEventsEnded} to reset all the appropriate
     *         fields to a default state while also animating the hover cell to its correct location
     *         in the {@code ListView}, and then break.
     *     </li>
     *     <li>
     *         ACTION_CANCEL: We call our method {@code touchEventsCancelled} to reset all the
     *         appropriate fields to a default state, and then break.
     *     </li>
     *     <li>
     *         ACTION_POINTER_UP: We mask off the action pointer index from the action of {@code event}
     *         and normalize its shift in order to set {@code pointerIndex}. We initialize {@code int pointerId}
     *         with the pointer id associated with {@code pointerIndex} and if it is equal to
     *         {@code mActivePointerId} (the pointer id for the ACTION_DOWN which started the touch
     *         sequence) we call our method {@code touchEventsEnded} to reset all the appropriate
     *         fields to a default state while also animating the hover cell to its correct location
     *         in the {@code ListView} same as for an ACTION_UP event. In either case we then break.
     *     </li>
     *     <li>
     *         default: we just break.
     *     </li>
     * </ul>
     * Finally we return the value returned by our super's implementation of {@code onTouchEvent} to
     * the caller.
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent (MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int)event.getX();
                mDownY = (int)event.getY();
                mActivePointerId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER_ID) {
                    break;
                }

                int pointerIndex = event.findPointerIndex(mActivePointerId);

                mLastEventY = (int) event.getY(pointerIndex);
                int deltaY = mLastEventY - mDownY;

                if (mCellIsMobile) {
                    mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left,
                            mHoverCellOriginalBounds.top + deltaY + mTotalOffset);
                    mHoverCell.setBounds(mHoverCellCurrentBounds);
                    invalidate();

                    handleCellSwitch();

                    mIsMobileScrolling = false;
                    handleMobileCellScroll();

                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
                touchEventsEnded();
                break;
            case MotionEvent.ACTION_CANCEL:
                touchEventsCancelled();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                /* If a multitouch event took place and the original touch dictating
                 * the movement of the hover cell has ended, then the dragging event
                 * ends and the hover cell is animated to its corresponding position
                 * in the ListView. */
                pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                        MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    touchEventsEnded();
                }
                break;
            default:
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * This method determines whether the hover cell has been shifted far enough to invoke a cell
     * swap. If so, then the respective cell swap candidate is determined and the data set is changed.
     * Upon posting a notification of the data set change, a layout is invoked to place the cells in
     * the right place. Using a ViewTreeObserver and a corresponding OnPreDrawListener, we can offset
     * the cell being swapped to where it previously was and then animate it to its new position.
     * <p>
     * First we initialize {@code int deltaY} by subtracting {@code mDownY} from {@code mLastEventY}
     * (giving us the plus or minus distance on the screen we have moved since the initial long click,
     * or the last time a cell was swapped -- it is set to {@code mLastEventY} every time a cell is
     * swapped). Then we initialize {@code int deltaYTotal} by adding the {@code top} field of
     * {@code mHoverCellOriginalBounds} to {@code mTotalOffset} plus {@code deltaY} (this is the
     * current position of the top of the hover cell). We initialize {@code View belowView} with the
     * view with the id {@code mBelowItemId}, {@code View mobileView} with the view with the id
     * {@code mMobileItemId}, and {@code View aboveView} with the view with the id {@code mAboveItemId}.
     * We initialize {@code boolean isBelow} to true if {@code belowView} is not null and
     * {@code deltaYTotal} is greater than the top Y coordinate of {@code belowView}. We initialize
     * {@code boolean isAbove} to true if {@code aboveView} is not null and {@code deltaYTotal} is
     * less than the top Y coordinate of {@code aboveView}. If either {@code isBelow} or {@code isAbove}
     * is true we need to switch cells:
     * <ul>
     *     <li>
     *         We initialize {@code long switchItemID} to {@code mBelowItemId} if {@code isBelow} is
     *         true or to {@code mAboveItemId} if it is false, and initialize {@code View switchView}
     *         to {@code belowView} if {@code isBelow} is true or to {@code aboveView} if it is false.
     *     </li>
     *     <li>
     *         We initialize {@code int originalItem} with the position in our {@code AdapterView}'s
     *         dataset that the {@code getPositionForView} method calculates for {@code mobileView}.
     *     </li>
     *     <li>
     *         If {@code switchView} is null we call our method {@code updateNeighborViewsForID} to
     *         update the item id's of {@code mAboveItemId} and {@code mBelowItemId} to values for
     *         the item id {@code mMobileItemId} (the items before and after the hover cell's item id).
     *         I cannot find a case where {@code switchView} can possibly be null, the logic before
     *         this statement guaranties that it is either {@code belowView} or {@code aboveView}!
     *     </li>
     *     <li>
     *         We call our method {@code swapElements} to swap the data items at position {@code originalItem}
     *         (the item being dragged) with the item at the position occupied by {@code switchView} in
     *         our dataset {@code ArrayList<String> mCheeseList}. We then call the {@code notifyDataSetChanged}
     *         method of our adapter to notify it that that dataset changed and the {@code ListView} needs
     *         to be redrawn.
     *     </li>
     *     <li>
     *         We set {@code mDownY} to {@code mLastEventY}, and initialize {@code int switchViewStartTop}
     *         to the top Y coordinate of {@code switchView}.
     *     </li>
     *     <li>
     *         We set the visibility of {@code mobileView} to VISIBLE and the visibility of {@code switchView}
     *         to INVISIBLE, then we call our {@code updateNeighborViewsForID} to have it update the values
     *         of {@code mAboveItemId} (the id of the item that is now above the long clicked item on the screen
     *         given the new location of the item with id {@code mMobileItemId}), and {@code mBelowItemId} (the
     *         id of the item that is now below the long clicked item on the screen given the new location of
     *         the item with id {@code mMobileItemId}).
     *     </li>
     *     <li>
     *         We initialize {@code ViewTreeObserver observer} with the {@code ViewTreeObserver} for this view's
     *         hierarchy and add to it an anonymous {@code OnPreDrawListener} which animates the view containing
     *         the item with id {@code switchItemID} into its new position on the screen, and then returns true
     *         to the caller so that the drawing will proceed.
     *     </li>
     * </ul>
     */
    private void handleCellSwitch() {
        final int deltaY = mLastEventY - mDownY;
        int deltaYTotal = mHoverCellOriginalBounds.top + mTotalOffset + deltaY;

        View belowView = getViewForID(mBelowItemId);
        View mobileView = getViewForID(mMobileItemId);
        View aboveView = getViewForID(mAboveItemId);

        boolean isBelow = (belowView != null) && (deltaYTotal > belowView.getTop());
        boolean isAbove = (aboveView != null) && (deltaYTotal < aboveView.getTop());

        if (isBelow || isAbove) {
            Log.i(TAG, "deltaY: " + deltaY);

            final long switchItemID = isBelow ? mBelowItemId : mAboveItemId;
            View switchView = isBelow ? belowView : aboveView;
            final int originalItem = getPositionForView(mobileView);

            //noinspection ConstantConditions
            if (switchView == null) {
                updateNeighborViewsForID(mMobileItemId);
                return;
            }

            swapElements(mCheeseList, originalItem, getPositionForView(switchView));

            ((BaseAdapter) getAdapter()).notifyDataSetChanged();

            mDownY = mLastEventY;

            final int switchViewStartTop = switchView.getTop();

            mobileView.setVisibility(View.VISIBLE);
            switchView.setVisibility(View.INVISIBLE);

            updateNeighborViewsForID(mMobileItemId);

            final ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                /**
                 * Callback method to be invoked when the view tree is about to be drawn. At this point, all
                 * views in the tree have been measured and given a frame. First we remove ourselves as an
                 * {@code OnPreDrawListener}. Then we initialize {@code View switchView} with the view that
                 * our method {@code getViewForID} finds for the item id {@code switchItemID}. Then we add
                 * {@code deltaY} to our field {@code mTotalOffset}. We initialize {@code int switchViewNewTop}
                 * with the new top Y coordinate of {@code switchView}, and {@code int delta} by subtracting
                 * {@code switchViewNewTop} from {@code switchViewStartTop} (this is how far the switched view
                 * has to move to get to its new location), then we set the the vertical location of this view
                 * relative to its {@code getTop()} position to {@code delta} (this effectively positions the
                 * object post-layout, in addition to wherever the object's layout placed it). Then we initialize
                 * {@code ObjectAnimator animator} with an instance which will animate the TRANSLATION_Y property
                 * of {@code switchView} back to 0, set its duration to MOVE_DURATION (150ms), and start it running.
                 * Finally we return true to the caller so the current drawing pass will proceed.
                 *
                 * @return Returns true to proceed with the current drawing pass.
                 */
                @Override
                public boolean onPreDraw() {
                    observer.removeOnPreDrawListener(this);

                    View switchView = getViewForID(switchItemID);

                    mTotalOffset += deltaY;

                    int switchViewNewTop = switchView.getTop();
                    int delta = switchViewStartTop - switchViewNewTop;

                    switchView.setTranslationY(delta);

                    ObjectAnimator animator = ObjectAnimator.ofFloat(switchView, View.TRANSLATION_Y, 0);
                    animator.setDuration(MOVE_DURATION);
                    animator.start();

                    return true;
                }
            });
        }
    }

    /**
     * Swaps the location of two items in our dataset. First we initialize {@code Object temp} with
     * the {@code Object} at position {@code indexOne} in the list {@code arrayList}, then we replace
     * the element at position {@code indexOne} in this list with the element in position {@code indexTwo}.
     * Finally we replace the element at position {@code indexTwo} in this list with {@code temp}.
     *
     * @param arrayList the {@code ArrayList} holding our dataset
     * @param indexOne the first index into {@code ArrayList arrayList} whose item is to swapped
     * @param indexTwo the second index into {@code ArrayList arrayList} whose item is to swapped
     */
    private void swapElements(ArrayList arrayList, int indexOne, int indexTwo) {
        Object temp = arrayList.get(indexOne);
        //noinspection unchecked
        arrayList.set(indexOne, arrayList.get(indexTwo));
        //noinspection unchecked
        arrayList.set(indexTwo, temp);
    }


    /**
     * Resets all the appropriate fields to a default state while also animating the hover cell back
     * to its correct location. First we initialize {@code View mobileView} with the view whose item
     * id is {@code mMobileItemId}. Then we branch based on whether either of our fields {@code mCellIsMobile}
     * or {@code mIsWaitingForScrollFinish} is true:
     * <ul>
     *     <li>
     *         Either {@code mCellIsMobile} (an item in our list has been long clicked and has been being
     *         dragged about) or {@code mIsWaitingForScrollFinish} ({@code mScrollState} was not equal to
     *         SCROLL_STATE_IDLE on a previous call to us) is true:
     *         We set {@code mCellIsMobile}, {@code mIsWaitingForScrollFinish}, and {@code mIsMobileScrolling}
     *         all to false, and set {@code mActivePointerId} to INVALID_POINTER_ID. Then if {@code mScrollState}
     *         is not equal to SCROLL_STATE_IDLE we set {@code mIsWaitingForScrollFinish} to true and just
     *         return (since the AutoScroller has not completed scrolling, we need to wait for it to finish
     *         in order to determine the final location of where the hover cell should be animated to).
     *         Otherwise we offset the rectangle of {@code mHoverCellCurrentBounds} to the top Y coordinate
     *         of {@code mobileView}. We initialize {@code ObjectAnimator hoverViewAnimator} with an instance
     *         which will animate the "bounds" property to {@code BitmapDrawable mHoverCell} to {@code mHoverCellCurrentBounds}
     *         using {@code sBoundEvaluator} as the {@code TypeEvaluator}, and set its {@code AnimatorUpdateListener}
     *         to an anonymous class whose {@code onAnimationUpdate} override just calls {@code invalidate} to
     *         have our view redrawn, and whose {@code onAnimationEnd} sets everything back to the state they
     *         should be in when the dragging is over. Finally we start {@code hoverViewAnimator} running.
     *     </li>
     *     <li>
     *         If both {@code mCellIsMobile} and {@code mIsWaitingForScrollFinish} are false we just
     *         call our {@code touchEventsCancelled} method to reset all the appropriate fields to a
     *         default state.
     *     </li>
     * </ul>
     */
    private void touchEventsEnded () {
        final View mobileView = getViewForID(mMobileItemId);
        if (mCellIsMobile || mIsWaitingForScrollFinish) {
            mCellIsMobile = false;
            mIsWaitingForScrollFinish = false;
            mIsMobileScrolling = false;
            mActivePointerId = INVALID_POINTER_ID;

            // If the AutoScroller has not completed scrolling, we need to wait for it to
            // finish in order to determine the final location of where the hover cell
            // c.
            if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mIsWaitingForScrollFinish = true;
                return;
            }

            mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left, mobileView.getTop());

            ObjectAnimator hoverViewAnimator = ObjectAnimator.ofObject(mHoverCell, "bounds",
                    sBoundEvaluator, mHoverCellCurrentBounds);
            hoverViewAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /**
                 * Notifies the occurrence of another frame of the animation. We just call the
                 * {@code invalidate} method to have our view redrawn.
                 *
                 * @param valueAnimator The animation which has started another frame.
                 */
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    invalidate();
                }
            });
            hoverViewAnimator.addListener(new AnimatorListenerAdapter() {
                /**
                 * Notifies the start of the animation. We disable our view (to prevent additional
                 * touch events occurring during the animation).
                 *
                 * @param animation The started animation.
                 */
                @Override
                public void onAnimationStart(Animator animation) {
                    setEnabled(false);
                }

                /**
                 * Notifies the end of the animation. We set our fields {@code mAboveItemId}, {@code mMobileItemId},
                 * and {@code mBelowItemId} all to INVALID_ID, set the visibility of {@code mobileView} to VISIBLE,
                 * set {@code mHoverCell} to null, enable our view, and then call {@code invalidate} to have our
                 * view redrawn.
                 *
                 * @param animation The animation which reached its end.
                 */
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAboveItemId = INVALID_ID;
                    mMobileItemId = INVALID_ID;
                    mBelowItemId = INVALID_ID;
                    mobileView.setVisibility(VISIBLE);
                    mHoverCell = null;
                    setEnabled(true);
                    invalidate();
                }
            });
            hoverViewAnimator.start();
        } else {
            touchEventsCancelled();
        }
    }

    /**
     * Resets all the appropriate fields to a default state. First we initialize {@code View mobileView}
     * with the view whose item id is {@code mMobileItemId}. If {@code mCellIsMobile} is true:
     * <ul>
     *     <li>
     *         We set our fields {@code mAboveItemId}, {@code mMobileItemId}, and {@code mBelowItemId}
     *         all to INVALID_ID, set the visibility of {@code mobileView} to VISIBLE, set {@code mHoverCell}
     *         to null, and then call {@code invalidate} to have our view redrawn.
     *     </li>
     * </ul>
     * We then set our fields {@code mCellIsMobile} and {@code mIsMobileScrolling} to false and set
     * {@code mActivePointerId} to INVALID_POINTER_ID.
     */
    private void touchEventsCancelled () {
        View mobileView = getViewForID(mMobileItemId);
        if (mCellIsMobile) {
            mAboveItemId = INVALID_ID;
            mMobileItemId = INVALID_ID;
            mBelowItemId = INVALID_ID;
            mobileView.setVisibility(VISIBLE);
            mHoverCell = null;
            invalidate();
        }
        mCellIsMobile = false;
        mIsMobileScrolling = false;
        mActivePointerId = INVALID_POINTER_ID;
    }

    /**
     * This TypeEvaluator is used to animate the BitmapDrawable back to its
     * final location when the user lifts his finger by modifying the
     * BitmapDrawable's bounds.
     */
    private final static TypeEvaluator<Rect> sBoundEvaluator = new TypeEvaluator<Rect>() {
        /**
         * This function returns the result of linearly interpolating the start and end values, with
         * {@code fraction} representing the proportion between the start and end values. We return
         * a new instance of {@code Rect} whose {@code left}, {@code top}, {@code right}, and {@code bottom}
         * values are calculated by our method {@code interpolate} by interpolating between the {@code startValue}
         * and {@code endValue} values of those fields with {@code fraction} representing the proportion
         * between the start and end values.
         *
         * @param fraction   The fraction from the starting to the ending values
         * @param startValue The start value.
         * @param endValue   The end value.
         * @return A linear interpolation between the start and end values, given the
         *         {@code fraction} parameter.
         */
        @Override
        public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
            return new Rect(interpolate(startValue.left, endValue.left, fraction),
                    interpolate(startValue.top, endValue.top, fraction),
                    interpolate(startValue.right, endValue.right, fraction),
                    interpolate(startValue.bottom, endValue.bottom, fraction));
        }

        /**
         * Returns a value calculated by linearly interpolating the start and end values, with
         * {@code fraction} representing the proportion between the start and end values.
         *
         * @param start start value
         * @param end end value
         * @param fraction proportion between the start and end values
         * @return value calculated by linearly interpolating the start and end values, with
         * {@code fraction} representing the proportion between the start and end values
         */
        public int interpolate(int start, int end, float fraction) {
            return (int)(start + fraction * (end - start));
        }
    };

    /**
     * Determines whether this ListView is in a scrolling state invoked by the fact that the hover
     * cell is out of the bounds of the ListView. We just set our field {@code mIsMobileScrolling}
     * to the value returned by our {@code handleMobileCellScroll} method for the current value of
     * our field {@code Rect mHoverCellCurrentBounds}.
     */
    private void handleMobileCellScroll() {
        mIsMobileScrolling = handleMobileCellScroll(mHoverCellCurrentBounds);
    }

    /**
     * This method is in charge of determining if the hover cell is above or below the bounds of the
     * {@code ListView}. If so, the {@code ListView} does an appropriate upward or downward smooth
     * scroll so as to reveal new items. We initialize {@code int offset} with the value that the
     * {@code computeVerticalScrollOffset} method returns for the vertical offset of the scrollbar's
     * thumb. We initialize {@code int height} with the height of our view. We initialize {@code int extent}
     * with the value that the {@code computeVerticalScrollExtent} method returns for the vertical
     * extent of the scrollbar's thumb. We initialize {@code int range} with the value that the
     * {@code computeVerticalScrollRange} method returns for the total vertical range represented by
     * the vertical scrollbar. We initialize {@code int hoverViewTop} with the {@code top} field of
     * our parameter {@code Rect r} (the top Y coordinate of our hover cell). And we initialize
     * {@code int hoverHeight} with the height of our hover cell. If {@code hoverViewTop} is less
     * than or equal to 0 (the hover cell has been dragged to the very top of the {@code ListView})
     * and {@code offset} is greater than 0 (part of the {@code ListView} is above the top of the
     * screen and can be scrolled down) we call the method {@code smoothScrollBy} to scroll the
     * {@code ListView} by {@code -mSmoothScrollAmountAtEdge} (down) with 0 as the animation duration,
     * and then return true to the caller. If {@code hoverViewTop} plus {@code hoverHeight} is greater
     * than or equal to {@code height} (the hover cell has been dragged to the bottom of the screen)
     * and {@code offset} plus {@code extent} is less than {@code range} (there are still cells below
     * the bottom of the screen in the {@code ListView}) we call the method {@code smoothScrollBy} to
     * scroll the {@code ListView} by {@code mSmoothScrollAmountAtEdge} (up) with 0 as the animation
     * duration, and then return true to the caller. If the hover cell is not at top or the bottom of
     * the screen we return false to the caller.
     *
     * @param r current hover cell bounds (always {@code mHoverCellCurrentBounds} in our case)
     * @return true if the {@code ListView} is smooth scrolling, false if it did not need to.
     */
    public boolean handleMobileCellScroll(Rect r) {
        int offset = computeVerticalScrollOffset();
        int height = getHeight();
        int extent = computeVerticalScrollExtent();
        int range = computeVerticalScrollRange();
        int hoverViewTop = r.top;
        int hoverHeight = r.height();

        if (hoverViewTop <= 0 && offset > 0) {
            smoothScrollBy(-mSmoothScrollAmountAtEdge, 0);
            return true;
        }

        if (hoverViewTop + hoverHeight >= height && (offset + extent) < range) {
            smoothScrollBy(mSmoothScrollAmountAtEdge, 0);
            return true;
        }

        return false;
    }

    /**
     * Sets our dataset {@code ArrayList<String> mCheeseList} to our parameter {@code cheeseList}.
     *
     * @param cheeseList List of cheeses that we should use as our dataset.
     */
    public void setCheeseList(ArrayList<String> cheeseList) {
        mCheeseList = cheeseList;
    }

    /**
     * This scroll listener is added to the ListView in order to handle cell swapping
     * when the cell is either at the top or bottom edge of the ListView. If the hover
     * cell is at either edge of the ListView, the ListView will begin scrolling. As
     * scrolling takes place, the ListView continuously checks if new cells became visible
     * and determines whether they are potential candidates for a cell swap.
     */
    private AbsListView.OnScrollListener mScrollListener = new AbsListView.OnScrollListener () {

        /**
         * Value of the index of the first visible cell on the previous call to {@code onScroll}
         */
        private int mPreviousFirstVisibleItem = -1;
        /**
         * Value of the number of visible cells on the previous call to {@code onScroll}
         */
        private int mPreviousVisibleItemCount = -1;
        /**
         * Value of the index of the first visible cell on the latest call to {@code onScroll}
         */
        private int mCurrentFirstVisibleItem;
        /**
         * Value of the number of visible cells on the latest call to {@code onScroll}
         */
        private int mCurrentVisibleItemCount;
        /**
         * Value of the latest scroll state reported to {@code onScrollStateChanged}
         */
        private int mCurrentScrollState;

        /**
         * Callback method to be invoked when the list or grid has been scrolled. This will be called
         * after the scroll has completed. We save our parameter {@code int firstVisibleItem} in
         * {@code mCurrentFirstVisibleItem}, and our parameter {@code int visibleItemCount} in
         * {@code mCurrentVisibleItemCount}. If {@code mPreviousFirstVisibleItem} is equal to -1 (the
         * uninitialized state) we set it to {@code mCurrentFirstVisibleItem} and if {@code mPreviousVisibleItemCount}
         * is equal to -1 (the uninitialized state) we set it to {@code mCurrentVisibleItemCount}. We
         * then call the methods {@code checkAndHandleFirstVisibleCellChange} (which handles the situation
         * where mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) and {@code checkAndHandleLastVisibleCellChange}
         * (which handles the situation where the {@code ListView} has scrolled down enough to reveal
         * a new cell at the bottom of the list). Finally we set {@code mPreviousFirstVisibleItem} to
         * {@code mCurrentFirstVisibleItem} and {@code mPreviousVisibleItemCount} to {@code mCurrentVisibleItemCount}.
         *
         * @param view             The view whose scroll state is being reported
         * @param firstVisibleItem the index of the first visible cell (ignore if visibleItemCount == 0)
         * @param visibleItemCount the number of visible cells
         * @param totalItemCount   the number of items in the list adaptor
         */
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            mCurrentFirstVisibleItem = firstVisibleItem;
            mCurrentVisibleItemCount = visibleItemCount;

            mPreviousFirstVisibleItem = (mPreviousFirstVisibleItem == -1) ? mCurrentFirstVisibleItem
                    : mPreviousFirstVisibleItem;
            mPreviousVisibleItemCount = (mPreviousVisibleItemCount == -1) ? mCurrentVisibleItemCount
                    : mPreviousVisibleItemCount;

            checkAndHandleFirstVisibleCellChange();
            checkAndHandleLastVisibleCellChange();

            mPreviousFirstVisibleItem = mCurrentFirstVisibleItem;
            mPreviousVisibleItemCount = mCurrentVisibleItemCount;
        }

        /**
         * Callback method to be invoked while the list view or grid view is being scrolled. If the
         * view is being scrolled, this method will be called before the next frame of the scroll is
         * rendered. We save our parameter {@code int scrollState} in {@code mCurrentScrollState} as
         * well as in our field {@code mScrollState}. We then call our {@code isScrollCompleted} method
         * to either continue a scroll invoked by the hover cell being outside the bounds of the
         * {@code ListView}, or if the hover cell has already been released invoke the animation for
         * the hover cell to return to its correct position after the ListView has entered an idle
         * scroll state.
         *
         * @param view The view whose scroll state is being reported
         * @param scrollState The current scroll state. One of
         * {@link #SCROLL_STATE_TOUCH_SCROLL} or {@link #SCROLL_STATE_IDLE}.
         */
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            mCurrentScrollState = scrollState;
            mScrollState = scrollState;
            isScrollCompleted();
        }

        /**
         * This method is in charge of invoking 1 of 2 actions. Firstly, if the {@code ListView} is
         * in a state of scrolling invoked by the hover cell being outside its bounds, then this
         * scrolling event is continued. Secondly, if the hover cell has already been released, this
         * invokes the animation for the hover cell to return to its correct position after the
         * {@code ListView} has entered an idle scroll state. If {@code mCurrentVisibleItemCount} is
         * greater than 0 (there are one or more visible items from our {@code ListView} on the screen)
         * and {@code mCurrentScrollState} is equal to SCROLL_STATE_IDLE (view is not scrolling) we
         * branch whether both {@code mCellIsMobile} and {@code mIsMobileScrolling} are true:
         * <ul>
         *     <li>
         *         yes: we call the {@code handleMobileCellScroll} to handle any scrolling that may
         *         be necessary when the hover cell is above or below the bounds of the {@code ListView}.
         *     </li>
         *     <li>
         *         no: if {@code mIsWaitingForScrollFinish} is true we call our {@code touchEventsEnded}
         *         method to reset all the appropriate fields to a default state while also animating
         *         the hover cell back to its correct location.
         *     </li>
         * </ul>
         */
        private void isScrollCompleted() {
            if (mCurrentVisibleItemCount > 0 && mCurrentScrollState == SCROLL_STATE_IDLE) {
                if (mCellIsMobile && mIsMobileScrolling) {
                    handleMobileCellScroll();
                } else if (mIsWaitingForScrollFinish) {
                    touchEventsEnded();
                }
            }
        }

        /**
         * Determines if the ListView scrolled up enough to reveal a new cell at the top of the list.
         * If so, then the appropriate parameters are updated. If {@code mCurrentFirstVisibleItem} is
         * not equal to {@code mPreviousFirstVisibleItem} (the first visible item has changed) we check
         * to see whether{@code mCellIsMobile} is true (the hover cell is in motion) and {@code mMobileItemId}
         * is not equal to INVALID_ID (the item id of the hover cell is valid) and if this is true we
         * call our {@code updateNeighborViewsForID} method to update the item id's of the items above
         * and below {@code mMobileItemId} in the {@code ListView} and then call our method {@code handleCellSwitch}
         * to swap cells if it is necessary to do so.
         */
        public void checkAndHandleFirstVisibleCellChange() {
            if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
                if (mCellIsMobile && mMobileItemId != INVALID_ID) {
                    updateNeighborViewsForID(mMobileItemId);
                    handleCellSwitch();
                }
            }
        }

        /**
         * Determines if the ListView scrolled down enough to reveal a new cell at the bottom of the
         * list. If so, then the appropriate parameters are updated. We set {@code int currentLastVisibleItem}
         * to {@code mCurrentFirstVisibleItem} plus {@code mCurrentVisibleItemCount} (this is the index
         * of the last visible item in the {@code ListView}) and {@code int previousLastVisibleItem} to
         * {@code mPreviousFirstVisibleItem} plus {@code mPreviousVisibleItemCount} (this is the index
         * of the previous last visible item in the {@code ListView}). If {@code currentLastVisibleItem}
         * is not equal to {@code previousLastVisibleItem} (the last visible item has changed) we check
         * to see whether{@code mCellIsMobile} is true (the hover cell is in motion) and {@code mMobileItemId}
         * is not equal to INVALID_ID (the item id of the hover cell is valid) and if this is true we
         * call our {@code updateNeighborViewsForID} method to update the item id's of the items above
         * to swap cells if it is necessary to do so.
         */
        public void checkAndHandleLastVisibleCellChange() {
            int currentLastVisibleItem = mCurrentFirstVisibleItem + mCurrentVisibleItemCount;
            int previousLastVisibleItem = mPreviousFirstVisibleItem + mPreviousVisibleItemCount;
            if (currentLastVisibleItem != previousLastVisibleItem) {
                if (mCellIsMobile && mMobileItemId != INVALID_ID) {
                    updateNeighborViewsForID(mMobileItemId);
                    handleCellSwitch();
                }
            }
        }
    };
}