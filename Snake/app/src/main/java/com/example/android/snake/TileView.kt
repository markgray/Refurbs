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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.snake

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.createBitmap

/**
 * [TileView]: a [View]-variant designed for handling arrays of "icons" or other drawables, it is
 * used as the base class of [SnakeView].
 */
open class TileView : View {
    /**
     * [Paint] we use to draw our tiles.
     */
    private val mPaint = Paint()

    /**
     * A hash that maps integer handles specified by the subclass to the drawable that will be
     * used for that reference
     */
    private lateinit var mTileArray: Array<Bitmap?>

    /**
     * Two-dimensional array of integers representing the game board, with the outer array addressing
     * the `x` coordinate, the inner array addressing the `y` coordinate, and the content of the inner
     * array holding the index of the [Bitmap] in [Array] of [Bitmap] field [mTileArray] that should
     * be drawn at that location.
     */
    private lateinit var mTileGrid: Array<IntArray>

    /**
     * Constructor that is called when inflating a view from XML. First we call our super's constructor,
     * then we use the [Context.withStyledAttributes] extension function to fill a [TypedArray] with
     * the attribute values that are listed in our [AttributeSet] parameter [attrs] that are
     * defined in the style file `R.styleable.TileView`. That [TypedArray] is then the receiver of
     * a lambda block that uses the [TypedArray.getDimensionPixelSize] method to retrieve the value
     * for the attribute `R.styleable.TileView_tileSize` (defaulting to 12) which its uses to
     * initialize [Int] field [mTileSize] ([Context.withStyledAttributes] will recycle the
     * [TypedArray] when the lambda returns).
     *
     * @param context The Context the view is running in, through which it can
     * access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        context.withStyledAttributes(set = attrs, attrs = R.styleable.TileView) {
            mTileSize = getDimensionPixelSize(R.styleable.TileView_tileSize, 12)
        }
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute. First
     * we call our super's constructor, then we use the [Context.withStyledAttributes] extension
     * function to fill a [TypedArray] with the attribute values that are listed in our [AttributeSet]
     * parameter [attrs] that are defined in the style file `R.styleable.TileView`. That [TypedArray]
     * is then the receiver of a lambda block that uses the [TypedArray.getDimensionPixelSize] method
     * to retrieve the value for the attribute `R.styleable.TileView_tileSize` (defaulting to 12) which
     * its uses to initialize [Int] field [mTileSize] ([Context.withStyledAttributes] will recycle the
     * [TypedArray] when the lambda returns).
     *
     * @param context  The [Context] the view is running in, through which it can access the current
     * theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style resource
     * that supplies default values for the view. Can be 0 to not look for defaults.
     * We ignore.
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        context.withStyledAttributes(set = attrs, attrs = R.styleable.TileView) {
            mTileSize = getDimensionPixelSize(R.styleable.TileView_tileSize, 12)
        }
    }

    /**
     * Resets all tiles to 0 (empty). We loop over [Int] variable `x` for all the tiles in the X
     * direction specified by our field [mXTileCount], and in an inner loop we loop over [Int]
     * variable `y` for all the tiles in the Y direction specified by our field [mYTileCount] calling
     * our method [setTile] to set the index for the [Bitmap] in our array of [Bitmap] field
     * [mTileArray] used for the tile at `(x,y)` to 0.
     */
    fun clearTiles() {
        for (x: Int in 0 until mXTileCount) {
            for (y: Int in 0 until mYTileCount) {
                setTile(tileIndex = 0, x = x, y = y)
            }
        }
    }

    /**
     * Function to set the specified [Drawable] as the tile for a particular integer key. We
     * initialize our [Bitmap] variable `val bitmap` with an instance that is [mTileSize] by
     * [mTileSize], and then initialize our [Canvas] variable `val canvas` with an instance which
     * will draw into `bitmap`. We set the bounding rectangle of our [Drawable] parameter [tile]
     * to be [mTileSize] by [mTileSize] with the upper corner at (0,0) then have it draw itself
     * into `canvas`. Finally we set the contents of the [key] entry in [mTileArray] to `bitmap`.
     *
     * @param key key of the tile in the array of [Bitmap] field [mTileArray] whose bitmap we
     * are setting.
     * @param tile the [Drawable] to set the tile to.
     */
    fun loadTile(key: Int, tile: Drawable) {
        val bitmap = createBitmap(width = mTileSize, height = mTileSize)
        val canvas = Canvas(bitmap)
        tile.setBounds(
            /* left = */ 0,
            /* top = */ 0,
            /* right = */ mTileSize,
            /* bottom = */ mTileSize
        )
        tile.draw(canvas)
        mTileArray[key] = bitmap
    }

    /**
     * We implement this to do our drawing. First we call our super's implementation of `onDraw`.
     * Then we loop over the [Int] variable `x` for the [mXTileCount] tiles in the X direction and
     * in an inner loop we loop over the [Int] variable `y` for the [mYTileCount] tiles in the Y
     * direction, skipping all the [mTileGrid]'s in location (`x`,`y`) whose contents are not greater
     * than 0, and for those whose contents are greater than 0 we draw the [Bitmap] contained in the
     * array of [Bitmap] field [mTileArray] whose index is specified by the contents of the (`x`,`y`)
     * entry in [mTileGrid] at the X coordinate of [mXOffset] plus `x` times [mTileSize], and the
     * Y coordinate of [mYOffset] plus `y` times [mTileSize], using [mPaint] as the [Paint].
     *
     * @param canvas the [Canvas] on which the background will be drawn
     */
    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var x = 0
        while (x < mXTileCount) {
            var y = 0
            while (y < mYTileCount) {
                if (mTileGrid[x][y] > 0) {
                    canvas.drawBitmap(
                        /* bitmap = */ mTileArray[mTileGrid[x][y]]!!,
                        /* left = */ (mXOffset + x * mTileSize).toFloat(),
                        /* top = */ (mYOffset + y * mTileSize).toFloat(),
                        /* paint = */ mPaint
                    )
                }
                y += 1
            }
            x += 1
        }
    }

    /**
     * Resets the internal array of [Bitmap]'s used for drawing tiles, and sets the maximum index of
     * tiles to be inserted. We just allocate our [Int] parameter [tileCount] entries for the array
     * array of [Bitmap] field [mTileArray].
     *
     * @param tileCount number of tiles to use for our internal array of Bitmap
     */
    fun resetTiles(tileCount: Int) {
        mTileArray = arrayOfNulls(size = tileCount)
    }

    /**
     * Used to indicate that a particular tile (set with [loadTile] and referenced by an integer)
     * should be drawn at the given x/y coordinates during the next invalidate/draw cycle. We just
     * set the value of the (`x`,`y`) entry in our [mTileGrid] field to our parameter [tileIndex].
     *
     * @param tileIndex index into the array of [Bitmap] field [mTileArray] of the [Bitmap] to use
     * @param x X coordinate of the tile
     * @param y Y coordinate of the tile
     */
    fun setTile(tileIndex: Int, x: Int, y: Int) {
        mTileGrid[x][y] = tileIndex
    }

    /**
     * This is called during layout when the size of this view has changed. We set our field
     * [mXTileCount] to our parameter [w] divided by [mTileSize] rounded down to the nearest [Int],
     * and our field [mYTileCount] to our parameter [h] divided by [mTileSize] rounded down to the
     * nearest [Int]. We set our field [mXOffset] to our parameter [w] minus half of [mTileSize]
     * times [mXTileCount] and our field [mYOffset] to our parameter [h] minus half of [mTileSize]
     * times [mYTileCount]. We then initialize our "array of array of [Int]" field [mTileGrid] to
     * an [mXTileCount] by [mYTileCount] array and call our method [clearTiles] to set all of its
     * entries to 0.
     *
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog") // Using kotlin methods here causes BUG
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mXTileCount = Math.floor((w.toDouble() / mTileSize.toDouble())).toInt()
        mYTileCount = Math.floor((h.toDouble() / mTileSize.toDouble())).toInt()
        mXOffset = (w - mTileSize * mXTileCount) / 2
        mYOffset = (h - mTileSize * mYTileCount) / 2
        mTileGrid = Array(size = mXTileCount) { IntArray(size = mYTileCount) }
        clearTiles()
    }

    companion object {
    /*
     * Parameters controlling the size of the tiles and their range within view. Width/Height are in
     * pixels, and Drawables will be scaled to fit to these dimensions. X/Y Tile Counts are the
     * number of tiles that will be drawn.
     */
        /**
         * Size of the square tile in pixels, controlled by the attribute `R.styleable.TileView_tileSize`
         * (defaulting to 12) set to 24dp in our layout file.
         */
        @JvmStatic
        protected var mTileSize: Int = 0

        /**
         * Number of tiles in the X direction
         */
        @JvmStatic
        protected var mXTileCount: Int = 0

        /**
         * Number of tiles in the Y direction
         */
        @JvmStatic
        protected var mYTileCount: Int = 0

        /**
         * Offset from the left side of the screen to begin drawing tiles in pixels.
         */
        @JvmStatic
        private var mXOffset: Int = 0

        /**
         * Offset from the top of the screen to begin drawing tiles in pixels.
         */
        @JvmStatic
        private var mYOffset: Int = 0
    }
}
