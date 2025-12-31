/*
 * Copyright (C) 2007 The Android Open Source Project
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
package com.example.android.snake

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import java.util.Random

/**
 * [SnakeView]: implementation of a simple game of Snake
 */
class SnakeView : TileView {

    /**
     * Current mode of application: [READY] to run, [RUNNING], or you have already lost.
     * static final ints are used instead of an enum for performance reasons.
     */
    var gameState: Int = READY
        private set

    /**
     * Current direction the snake is headed.
     */
    private var mDirection = NORTH

    /**
     * The next direction for the snake to move.
     */
    private var mNextDirection = NORTH

    /**
     * Used to track the number of apples captured.
     */
    private var mScore: Long = 0

    /**
     * number of milliseconds between snake movements. This will decrease as apples are captured.
     */
    private var mMoveDelay: Long = 600

    /**
     * Tracks the absolute time when the snake last moved, and is used to determine if a
     * move should be made based on [mMoveDelay].
     */
    private var mLastMove: Long = 0

    /**
     * [mStatusText]: [TextView] that shows status to the user in some run states
     */
    private var mStatusText: TextView? = null

    /**
     * [mArrowsView]: [View] which shows 4 arrows to signify 4 directions in which the snake can move
     */
    private var mArrowsView: View? = null

    /**
     * [mBackgroundView]: Background [View] which shows 4 different colored triangles which the user
     * can click to change the direction the snake is moving.
     */
    private var mBackgroundView: View? = null

    /**
     * [mSnakeTrail]: A list of [Coordinate] that make up the snake's body.
     */
    private var mSnakeTrail = ArrayList<Coordinate?>()

    /**
     * The list of [Coordinate] that have an apple in them.
     */
    private var mAppleList = ArrayList<Coordinate?>()

    /**
     * Create a simple handler that we can use to cause animation to happen. We set ourselves as a
     * target and we can use the sleep() function to cause an update/invalidate to occur at a later
     * date.
     */
    private val mRedrawHandler = RefreshHandler()

    /**
     * A simple handler that we can use to cause animation to happen.
     */
    @SuppressLint("HandlerLeak") // TODO: Determine if a leak is possible (I doubt it)
    internal inner class RefreshHandler : Handler(Looper.getMainLooper()) {
        /**
         * Subclasses must implement this to receive messages. We call the [update] method of
         * this instance of [SnakeView] to update the snake's location if necessary, then call
         * its [invalidate] method to schedule a call to its [onDraw] method.
         *
         * @param msg A [Message][Message] object
         */
        override fun handleMessage(msg: Message) {
            update()
            this@SnakeView.invalidate()
        }

        /**
         * Sends a delayed message to ourselves. First we remove all other messages whose `what`
         * field is 0, then we enqueue a message with a `what` field of 0 to run after the delay
         * specified by our parameter `delayMillis`
         *
         * @param delayMillis how long to wait to send a message to ourselves in milliseconds.
         */
        fun sleep(delayMillis: Long) {
            this.removeMessages(/* what = */ 0)
            sendMessageDelayed(/* msg = */ obtainMessage(0), /* delayMillis = */ delayMillis)
        }
    }

    /**
     * Constructor that is called when inflating a view from XML. First we call our super's constructor,
     * then we call our method [initSnakeView] to initialize our view.
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initSnakeView(context = context)
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute. First
     * we call our super's constructor, then we call our method [initSnakeView] to initialize
     * our view.
     *
     * @param context  The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style resource
     * that supplies default values for the view. Can be 0 to not look for defaults.
     * We ignore.
     */
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initSnakeView(context)
    }

    /**
     * Initializes this view. First we enable our view to receive focus, then we initialize our
     * [Resources] variable ` r` with a `Resources` instance for the application's package. We call
     * our method [resetTiles] to allocate 4 entries for our [Array] of [Bitmap] field [mTileArray],
     * then call our method [loadTile] three times to load the drawable with resource id
     * `R.drawable.redstar` into the [RED_STAR] (1) index of [mTileArray], the drawable with resource
     * id `R.drawable.yellowstar` into the [YELLOW_STAR] (2) index of [mTileArray], and the drawable
     * with resource id `R.drawable.greenstar` into the [GREEN_STAR] (3) index of [mTileArray] (note
     * that we use the overload of [Resources.getDrawable] that includes a `theme` argument for
     * devices running `LOLLIPOP` or newer).
     *
     * @param context The Context the view is running in, through which it can access
     * the current theme, resources, etc.
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun initSnakeView(context: Context) {
        isFocusable = true
        val r: Resources = context.resources
        resetTiles(tileCount = 4)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            loadTile(key = RED_STAR, tile = r.getDrawable(R.drawable.redstar, null))
        } else {
            @Suppress("DEPRECATION") // Needed for pre-LOLLIPOP
            loadTile(key = RED_STAR, tile = r.getDrawable(R.drawable.redstar))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            loadTile(key = YELLOW_STAR, tile = r.getDrawable(R.drawable.yellowstar, null))
        } else {
            @Suppress("DEPRECATION") // Needed for pre-LOLLIPOP
            loadTile(key = YELLOW_STAR, tile = r.getDrawable(R.drawable.yellowstar))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            loadTile(key = GREEN_STAR, tile = r.getDrawable(R.drawable.greenstar, null))
        } else {
            @Suppress("DEPRECATION") // Needed for pre-LOLLIPOP
            loadTile(key = GREEN_STAR, tile = r.getDrawable(R.drawable.greenstar))
        }
    }

    /**
     * Initializes the view for the start of a new game. First we clear our two fields
     * [ArrayList] of [Coordinate]'s [mSnakeTrail] and [ArrayList] of [Coordinate]'s [mAppleList].
     * Then we add 6 segments of a snake to [mSnakeTrail] with locations (7,7), (6,7), (5,7), (4,7),
     * (3,7) and (2,7), and we set the field [mNextDirection] to [NORTH]. We then call our method
     * [addRandomApple] to add two randomly placed apples to our view. Finally we set the delay
     * between moves [mMoveDelay] to 600ms and set the score [mScore] to 0.
     */
    private fun initNewGame() {
        mSnakeTrail.clear()
        mAppleList.clear()

        // For now we're just going to load up a short default eastbound snake
        // that's just turned north
        mSnakeTrail.add(Coordinate(7, 7))
        mSnakeTrail.add(Coordinate(6, 7))
        mSnakeTrail.add(Coordinate(5, 7))
        mSnakeTrail.add(Coordinate(4, 7))
        mSnakeTrail.add(Coordinate(3, 7))
        mSnakeTrail.add(Coordinate(2, 7))
        mNextDirection = NORTH

        // Two apples to start with
        addRandomApple()
        addRandomApple()
        mMoveDelay = 600
        mScore = 0
    }

    /**
     * Given a [ArrayList] of coordinates, we need to flatten them into an array of [Int]'s before
     * we can stuff them into a map for flattening and storage. First we initialize [Int] array
     * `val rawArray` with an array that will hold twice as many [Int]'s as there are [Coordinate]
     * objects in our [ArrayList] of [Coordinate] parameter [cVec], and initialize [Int] variable
     * `var i` to 0. We then loop over all of the `c` [Coordinate] in [cVec] assigning the [Coordinate.x]
     * field of `c` to the `i`'th entry of `rawArray`, post incrementing `i` and then assigning the
     * [Coordinate.y] field of `c` to this `i`'th entry of `rawArray` also post incrementing `i`.
     * When done filling `rawArray` with the values from [cVec] we return `rawArray` to the caller.
     *
     * @param cVec a [ArrayList] of Coordinate objects
     * @return : a simple array containing the x/y values of the coordinates as
     * [x1,y1,x2,y2,x3,y3...]
     */
    private fun coordArrayListToArray(cVec: ArrayList<Coordinate?>): IntArray {
        val rawArray = IntArray(cVec.size * 2)
        var i = 0
        for (c in cVec) {
            rawArray[i++] = c!!.x
            rawArray[i++] = c.y
        }
        return rawArray
    }

    /**
     * Save game state so that the user does not lose anything if the game process is killed while
     * we are in the background. First we allocate a new instance for our [Bundle] variable `val map`.
     * Then we add the [Int] array created from our [ArrayList] of [Coordinate] field [mAppleList]
     * by our method [coordArrayListToArray] to `map` under the key "mAppleList", add our field
     * [mDirection] under the key "mDirection", add our field [mNextDirection] under the key
     * "mNextDirection", add our field [mMoveDelay] under our key "mMoveDelay", add our field
     * [mScore] under the key "mScore", and add the [Int] array created from our [ArrayList] of
     * [Coordinate] field  [mSnakeTrail] under the key "mSnakeTrail". Finally we return `map` to
     * the caller.
     *
     * @return a [Bundle] with this view's state
     */
    fun saveState(): Bundle {
        val map = Bundle()
        map.putIntArray("mAppleList", coordArrayListToArray(cVec = mAppleList))
        map.putInt("mDirection", mDirection)
        map.putInt("mNextDirection", mNextDirection)
        map.putLong("mMoveDelay", mMoveDelay)
        map.putLong("mScore", mScore)
        map.putIntArray("mSnakeTrail", coordArrayListToArray(cVec = mSnakeTrail))
        return map
    }

    /**
     * Given a flattened array of ordinate pairs, we reconstitute them into a [ArrayList] of
     * [Coordinate] objects. First we initialize our [ArrayList] of [Coordinate] variable
     * `val coordArrayList` with a new instance, and initialize [Int] variable `val coordCount`
     * to the length of our [IntArray] parameter [rawArray]. Then we loop over [Int] `index`
     * incrementing by 2 each iteration constructing [Coordinate] variable `val c` from the `index`
     * entry in `rawArray` for the X coordinate, and `index+1` entry in `rawArray` for the Y
     * coordinate, and then adding `c` to `coordArrayList`. When done with all the `coordCount`
     * values in `rawArray` we return `coordArrayList` to the caller.
     *
     * @param rawArray : [x1,y1,x2,y2,...]
     * @return a [ArrayList] of [Coordinate]'s
     */
    private fun coordArrayToArrayList(rawArray: IntArray?): ArrayList<Coordinate?> {
        val coordArrayList = ArrayList<Coordinate?>()
        val coordCount: Int = rawArray!!.size
        var index = 0
        while (index < coordCount) {
            val c = Coordinate(x = rawArray[index], y = rawArray[index + 1])
            coordArrayList.add(c)
            index += 2
        }
        return coordArrayList
    }

    /**
     * Restore game state if our process is being relaunched. First we call our method [setMode]
     * to set the game mode to [PAUSE]. Then we set our [ArrayList] of [Coordinate] field
     * [mAppleList] to the list created by our method [coordArrayToArrayList] from the [IntArray]
     * stored in our [Bundle] parameter [icicle] under the key "mAppleList", set our field
     * [mDirection] to the [Int] stored under the key "mDirection", set our field [mNextDirection]
     * to the [Int] stored under the key "mNextDirection", set our field [mMoveDelay] to the
     * [Long] stored under the key "mMoveDelay", set our field [mScore] to the [Long] stored
     * under the key "mScore", and set our [ArrayList] of [Coordinate] field [mSnakeTrail] to
     * the list created by our method [coordArrayToArrayList] from the [IntArray] stored in [icicle]
     * under the key "mSnakeTrail".
     *
     * @param icicle a [Bundle] containing the game state
     */
    fun restoreState(icicle: Bundle) {
        setMode(PAUSE)
        mAppleList = coordArrayToArrayList(rawArray = icicle.getIntArray("mAppleList"))
        mDirection = icicle.getInt("mDirection")
        mNextDirection = icicle.getInt("mNextDirection")
        mMoveDelay = icicle.getLong("mMoveDelay")
        mScore = icicle.getLong("mScore")
        mSnakeTrail = coordArrayToArrayList(rawArray = icicle.getIntArray("mSnakeTrail"))
    }

    /**
     * Handles snake movement triggers from Snake Activity and moves the snake accordingly. Ignore
     * events that would cause the snake to immediately turn back on itself. We branch on the value
     * of our parameter `int direction`:
     *
     *  * [Snake.MOVE_UP]: If our game mode [gameState] is [READY] or [LOSE] we want to start a new
     *  game if the UP key is clicked so we call our [initNewGame] method to initialize us to a
     *  new game status, set our mode to [RUNNING], call our method [update] to update the snake's
     *  position and then we return. If our game mode [gameState] is [PAUSE] we want to
     *  just continue where we left off so we set our mode to [RUNNING], call our method [update]
     *  to update the snake's position and then we return. Otherwise if our current direction
     *  [mDirection] is not [SOUTH] we set our next direction [mNextDirection] to [NORTH] and in
     *  either case return.
     *
     *  * [Snake.MOVE_DOWN]: If our current direction [mDirection] is not [NORTH] we set our next
     *  direction [mNextDirection] to [SOUTH] and in either case return.
     *
     *  * [Snake.MOVE_LEFT]: If our current direction [mDirection] is not [EAST] we set our next
     *  direction [mNextDirection] to [WEST] and in either case return.
     *
     *  * [Snake.MOVE_RIGHT]: If our current direction [mDirection] is not [WEST] we set our next
     *  direction [mNextDirection] to [EAST] and in either case return.
     *
     * @param direction The desired direction of movement
     */
    fun moveSnake(direction: Int) {
        if (direction == Snake.MOVE_UP) {
            if ((gameState == READY) or (gameState == LOSE)) {
                /*
                 * At the beginning of the game, or the end of a previous one,
                 * we should start a new game if UP key is clicked.
                 */
                initNewGame()
                setMode(RUNNING)
                update()
                return
            }
            if (gameState == PAUSE) {
                /*
                 * If the game is merely paused, we should just continue where we left off.
                 */
                setMode(RUNNING)
                update()
                return
            }
            if (mDirection != SOUTH) {
                mNextDirection = NORTH
            }
            return
        }
        if (direction == Snake.MOVE_DOWN) {
            if (mDirection != NORTH) {
                mNextDirection = SOUTH
            }
            return
        }
        if (direction == Snake.MOVE_LEFT) {
            if (mDirection != EAST) {
                mNextDirection = WEST
            }
            return
        }
        if (direction == Snake.MOVE_RIGHT) {
            if (mDirection != WEST) {
                mNextDirection = EAST
            }
        }
    }

    /**
     * Sets the Dependent views that will be used to give information (such as "Game Over" to the
     * user and also to handle touch events for making movements. We just store our [TextView]
     * parameter [msgView] in our field [mStatusText], our [View] parameter [arrowView] in our field
     * [mArrowsView], and our [View] parameter [backgroundView] in our field [mBackgroundView].
     *
     * @param msgView view to use for our [TextView] field [mStatusText]
     * @param arrowView view to use for our [View] field [mArrowsView]
     * @param backgroundView view to use for our [View] field [mBackgroundView].
     */
    fun setDependentViews(msgView: TextView?, arrowView: View?, backgroundView: View?) {
        mStatusText = msgView
        mArrowsView = arrowView
        mBackgroundView = backgroundView
    }

    /**
     * Updates the current mode of the application ([RUNNING] or [PAUSE] or the like) as well as
     * sets the visibility of the [TextView] used for notification. First we save the current mode
     * [gameState] in our variable `val oldMode`, then we set the [gameState] mode to our [Int]
     * parameter [newMode]. If `newMode` is [RUNNING] and `oldMode` is not [RUNNING] we hide the
     * game instructions by setting the visibility of our [TextView] field [mStatusText] to INVISIBLE,
     * call our method [update] to update the snakes position, set the visibility of both our [View]
     * fields [mArrowsView] and [mBackgroundView] to visible and return.
     *
     * Otherwise we initialize our [Resources] variable `val res` to a [Resources] instance for the
     * application's package and initialize [CharSequence] variable `var str` to the empty string.
     * If `newMode` is [PAUSE] we set the visibility of our fields [mArrowsView] and [mBackgroundView]
     * to `GONE` and load `str` with the string whose resource id is `R.string.mode_pause` ("Paused
     * Press Up To Resume"). If `newMode` is [READY] we set the visibility of our fields [mArrowsView]
     * and [mBackgroundView] to `GONE` and load `str` with the string whose resource id is
     * `R.string.mode_ready` ("Snake Press Up To Play"). If `newMode` is [LOSE] we set the visibility
     * of our fields [mArrowsView] and [mBackgroundView]` to `GONE` and load `str` with the string
     * created by using the string with resource id `R.string.mode_lose` ("Game Over Score: [mScore]
     * Press Up To Play") to format our field [mScore]. In any case we then set the text of our
     * field [mStatusText] to `str` and set its visibility to `VISIBLE`.
     *
     * @param newMode new mode to transition to
     */
    fun setMode(newMode: Int) {
        val oldMode: Int = gameState
        gameState = newMode
        if (newMode == RUNNING && oldMode != RUNNING) {
            // hide the game instructions
            (mStatusText ?: return).visibility = INVISIBLE
            update()
            // make the background and arrows visible as soon the snake starts moving
            (mArrowsView ?: return).visibility = VISIBLE
            (mBackgroundView ?: return).visibility = VISIBLE
            return
        }
        val res: Resources = context.resources
        var str: CharSequence = ""
        if (newMode == PAUSE) {
            (mArrowsView ?: return).visibility = GONE
            (mBackgroundView ?: return).visibility = GONE
            str = res.getText(R.string.mode_pause)
        }
        if (newMode == READY) {
            (mArrowsView ?: return).visibility = GONE
            (mBackgroundView ?: return).visibility = GONE
            str = res.getText(R.string.mode_ready)
        }
        if (newMode == LOSE) {
            (mArrowsView ?: return).visibility = GONE
            (mBackgroundView ?: return).visibility = GONE
            str = res.getString(R.string.mode_lose, mScore)
        }
        (mStatusText ?: return).text = str
        (mStatusText ?: return).visibility = VISIBLE
    }

    /**
     * Selects a random location within the garden that is not currently covered by the snake.
     * Currently _could_ go into an infinite loop if the snake currently fills the garden, but we'll
     * leave discovery of this prize to a truly excellent snake-player. We initialize our [Coordinate]
     * variable `var newCoord` to `null`, and our [Boolean] variable `var found` to `false`. Then
     * we loop while `found` is false:
     *
     *  * We set [Int] `val newX` to a random X coordinate in the garden and [Int] `val newY` to
     *  a random Y coordinate in the garden and set `newCoord` to an instance constructed
     *  from them.
     *
     *  * We initialize [Boolean] variable `var collision` to `false` and [Int] variable
     *  `val snakeLength` to the length of our [ArrayList] of  field [Coordinate] field [mSnakeTrail]
     *  (the coordinates currently occupied by our snake).
     *
     *  * We then loop over [Int] variable `index` for all the `snakeLength` objects in [mSnakeTrail]
     *  setting `collision` to true if the [Coordinate] in position `index` of [mSnakeTrail] is equal
     *  to `newCoord`.
     *
     *  * When done with all the [Coordinate] objects occupied by the snake we set `found` to the
     *  inverse of `collision` and if this is now `true` we found a good location, otherwise we have
     *  to loop around to try the next random location.
     *
     * If `newCoord` is still null we log this "impossible" problem, in any case we add `newCoord`
     * to our list of apple locations: the [ArrayList] of [Coordinate] field [mAppleList].
     */
    private fun addRandomApple() {
        var newCoord: Coordinate? = null
        var found = false
        while (!found) {
            // Choose a new location for our apple
            val newX: Int = 1 + RNG.nextInt(/* bound = */ mXTileCount - 2)
            val newY: Int = 1 + RNG.nextInt(/* bound = */ mYTileCount - 2)
            newCoord = Coordinate(x = newX, y = newY)

            // Make sure it's not already under the snake
            var collision = false
            val snakeLength: Int = mSnakeTrail.size
            for (index: Int in 0 until snakeLength) {
                if ((mSnakeTrail[index] ?: return).equals(newCoord)) {
                    collision = true
                    break
                }
            }
            // if we're here and there's been no collision, then we have
            // a good location for an apple. Otherwise, we'll circle back
            // and try again
            found = !collision
        }
        @Suppress("KotlinConstantConditions")
        if (newCoord == null) {
            Log.e(TAG, "Somehow ended up with a null newCoord!")
        }
        mAppleList.add(newCoord)
    }

    /**
     * Handles the basic update loop, checking to see if we are in the running state, determining
     * if a move should be made, updating the snake's location. If our game mode [gameState] is
     * [RUNNING] we initialize our [Long] variable `val now` with the current system time in
     * milliseconds, and if `now` minus our [mLastMove] field is greater than our [mMoveDelay]
     * field we call our method [clearTiles] to set all tiles to 0 (empty), call our method
     * [updateWalls] to load the tiles around the border of the garden with the index of the wall
     * bitmap [GREEN_STAR]. We then call our method [updateSnake] to figure out which way the snake
     * is going, see if he's run into anything (the walls, himself, or an apple). If he's not going
     * to die, we then add to the front and subtract from the rear in order to simulate motion. If
     * we want to grow him, we don't subtract from the rear. We then call our method [updateApples]
     * "draw" the [YELLOW_STAR] apples in the list of apples in our [ArrayList] of [Coordinate]
     * field [mAppleList]. Then we set our field [mLastMove] to `now`. Whether it was time to move
     * or not we call the [RefreshHandler.sleep] method of our [mRedrawHandler] field to have it
     * schedule itself to run again in [mMoveDelay] milliseconds.
     */
    fun update() {
        if (gameState == RUNNING) {
            val now: Long = System.currentTimeMillis()
            if (now - mLastMove > mMoveDelay) {
                clearTiles()
                updateWalls()
                updateSnake()
                updateApples()
                mLastMove = now
            }
            mRedrawHandler.sleep(delayMillis = mMoveDelay)
        }
    }

    /**
     * Draws some walls. We loop over [Int] variable `x` for the [TileView.mXTileCount] tiles in the
     * X direction setting the tile at coordinate (x,0) to the [GREEN_STAR] bitmap, and the tile at
     * (x,mYTileCount-1) to the [GREEN_STAR] bitmap. Then we loop over [Int] variable  `y` for the
     * [TileView.mYTileCount] tiles in the Y direction setting the tile at coordinate (0,y) to the
     * [GREEN_STAR] bitmap, and the tile at (mXTileCount-1,y) to the [GREEN_STAR] bitmap.
     */
    private fun updateWalls() {
        for (x: Int in 0 until mXTileCount) {
            setTile(tileIndex = GREEN_STAR, x = x, y = 0)
            setTile(tileIndex = GREEN_STAR, x = x, y = mYTileCount - 1)
        }
        for (y: Int in 1 until mYTileCount - 1) {
            setTile(tileIndex = GREEN_STAR, x = 0, y = y)
            setTile(tileIndex = GREEN_STAR, x = mXTileCount - 1, y = y)
        }
    }

    /**
     * Draws some apples. We loop for all the [Coordinate] variable `c` in our [ArrayList] of
     * [Coordinate] field [mAppleList] setting the tile located at the [Coordinate.x] field of `c`
     * and the [Coordinate.y] field of `c` to the [YELLOW_STAR] bitmap.
     */
    private fun updateApples() {
        for (c: Coordinate? in mAppleList) {
            setTile(tileIndex = YELLOW_STAR, x = (c ?: return).x, y = c.y)
        }
    }

    /**
     * Figure out which way the snake is going, see if he's run into anything (the walls, himself,
     * or an apple). If he's not going to die, we then add to the front and subtract from the rear
     * in order to simulate motion. If we want to grow him, we don't subtract from the rear.
     *
     * First we initialize [Boolean] variable `var growSnake` to `false`. We initialize [Coordinate]
     * variable `val head` with the [Coordinate] at index 0 in the [ArrayList] of [Coordinate]
     * field [mSnakeTrail], and initialize [Coordinate] variable `var newHead` with an instance
     * located at (1,1). We then set our [mDirection] field to our [mNextDirection] field and `when`
     * switch on the value of [mDirection]:
     *
     *  * [EAST]: we set `newHead` to a new instance of [Coordinate] located at
     *  (`head.x+1`,`head.y`).
     *
     *  * [WEST]: we set `newHead` to a new instance of [Coordinate] located at
     *  (`head.x-1`,`head.y`).
     *
     *  * [NORTH]: we set `newHead` to a new instance of [Coordinate] located at
     *  (`head.x`,`head.y-1`).
     *
     *  * [SOUTH]: we set `newHead` to a new instance of [Coordinate] located at
     *  (`head.x`,`head.y+1`).
     *
     * Now we check to see if we collided into a wall by checking whether the `x` field of
     * `newHead` is less 1, or the `y` field is less than 1, or the `x` field is greater than
     * [TileView.mXTileCount] minus 2, or the `y` field is greater than [TileView.mYTileCount]
     * minus 2, and if so then we set our game mode to [LOSE] and return.
     *
     * If we did not run into a wall we now look for collisions with itself by initializing [Int]
     * variable `val snakeLength` to the length of [mSnakeTrail] then looping over [Int] variable
     * `snakeIndex` for the `snakeLength` [Coordinate]'s in [mSnakeTrail] fetching each to the
     * [Coordinate] variable `val c` in turn and if it is equal to `newHead` we set our game mode
     * to [LOSE] and return.
     *
     * If we are still alive we check to see if we just ate an apple by initializing [Int] variable
     * `val appleCount` to the length of [mAppleList] then looping over [Int] variable `appleIndex`
     * for the `appleCount` [Coordinate]'s in [mAppleList] fetching each [Coordinate] to variable
     * `val c` in turn and if it is equal to `newHead` we remove `c` from [mAppleList], call our
     * method [addRandomApple] to add another randomly placed apple to the 'garden', increment our
     * score [mScore], multiply [mMoveDelay] by 0.9 and set `growSnake` to true.
     *
     * When done checking the apples we add `newHead` to [mSnakeTrail] and if `growSnake` is `false`
     * we remove the [Coordinate] at the end of [mSnakeTrail] (if it is `true` the snake grows larger
     * by keeping the last segment).
     *
     * Now we need to set the tile bitmaps representing [mSnakeTrail] to the correct colored one,
     * and we do this by using the [withIndex] extension function to set our [Int] variable `index`
     * to the index of the [Coordinate] in [mSnakeTrail] and our [Coordinate] variable `c` to the
     * [Coordinate] at that index in [mSnakeTrail] and looping over all the [Coordinate]'s in
     * [mSnakeTrail]:
     *
     *  * if `index` is 0 we call our [setTile] method to set the head located at the [Coordinate.x]
     *  and [Coordinate.y] field of `c` to the [YELLOW_STAR] bitmap.
     *
     *  * otherwise we call our [setTile] method to set the segment located at the [Coordinate.x]
     *  and [Coordinate.y] field of `c` to the [RED_STAR] bitmap.
     *
     * The [withIndex] method then loops around for the next [Coordinate] in [mSnakeTrail].
     */
    private fun updateSnake() {
        var growSnake = false

        // Grab the snake by the head
        val head: Coordinate? = mSnakeTrail[0]
        var newHead = Coordinate(x = 1, y = 1)
        mDirection = mNextDirection
        when (mDirection) {
            EAST -> newHead = Coordinate(x = (head ?: return).x + 1, y = head.y)
            WEST -> newHead = Coordinate(x = (head ?: return).x - 1, y = head.y)
            NORTH -> newHead = Coordinate(x = (head ?: return).x, y = head.y - 1)
            SOUTH -> newHead = Coordinate(x = (head ?: return).x, y = head.y + 1)
        }

        // Collision detection
        // For now we have a 1-square wall around the entire arena
        if (newHead.x < 1 || newHead.y < 1 || newHead.x > mXTileCount - 2 || newHead.y > mYTileCount - 2) {
            setMode(LOSE)
            return
        }

        // Look for collisions with itself
        val snakeLength: Int = mSnakeTrail.size
        for (snakeIndex: Int in 0 until snakeLength) {
            val c: Coordinate? = mSnakeTrail[snakeIndex]
            if ((c ?: return).equals(newHead)) {
                setMode(LOSE)
                return
            }
        }

        // Look for apples
        val appleCount: Int = mAppleList.size
        for (appleIndex: Int in 0 until appleCount) {
            val c: Coordinate? = mAppleList[appleIndex]
            if ((c ?: return).equals(newHead)) {
                mAppleList.remove(c)
                addRandomApple()
                mScore++
                mMoveDelay = (mMoveDelay.toDouble() * 0.9).toLong()
                growSnake = true
            }
        }

        // push a new head onto the ArrayList and pull off the tail
        mSnakeTrail.add(/* index = */ 0, /* element = */ newHead)
        // except if we want the snake to grow
        if (!growSnake) {
            mSnakeTrail.removeAt(/* index = */ mSnakeTrail.size - 1)
        }
        for ((index: Int, c: Coordinate?) in mSnakeTrail.withIndex()) {
            if (index == 0) {
                setTile(tileIndex = YELLOW_STAR, x = (c ?: return).x, y = c.y)
            } else {
                setTile(tileIndex = RED_STAR, x = (c ?: return).x, y = c.y)
            }
        }
    }

    /**
     * Simple class containing two integer values and a comparison function. There's probably
     * something I should use instead, but this was quick and easy to build.
     *
     *  @param x X coordinate of the [Coordinate]
     *  @param y Y coordinate of the [Coordinate]
     */
    private class Coordinate(
        var x: Int,
        var y: Int
    ) {
        /**
         * Compares ourselves to our [Coordinate] parameter [other] by checking that the `x`
         * and `y` fields of the two are equal.
         *
         * @param other the [Coordinate] we are comparing ourselves to.
         * @return `true` if the two [Coordinate] objects have the same (x,y) location.
         */
        @Suppress("CovariantEquals") // Calling `equals` with an `Any` should crash!
        fun equals(other: Coordinate): Boolean {
            return x == other.x && y == other.y
        }

        /**
         * Creates and returns a [String] displaying the values of our fields `x` and
         * `y`.
         *
         * @return a [String] displaying the values of our fields `x` and `y`
         */
        override fun toString(): String {
            return "Coordinate: [$x,$y]"
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "SnakeView"

        /**
         * Game is in paused mode.
         */
        const val PAUSE: Int = 0

        /**
         * We are at the beginning of a game, ready to run.
         */
        const val READY: Int = 1

        /**
         * Game is running.
         */
        const val RUNNING: Int = 2

        /**
         * We have already lost.
         */
        const val LOSE: Int = 3

        /**
         * The UP direction.
         */
        private const val NORTH = 1

        /**
         * The DOWN direction.
         */
        private const val SOUTH = 2

        /**
         * The RIGHT direction.
         */
        private const val EAST = 3

        /**
         * The LEFT direction.
         */
        private const val WEST = 4

        // Labels for the indices of the drawables that will be loaded into the TileView class

        /**
         * Used for the body behind the head of the snake
         */
        private const val RED_STAR = 1

        /**
         * Used for the head of the snake and the apples
         */
        private const val YELLOW_STAR = 2

        /**
         * Used for the walls that you don't want the snake to run into
         */
        private const val GREEN_STAR = 3

        /**
         * Everyone needs a little randomness in their life
         */
        private val RNG = Random()
    }
}
