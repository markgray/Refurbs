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
@file:Suppress("ReplaceNotNullAssertionWithElvisReturn")

package com.example.android.mediaeffects

import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.effect.Effect
import android.media.effect.EffectContext
import android.media.effect.EffectFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * `Fragment` which implements our demo.
 */
class MediaEffectsFragment : Fragment(), GLSurfaceView.Renderer {
    /**
     * `GLSurfaceView` which we render to.
     */
    private var mEffectView: GLSurfaceView? = null

    /**
     * Generated texture names we use, `mTextures[0]` for the original bitmap, `mTextures[1]`
     * for the result of `applyEffect()`.
     */
    private val mTextures = IntArray(2)

    /**
     * keeps all necessary state information to run Effects within a single Open GL ES 2.0 context.
     */
    private var mEffectContext: EffectContext? = null

    /**
     * Current `Effect`, Effects are high-performance transformations that can be applied to
     * image frames via the use of textures which have had the transformation applied to them.
     */
    private var mEffect: Effect? = null

    /**
     * Our instance of `TextureRenderer`, its methods do all our rendering for us.
     */
    private val mTexRenderer: TextureRenderer = TextureRenderer()

    /**
     * Width of our bitmap R.drawable.puppy: drawable-nodpi/puppy.jpg
     */
    private var mImageWidth = 0

    /**
     * Height of our bitmap R.drawable.puppy: drawable-nodpi/puppy.jpg
     */
    private var mImageHeight = 0

    /**
     * Flag to indicate whether our `onDrawFrame` override has been called at least once so
     * it knows to only do its initialization duties the first time it is called.
     */
    private var mInitialized = false

    /**
     * Item id of the effect selected by the user using the options menu.
     */
    private var mCurrentEffect = 0

    /**
     * Called to do initial creation of a fragment. We call our super's implementation of `onCreate`
     * then we call the `setHasOptionsMenu(true)` method to report that this fragment would like to
     * participate in populating the options menu by receiving a call to [.onCreateOptionsMenu] and
     * related methods.
     *
     * @param savedInstanceState we wait until our `onViewCreated` override is called to use this
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * Called to have the fragment instantiate its user interface view. We return the view created by
     * using our parameter `LayoutInflater inflater` to inflate our layout file R.layout.fragment_media_effects
     * using our parameter `ViewGroup container` for the layout params without attaching to it.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_effects, container, false)
    }

    /**
     * Called immediately after [.onCreateView] has returned,
     * but before any saved state has been restored in to the view. We initialize our field
     * `GLSurfaceView mEffectView` by finding the view in our parameter `View view` with
     * id R.id.effects_view, then we set its EGLContext client version to use to 2 in order to use
     * OpenGL ES 2.0, set its renderer to this, and set its rendermode to RENDERMODE_WHEN_DIRTY.
     * If our parameter `Bundle savedInstanceState` is not null we call our `setCurrentEffect`
     * method to set our field `mCurrentEffect` to the int stored in `savedInstanceState`
     * under the key STATE_CURRENT_EFFECT ("current_effect"), otherwise we call `setCurrentEffect`
     * with R.id.none.
     *
     * @param view The View returned by [.onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mEffectView = view.findViewById(R.id.effects_view)
        mEffectView!!.setEGLContextClientVersion(2)
        mEffectView!!.setRenderer(this)
        mEffectView!!.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        if (null != savedInstanceState && savedInstanceState.containsKey(STATE_CURRENT_EFFECT)) {
            setCurrentEffect(savedInstanceState.getInt(STATE_CURRENT_EFFECT))
        } else {
            setCurrentEffect(R.id.none)
        }
    }

    /**
     * Initialize the contents of the Fragment host's standard options menu. We use our parameter
     * `MenuInflater inflater` to inflate our menu layout file R.menu.media_effects into our
     * parameter `Menu menu`.
     *
     * @param menu The options menu in which you place your items.
     * @param inflater `MenuInflater` you can use to inflate xml menu layout files.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.media_effects, menu)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We call our method
     * `setCurrentEffect` to set our field `mCurrentEffect` to the item id of our parameter
     * `MenuItem item`, then we call the `requestRender` method of our field
     * `GLSurfaceView mEffectView` to request that the renderer render a frame. Finally we return
     * true to consume the event here.
     *
     * @param item The menu item that was selected.
     * @return We return true to consume the event here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        setCurrentEffect(item.itemId)
        mEffectView!!.requestRender()
        return true
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it can later be reconstructed
     * in a new instance of its process is restarted. We store our field `int mCurrentEffect`
     * under the key STATE_CURRENT_EFFECT ("current_effect") in our parameter `Bundle outState`
     * (it will be restored by our `onViewCreated` override when our fragment is restarted).
     *
     * @param outState Bundle in which to place your saved state.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_CURRENT_EFFECT, mCurrentEffect)
    }

    /**
     * Called when the surface is created or recreated. We ignore
     *
     * @param gl the GL interface. Use `instanceof` to
     * test if the interface supports GL11 or higher interfaces.
     * @param eglConfig the EGLConfig of the created surface. Can be used
     * to create matching pbuffers.
     */
    override fun onSurfaceCreated(gl: GL10, eglConfig: EGLConfig) {
        // Nothing to do here
    }

    /**
     * Called when the surface changed size. If our field `TextureRenderer mTexRenderer` is not
     * null we call its `updateViewSize` method with the new width and height.
     *
     * @param gl the GL interface. Use `instanceof` to
     * test if the interface supports GL11 or higher interfaces.
     * @param width new width
     * @param height new height
     */
    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        mTexRenderer.updateViewSize(width, height)
    }

    /**
     * Called to draw the current frame. If our field `boolean mInitialized` is false, this is
     * the first time we have been called so we have to initialize our field `EffectContext mEffectContext`
     * with a context within the current GL context created by the method `createWithCurrentGlContext`.
     * We then call the `init` method of our field `TextureRenderer mTexRenderer` to initialize
     * the OpenGL graphics engine to draw for us. Then we call our method `loadTextures` to allocate
     * two texture names for our field `int[] mTextures` load our image file R.drawable.puppy
     * (drawable-nodpi/puppy.jpg) into the texture named by `mTextures[0]` and do some other initialization
     * necessary to use it as a texture. We then set our field `mInitialized` to true so we do not
     * do this initialization the next time we are called. Whether it is the first or a subsequent call,
     * if our field `mCurrentEffect` is not R.id.none, we call our method `initEffect` to
     * initialize the chosen effect, and then call our method `applyEffect` to apply the effect
     * to the texture with texture name `mTextures[0]` (our unmodified bitmap) to create a new
     * texture for the texture name `mTextures[1]`. In either case we then call our method
     * `renderResult` to draw our frame using the appropriate texture name.
     *
     * @param gl the GL interface. Use `instanceof` to
     * test if the interface supports GL11 or higher interfaces.
     */
    override fun onDrawFrame(gl: GL10) {
        if (!mInitialized) {
            //Only need to do this once
            mEffectContext = EffectContext.createWithCurrentGlContext()
            mTexRenderer.init()
            loadTextures()
            mInitialized = true
        }
        if (mCurrentEffect != R.id.none) {
            //if an effect is chosen initialize it and apply it to the texture
            initEffect()
            applyEffect()
        }
        renderResult()
    }

    /**
     * Setter for our field `int mCurrentEffect`.
     *
     * @param effect one of the 24 possible effects (including R.id.none)
     */
    private fun setCurrentEffect(effect: Int) {
        mCurrentEffect = effect
    }

    /**
     * Creates two texture names, uploads a bitmap to one of the textures, and sets the texture
     * parameters. First we call the `glGenTextures` method to generate 2 texture names for
     * our field `int[] mTextures`. We initialize `Bitmap bitmap` by decoding the jpg
     * with resource id R.drawable.puppy, set our field `mImageWidth` to the width of
     * `bitmap` and `mImageHeight` to the height of `bitmap`, then call the
     * `updateTextureSize` to update it with the new size. We call `glBindTexture` to
     * bind the texture named `mTextures[0]` to GL_TEXTURE_2D (texture targets become aliases
     * for the textures currently bound to them). We then call the `GLUtils.texImage2D` method
     * to upload `bitmap` into GL_TEXTURE_2D (alias for `mTextures[0]` recall). Finally
     * we call the `GLToolbox.initTexParams` method to set the texture parameters as we want them.
     */
    private fun loadTextures() {
        // Generate textures
        GLES20.glGenTextures(2, mTextures, 0)

        // Load input bitmap
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.puppy)
        mImageWidth = bitmap.width
        mImageHeight = bitmap.height
        mTexRenderer.updateTextureSize(mImageWidth, mImageHeight)

        // Upload to texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0])
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        // Set texture parameters
        GLToolbox.initTexParams()
    }

    /**
     * Creates and initializes our field `Effect mEffect` for the effect chosen by our field
     * `int mCurrentEffect`. We use the `getFactory` method of our field
     * `EffectContext mEffectContext` to initialize `EffectFactory effectFactory` with
     * the EffectFactory for that context. If our field `Effect mEffect` is not null we call
     * its `release` method to release the effect and any resources associated with it. Then
     * we switch on the value or our field `int mCurrentEffect`:
     *
     *  *
     * R.id.none: we just break having done nothing
     *
     *  *
     * R.id.auto_fix: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_AUTOFIX (Attempts to auto-fix the image based on
     * histogram equalization). We then set the parameter of `mEffect` with the key
     * "scale" to 0.5 (scale of the adjustment, Zero means no adjustment, while 1 indicates
     * the maximum amount of adjustment). Then we break.
     *
     *  *
     * R.id.bw: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_BLACKWHITE (Adjusts the range of minimal and maximal
     * color pixel intensities). We then set the parameter of `mEffect` with the key
     * "black" to .1f (value of the minimal pixel) and the parameter of `mEffect` with
     * the key "white" to .7f (value of the maximal pixel). Then we break.
     *
     *  *
     * R.id.brightness: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_BRIGHTNESS (Adjusts the brightness of the image).
     * We then set the parameter of `mEffect` with the key "brightness" to 2.0f (The
     * brightness multiplier, 1.0 means no change, larger values will increase brightness).
     * Then we break.
     *
     *  *
     * R.id.contrast: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_CONTRAST (Adjusts the contrast of the image). We
     * then set the parameter of `mEffect` with the key "contrast" to 1.4f (The
     * contrast multiplier, 1.0 means no change larger values will increase contrast).
     * Then we break.
     *
     *  *
     * R.id.crossprocess: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_CROSSPROCESS (Applies a cross process effect on image,
     * in which the red and green channels are enhanced while the blue channel is restricted).
     * Then we break.
     *
     *  *
     * R.id.documentary: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_DOCUMENTARY (Applies black and white documentary style
     * effect on image). Then we break.
     *
     *  *
     * R.id.duotone: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_DUOTONE (Representation of photo using only two color
     * tones). We then set the parameter of `mEffect` with the key "first_color" to
     * YELLOW (first color tone), and the parameter of `mEffect` with the key "second_color"
     * to DKGRAY (second color tone). Then we break.
     *
     *  *
     * R.id.filllight: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_FILLLIGHT (Applies back-light filling to the image).
     * We then set the parameter of `mEffect` with the key "strength" to .8f (strength
     * of the backlight, between 0 and 1, Zero means no change). Then we break.
     *
     *  *
     * R.id.fisheye: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_FISHEYE (Applies a fisheye lens distortion to the
     * image). We then set the parameter of `mEffect` with the key "scale" to .5f
     * (scale of the distortion, between 0 and 1, Zero means no distortion). Then we break.
     *
     *  *
     * R.id.flipvert: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_FLIP (Flips image vertically and/or horizontally).
     * We then set the parameter of `mEffect` with the key "vertical" to true (Whether
     * to flip image vertically). Then we break.
     *
     *  *
     * R.id.fliphor: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_FLIP (Flips image vertically and/or horizontally).
     * We then set the parameter of `mEffect` with the key "horizontal" to true (Whether
     * to flip image horizontally). Then we break.
     *
     *  *
     * R.id.grain: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_GRAIN (Applies film grain effect to image). We then
     * set the parameter of `mEffect` with the key "strength" to 1.0f (strength of the
     * grain effect, between 0 and 1, Zero means no change). Then we break.
     *
     *  *
     * R.id.grayscale: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_GRAYSCALE (Converts image to grayscale). Then we break.
     *
     *  *
     * R.id.lomoish: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_LOMOISH (Applies lomo-camera style effect to image,
     * effect is inspired by photographs taken from an inexpensive Russian camera called the
     * Lomo LC-A. The photos produced by Lomo carry high-contrast, increased saturation, and
     * unique coloring due to “improper” color reproduction and dark blurry edges with a sharp
     * center). Then we break.
     *
     *  *
     * R.id.negative: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_NEGATIVE (Inverts the image colors). Then we break.
     *
     *  *
     * R.id.posterize: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_POSTERIZE (Applies posterization effect to image).
     * Then we break.
     *
     *  *
     * R.id.rotate: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_ROTATE (Rotates the image, snaps to nearest 90 degrees).
     * We then set the parameter of `mEffect` with the key "angle" to 180 (angle of
     * rotation in degrees, will be rounded to the nearest multiple of 90). Then we break.
     *
     *  *
     * R.id.saturate: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_SATURATE (Adjusts color saturation of image).
     * We then set the parameter of `mEffect` with the key "scale" to .5f (scale of
     * color saturation, between -1 and 1. 0 means no change, while -1 indicates full desaturation).
     * Then we break.
     *
     *  *
     * R.id.sepia: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_SEPIA (Converts image to sepia tone, Sepia toning is
     * a specialized treatment to give a black-and-white photographic print a warmer tone and
     * to enhance its archival qualities). Then we break.
     *
     *  *
     * R.id.sharpen: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_SHARPEN (Sharpens the image). Then we break.
     *
     *  *
     * R.id.temperature: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_TEMPERATURE (Adjusts color temperature of the image).
     * We then set the parameter of `mEffect` with the key "scale" to .9f (value of color
     * temperature, between 0 and 1, with 0 indicating cool, and 1 indicating warm. A value of
     * of 0.5 indicates no change). Then we break.
     *
     *  *
     * R.id.tint: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_TINT (Tints the photo with specified color).
     * We then set the parameter of `mEffect` with the key "tint" to MAGENTA.
     * Then we break.
     *
     *  *
     * R.id.vignette: we set our field `Effect mEffect` to the effect produced by
     * `effectFactory` for EFFECT_VIGNETTE (Adds a vignette effect to image, i.e. fades
     * away the outer image edges). We then set the parameter of `mEffect` with the key
     * "scale" to .5f (scale of vignetting, between 0 and 1. 0 means no change). Then we break.
     *
     *  *
     * default: we just break.
     *
     *
     */
    private fun initEffect() {
        val effectFactory = mEffectContext!!.factory
        if (mEffect != null) {
            mEffect!!.release()
        }
        when (mCurrentEffect) {
            R.id.none -> {}
            R.id.auto_fix -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX)
                mEffect!!.setParameter("scale", 0.5f)
            }

            R.id.bw -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_BLACKWHITE)
                mEffect!!.setParameter("black", .1f)
                mEffect!!.setParameter("white", .7f)
            }

            R.id.brightness -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_BRIGHTNESS)
                mEffect!!.setParameter("brightness", 2.0f)
            }

            R.id.contrast -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_CONTRAST)
                mEffect!!.setParameter("contrast", 1.4f)
            }

            R.id.crossprocess -> mEffect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS)
            R.id.documentary -> mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY)
            R.id.duotone -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE)
                mEffect!!.setParameter("first_color", Color.YELLOW)
                mEffect!!.setParameter("second_color", Color.DKGRAY)
            }

            R.id.filllight -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FILLLIGHT)
                mEffect!!.setParameter("strength", .8f)
            }

            R.id.fisheye -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FISHEYE)
                mEffect!!.setParameter("scale", .5f)
            }

            R.id.flipvert -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FLIP)
                mEffect!!.setParameter("vertical", true)
            }

            R.id.fliphor -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FLIP)
                mEffect!!.setParameter("horizontal", true)
            }

            R.id.grain -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_GRAIN)
                mEffect!!.setParameter("strength", 1.0f)
            }

            R.id.grayscale -> mEffect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE)
            R.id.lomoish -> mEffect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH)
            R.id.negative -> mEffect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE)
            R.id.posterize -> mEffect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE)
            R.id.rotate -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_ROTATE)
                mEffect!!.setParameter("angle", 180)
            }

            R.id.saturate -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SATURATE)
                mEffect!!.setParameter("scale", .5f)
            }

            R.id.sepia -> mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA)
            R.id.sharpen -> mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SHARPEN)
            R.id.temperature -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE)
                mEffect!!.setParameter("scale", .9f)
            }

            R.id.tint -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_TINT)
                mEffect!!.setParameter("tint", Color.MAGENTA)
            }

            R.id.vignette -> {
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE)
                mEffect!!.setParameter("scale", .5f)
            }

            else -> {}
        }
    }

    /**
     * Applies the `Effect` contained in our field `Effect mEffect` to the texture whose
     * name is in `mTextures[0]` saving the output in the texture whose name is in `mTextures[1]`,
     * using `mImageWidth` as the input texture width and `mImageHeight` as the input texture
     * height.
     */
    private fun applyEffect() {
        mEffect!!.apply(mTextures[0], mImageWidth, mImageHeight, mTextures[1])
    }

    /**
     * If the current effect ID in `mCurrentEffect` is not R.id.none we call the `renderTexture`
     * method of our field `TextureRenderer mTexRenderer` with `mTextures[1]` as the texture to
     * use (the texture which contains the result of applying the current effect), otherwise we call the
     * `renderTexture` method with `mTextures[0]` as the texture (the original bitmap).
     */
    private fun renderResult() {
        if (mCurrentEffect != R.id.none) {
            // render the result of applyEffect()
            mTexRenderer.renderTexture(mTextures[1])
        } else {
            // if no effect is chosen, just render the original bitmap
            mTexRenderer.renderTexture(mTextures[0])
        }
    }

    companion object {
        /**
         * Key under which the current media effect `mCurrentEffect` is stored in the `Bundle`
         * passed to our `onSaveInstanceState` override, and from which it is restored again in our
         * `onViewCreated` if we are being recreated from a previous state.
         */
        private const val STATE_CURRENT_EFFECT = "current_effect"
    }
}