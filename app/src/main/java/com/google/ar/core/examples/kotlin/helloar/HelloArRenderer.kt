/*
 * Copyright 2021 Google LLC
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
package com.google.ar.core.examples.kotlin.helloar

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper
import com.google.ar.core.examples.java.common.samplerender.Framebuffer
import com.google.ar.core.examples.java.common.samplerender.GLError
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.java.common.samplerender.Texture
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.examples.kotlin.helloar.GameConstants.INITIAL_ARROWS_PER_LEVEL
import com.google.ar.core.examples.kotlin.helloar.GameConstants.INITIAL_PLANET_COUNT
import com.google.ar.core.examples.kotlin.helloar.GameConstants.LEVELS_PER_NEW_PLANET
import com.google.ar.core.examples.kotlin.helloar.GameConstants.MAX_PLANETS_CAP
import java.io.IOException
import java.nio.ByteBuffer

class HelloArRenderer(val activity: HelloArActivity) :
    SampleRender.Renderer, DefaultLifecycleObserver {
    companion object {
        val TAG: String = HelloArRenderer::class.java.simpleName

        private const val Z_NEAR = 0.1f
        private const val Z_FAR = 100f
        const val CUBEMAP_RESOLUTION = 16
        const val CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32
    }

    // ----------------- UI
    private val levelTextView: TextView by lazy {
        activity.findViewById<TextView>(activity.resources.getIdentifier("level_text", "id", activity.packageName))
    }
    private val arrowsLeftTextView: TextView by lazy {
        activity.findViewById<TextView>(activity.resources.getIdentifier("arrows_left_text", "id", activity.packageName))
    }

    // ----------------- Core AR and Rendering Components
    private lateinit var render: SampleRender
    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var virtualSceneFramebuffer: Framebuffer
    private var hasSetTextureNames = false

    // Point Cloud
    // private lateinit var pointCloudVertexBuffer: VertexBuffer
    // private lateinit var pointCloudMesh: Mesh
    // private lateinit var pointCloudShader: Shader
    // private var lastPointCloudTimestamp: Long = 0

    // ----------------- Shaders and Textures and Maths
    private lateinit var virtualObjectShader: Shader
    private lateinit var dfgTexture: Texture
    private lateinit var cubemapFilter: SpecularCubemapFilter

    // Matrices
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewInverseMatrix = FloatArray(16)

    // Light Estimation
    private val sphericalHarmonicsCoefficients = FloatArray(9 * 3)
    private val viewLightDirection = FloatArray(4)

    // Helpers
    private val displayRotationHelper = DisplayRotationHelper(activity)
    private val trackingStateHelper = TrackingStateHelper(activity)
    private val session get() = activity.arCoreSessionHelper.session

    // ----------------- Game Logic and State Managers
    private var gameState = GameState()
    private val assetLoader = AssetLoader()
    private val anchorManager = AnchorManager()
    private lateinit var levelGenerator: LevelGenerator
    private val physicsSimulator = PhysicsSimulator()
    private lateinit var gameObjectRenderer: GameObjectRenderer
    private val lightManager = LightManager()

    // Initialization
    override fun onSurfaceCreated(render: SampleRender) {
        this.render = render
        try {
            // load game assets
            assetLoader.loadAssets(render)
            Log.i(TAG, "Assets loaded via AssetLoader.")

            // Initialize managers
            levelGenerator = LevelGenerator(assetLoader)
            gameObjectRenderer = GameObjectRenderer(assetLoader)
            Log.i(TAG, "Dependent managers initialized.")

            // ARCore rendering setup
            backgroundRenderer = BackgroundRenderer(render)
            // planeRenderer = PlaneRenderer(render) // to render planes debug
            virtualSceneFramebuffer = Framebuffer(render, /*width=*/1, /*height=*/1) // Will be resized

            // Cubemap and DFG texture for PBR lighting
            cubemapFilter = SpecularCubemapFilter(render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES)
            dfgTexture = Texture(
                render,
                Texture.Target.TEXTURE_2D,
                Texture.WrapMode.CLAMP_TO_EDGE,
                /*useMipmaps=*/false
            )
            val dfgResolution = 64
            val dfgBuffer = ByteBuffer.allocateDirect(dfgResolution * dfgResolution * 2 * 2) // RG16F is 2 components, 2 bytes each
            activity.assets.open("models/dfg.raw").use { it.read(dfgBuffer.array()) } // read into buffer's backing array
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.textureId)
            GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture")
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RG16F,
                dfgResolution,
                dfgResolution,
                0,
                GLES30.GL_RG,
                GLES30.GL_HALF_FLOAT,
                dfgBuffer
            )
            GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D")

            // Point cloud renderer (DEBUG)
            // pointCloudShader = Shader.createFromAssets(render, "shaders/point_cloud.vert", "shaders/point_cloud.frag",  null)
            //    .setVec4("u_Color", floatArrayOf(0.12f, 0.74f, 0.82f, 1.0f)) // Light blue
            //    .setFloat("u_PointSize", 5.0f)
            // pointCloudVertexBuffer = VertexBuffer(render, 4, null) // X,Y,Z,Confidence
            // pointCloudMesh = Mesh(render, Mesh.PrimitiveMode.POINTS, null, arrayOf(pointCloudVertexBuffer))

            // Virtual object shader (PBR)
            virtualObjectShader = Shader.createFromAssets(
                render,
                "shaders/environmental_hdr.vert",
                "shaders/environmental_hdr.frag",
                mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubemapFilter.numberOfMipmapLevels.toString())
            )
                .setTexture("u_Cubemap", cubemapFilter.filteredCubemapTexture)
                .setTexture("u_DfgTexture", dfgTexture)

            // Initialize game state
            gameState = GameState()
            Log.i(TAG, "onSurfaceCreated completed. Initial game state: $gameState")

        } catch (e: Exception) { // Broad catch for IOException, GLException, etc.
            Log.e(TAG, "Failed to create AR renderer components", e)
            showError("Error initializing renderer: ${e.message}")
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
        hasSetTextureNames = false
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        virtualSceneFramebuffer.resize(width, height)
    }


    // ----------------- UPDATE
    override fun onDrawFrame(render: SampleRender) {
        val localSession = session ?: return // Exit if session is null

        // Texture names should only be set once on a GL thread unless they change.
        // This is true during onDrawFrame rather than onSurfaceCreated.
        if (!hasSetTextureNames) {
            localSession.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
            hasSetTextureNames = true
        }

        displayRotationHelper.updateSessionIfNeeded(localSession)

        val frame: Frame = try {
            localSession.update()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available, exiting onDrawFrame.", e)
            showError("Camera not available. Please restart the app.")
            return
        }
        val camera = frame.camera

        // Anchor status
        anchorManager.updateTrackingStability(camera.trackingState, gameState.state)
        val previousStateBeforeAnchorUpdate = gameState.state
        gameState.state = anchorManager.updateAnchorLostStatus(gameState.state)
        if (previousStateBeforeAnchorUpdate == PuzzleState.PLAYING && gameState.state == PuzzleState.WAITING_FOR_ANCHOR) {
            physicsSimulator.clearTrajectory()
        }


        // Handle game state transitions related to anchor
        if (gameState.state == PuzzleState.WAITING_FOR_ANCHOR) {
            if (anchorManager.attemptPlaceOrRestoreAnchor(localSession, camera)) {
                Log.i(TAG, "Anchor successfully placed/restored. Resetting level.")
                resetLevel(localSession, camera)
            }
        }
        val anchorIsTracking = anchorManager.isAnchorTracking()
        val anchorPose = anchorManager.getAnchorPose()

        // Get current game elements based on anchor pose
        val currentPlanets = levelGenerator.getCurrentPlanetsWorld(anchorPose)
        val currentApple = levelGenerator.getCurrentAppleWorld(anchorPose)

        // Simulate trajectory only when playing
        if (gameState.state == PuzzleState.PLAYING && anchorIsTracking) {
            physicsSimulator.simulateArrowTrajectory(camera, currentPlanets, currentApple)
        } else {
            physicsSimulator.clearTrajectory()
        }

        // Background rendering
        backgroundRenderer.updateDisplayGeometry(frame)
        val depthEnabled = activity.depthSettings.useDepthForOcclusion()
        try {
            backgroundRenderer.setUseDepthVisualization(render, activity.depthSettings.depthColorVisualizationEnabled())
            backgroundRenderer.setUseOcclusion(render, depthEnabled)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read shader assets for depth settings.", e)
            showError("Error with depth shader: ${e.message}")
            return // Avoid further rendering if shaders are broken
        }

        // Depth image debug
        if (camera.trackingState == TrackingState.TRACKING && depthEnabled) {
            try {
                frame.acquireDepthImage16Bits().use { depthImage ->
                    backgroundRenderer.updateCameraDepthTexture(depthImage)
                }
            } catch (e: NotYetAvailableException) {
                // Depth image may not be available yet. This is normal.
            }
        }
        if (frame.timestamp != 0L) { //avoid renderer at first frame
            backgroundRenderer.drawBackground(render)
        }


        // ----------------- Game logic
        var appleHit = false
        if (gameState.state == PuzzleState.PLAYING && anchorIsTracking) {
            appleHit = physicsSimulator.updateGamePhysics(1f / 60f, currentPlanets, currentApple, anchorPose, gameState)
            if (appleHit) {
                gameState.state = PuzzleState.VICTORY
            }
        }

        // Check Defeat condition (after physics)
        if (gameState.state == PuzzleState.PLAYING && gameState.arrowsLeft == 0 && !physicsSimulator.hasActiveArrows()) {
            Log.i(TAG, "No arrows left and all shot arrows inactive. Defeat.")
            gameState.state = PuzzleState.DEFEAT
        }

        // Handle user taps
        handleTap(frame, camera, localSession)

        // Update screen on/off state based on tracking
        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        // messages
        val snackbarMessage = when {
            gameState.state == PuzzleState.WAITING_FOR_ANCHOR -> activity.getString(R.string.waiting_for_anchor)
            camera.trackingState == TrackingState.PAUSED && camera.trackingFailureReason == TrackingFailureReason.NONE -> activity.getString(R.string.searching_planes)
            camera.trackingState == TrackingState.PAUSED -> TrackingStateHelper.getTrackingFailureReasonString(camera)
            gameState.state == PuzzleState.VICTORY -> "Level Cleared! Tap to continue..." // (auto-advance kind of uselss)
            gameState.state == PuzzleState.DEFEAT -> "Game Over. Tap to retry."
            else -> null
        }
        if (snackbarMessage != null) {
            activity.view.snackbarHelper.showMessage(activity, snackbarMessage)
        } else {
            activity.view.snackbarHelper.hide(activity) // Hide if no message
        }

        // ARCore is not tracking, don't draw objects.
        if (camera.trackingState == TrackingState.PAUSED && gameState.state != PuzzleState.WAITING_FOR_ANCHOR) {
             Log.d(TAG, "Camera tracking paused, skipping virtual object rendering.")
             return
        }

        // Get camera matrices
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)
        camera.getViewMatrix(viewMatrix, 0)
        Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0) // Needed for lighting

        // Update light
        val lightEstimate = frame.lightEstimate
        lightManager.updateLightEstimation(lightEstimate, viewMatrix, virtualObjectShader, viewInverseMatrix, sphericalHarmonicsCoefficients, viewLightDirection)

        // ----------------- RENDERING

        // --- Render virtual objects to the virtual scene framebuffer ---
        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f) // Clear with transparent
        // Disable GL state checks like depth test and blend for PBR pass, as it writes to an offscreen framebuffer.
        // These will be re-enabled for the final composition.
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false) // Don't write to depth buffer for this pass
        GLES30.glDisable(GLES30.GL_BLEND)


        if (anchorIsTracking && assetLoader.assetsLoaded) { 
            // Draw Trajectory
            if (gameState.state == PuzzleState.PLAYING) {
                gameObjectRenderer.drawTrajectory(render, physicsSimulator.trajectoryPoints, virtualObjectShader, viewMatrix, projectionMatrix, virtualSceneFramebuffer)
            }
            // Draw Apple
            gameObjectRenderer.drawApple(render, currentApple, virtualObjectShader, viewMatrix, projectionMatrix, virtualSceneFramebuffer)
            // Draw Arrows
            gameObjectRenderer.drawArrows(render, physicsSimulator.arrows, virtualObjectShader, viewMatrix, projectionMatrix, virtualSceneFramebuffer)
            // Draw Planets
            gameObjectRenderer.drawPlanets(render, currentPlanets, virtualObjectShader, viewMatrix, projectionMatrix, virtualSceneFramebuffer)
        }

        // --- Composite virtual scene with camera feed ---
        // Enable depth test and writing for compositing
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_BLEND) // Blend virtual objects correctly over camera feed
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA) // Standard alpha blending

        // Occlusion handling: Render virtual scene without occlusion first, then enable for final composition.
        var occlusionWasForcedOff = false
        if (depthEnabled) {
            backgroundRenderer.setUseOcclusion(render, false) // Turn off for drawing virtual scene itself if it was on
            occlusionWasForcedOff = true
        }
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
        if (occlusionWasForcedOff) { // If we turned it off, turn it back on for subsequent frames/uses.
            backgroundRenderer.setUseOcclusion(render, true)
        }

        // ----------------- Post-render & game state updates ---
        if (gameState.state == PuzzleState.VICTORY) {
            Log.i(TAG, "Victory on Level ${gameState.level}!")
            gameState.level++ 
            resetLevel(localSession, camera)
        } else if (gameState.state == PuzzleState.DEFEAT) {
            Log.i(TAG, "Defeat on Level ${gameState.level}.")
            //TODO: HANDLE END GAME
        }

        // -- UI
        updateUIText()
    }


    private fun resetLevel(session: Session, camera: Camera?) {
        Log.i(TAG, "Resetting for level ${gameState.level}")
        physicsSimulator.clearArrows()
        physicsSimulator.clearTrajectory()
        levelGenerator.clearLevelLayout()

        if (camera != null && anchorManager.isAnchorTracking()) {
            anchorManager.getAnchor()?.let { anchor ->
                levelGenerator.generateLevelLayout(anchor, gameState)

                // Calculate arrows for this level (difficulty scaling)
                val arrowsThisLevel = (INITIAL_ARROWS_PER_LEVEL + (gameState.level / 2) - (gameState.level / 6)).coerceAtLeast(2)
                gameState.arrowsLeft = arrowsThisLevel
                gameState.state = PuzzleState.PLAYING 
                Log.i(TAG, "Level ${gameState.level} started with $arrowsThisLevel arrows.")
            }
        } else {
            Log.w(TAG, "Cannot reset level: Anchor not tracking or camera is null. Setting state to WAITING_FOR_ANCHOR.")
            gameState.state = PuzzleState.WAITING_FOR_ANCHOR
        }
        updateUIText()
    }

    private fun handleTap(frame: Frame, camera: Camera, session: Session) {
        if (camera.trackingState != TrackingState.TRACKING) return

        activity.view.tapHelper.poll()?.let { tap ->
            when (gameState.state) {
                PuzzleState.PLAYING -> {
                    if (anchorManager.isAnchorTracking() && gameState.arrowsLeft > 0) {
                        physicsSimulator.launchArrow(camera, gameState)
                        updateUIText()
                    } else if (!anchorManager.isAnchorTracking()){
                         activity.view.snackbarHelper.showMessage(activity, "Wait for stable tracking to shoot.")
                    } else
                    {

                    }
                }
                PuzzleState.DEFEAT -> {
                    Log.i(TAG, "Tap in DEFEAT state. Resetting game.")
                    //TODO HANDLE END GAME
                }
                PuzzleState.WAITING_FOR_ANCHOR -> {
                    Log.d(TAG, "Tap while WAITING_FOR_ANCHOR.")
                    if (anchorManager.attemptPlaceOrRestoreAnchor(session, camera)) {
                        resetLevel(session, camera)
                    } else {
                        activity.view.snackbarHelper.showMessage(activity, "Still trying to find a stable surface...")
                    }
                }
                 PuzzleState.VICTORY -> {
                    Log.d(TAG, "Tap during VICTORY (auto-advancing).")
                }
            }
        }
    }

    private fun updateUIText() {
        levelTextView.post { levelTextView.text = "Level ${gameState.level}" }
        arrowsLeftTextView.post { arrowsLeftTextView.text = gameState.arrowsLeft.toString() }
    }

    private fun showError(errorMessage: String) {
        Log.e(TAG, "Error shown to user: $errorMessage")
        activity.view.snackbarHelper.showError(activity, errorMessage)
    }
}