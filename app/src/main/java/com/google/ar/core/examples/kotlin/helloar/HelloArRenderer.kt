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

import android.graphics.ImageFormat
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import android.media.Image
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.LightEstimate
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.Trackable
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
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// --- CONSTANTS ---
private const val INITIAL_ARROWS_PER_LEVEL = 10
private const val MAX_PLANETS_CAP = 5
private const val MIN_PLACEMENT_DIST_FROM_CAMERA = 0.5f // Adjusted
private const val MAX_PLACEMENT_DIST_FROM_CAMERA = 3.5f // Adjusted
private const val APPLE_MAX_PLACEMENT_DIST_FACTOR = 1.0f // Apple placed within MAX_PLACEMENT_DIST

private const val MIN_DIST_BETWEEN_OBJECTS_FACTOR = 0.5f // Objects should be (radius1+radius2)*this_factor apart

// Visual and Collision Radii (assuming models are 1 unit in radius by default)
private const val ARROW_COLLISION_RADIUS = 0.05f
private const val APPLE_VISUAL_RADIUS = 0.5f // This is its world radius in meters
private const val PLANET_VISUAL_RADIUS_MIN = 1f
private const val PLANET_VISUAL_RADIUS_MAX = 3f

private const val VERTICAL_PLACEMENT_SPREAD = 0.5f
private const val OBJECT_SPAWN_ATTEMPTS = 400 // Increased attempts
private const val DEPTH_CHECK_OFFSET = 0.05f // Object center should be this much closer than depth surface

private const val MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT = 120

// --- GAME STATE DATA CLASSES ---
// Planet.scale now stores its visual (and collision) radius directly in meters
data class Planet(val position: FloatArray, val mass: Float, val textureIdx: Int, val visualRadius: Float)
data class Arrow(var position: FloatArray, var velocity: FloatArray, val mass: Float, var active: Boolean = true)
data class Apple(var position: FloatArray, val visualRadius: Float) // Added visualRadius for consistency
enum class PuzzleState { PLAYING, VICTORY, DEFEAT }
data class GameState(
    var level: Int = 1,
    var arrowsLeft: Int = INITIAL_ARROWS_PER_LEVEL,
    var score: Int = 0,
    var state: PuzzleState = PuzzleState.PLAYING
)

// --- MODEL AND TEXTURE FIELDS ---
private lateinit var planetMesh: Mesh
private lateinit var appleMesh: Mesh
private lateinit var arrowMesh: Mesh
private val planetTextures: MutableList<Texture> = mutableListOf()
private lateinit var appleTexture: Texture
private lateinit var arrowTexture: Texture
private val planetTextureFiles = listOf(
    "models/textures/planet_texture_1.jpg",
    "models/textures/planet_texture_2.jpg"
)
private const val appleTextureFile = "models/textures/apple_texture.jpg"
private const val arrowTextureFile = "models/textures/arrow_texture.jpg"
private const val planetObjFile = "models/planet.obj"
private const val appleObjFile = "models/apple.obj"
private const val arrowObjFile = "models/arrow.obj"

// --- GAME STATE FIELDS ---
private var gameState = GameState()
private var framesSinceTrackingStarted = 0
private var planets: MutableList<Planet> = mutableListOf()
private var arrows: MutableList<Arrow> = mutableListOf()
private var apple: Apple? = null
private val gravityConstant = 0.3f // Tuned gravity


/** Renders the HelloAR application using our example Renderer. */
class HelloArRenderer(val activity: HelloArActivity) :
  SampleRender.Renderer, DefaultLifecycleObserver {
  companion object {
    val TAG = "HelloArRenderer"
    private val sphericalHarmonicFactors =
      floatArrayOf(
        0.282095f, -0.325735f, 0.325735f, -0.325735f, 0.273137f,
        -0.273137f, 0.078848f, -0.273137f, 0.136569f
      )
    private const val Z_NEAR = 0.1f
    private const val Z_FAR = 100f
    const val APPROXIMATE_DISTANCE_METERS = 2.0f // For instant placement, not directly used in game logic
    const val CUBEMAP_RESOLUTION = 16
    const val CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32
  }

  lateinit var render: SampleRender
  lateinit var planeRenderer: PlaneRenderer
  lateinit var backgroundRenderer: BackgroundRenderer
  lateinit var virtualSceneFramebuffer: Framebuffer
  var hasSetTextureNames = false

  lateinit var pointCloudVertexBuffer: VertexBuffer
  lateinit var pointCloudMesh: Mesh
  lateinit var pointCloudShader: Shader
  var lastPointCloudTimestamp: Long = 0

  lateinit var virtualObjectShader: Shader

  private val wrappedAnchors = mutableListOf<WrappedAnchor>() // For ARCore example features, not game items

  lateinit var dfgTexture: Texture
  lateinit var cubemapFilter: SpecularCubemapFilter

  val modelMatrix = FloatArray(16)
  val viewMatrix = FloatArray(16)
  val projectionMatrix = FloatArray(16)
  val modelViewMatrix = FloatArray(16)
  val modelViewProjectionMatrix = FloatArray(16)
  val sphericalHarmonicsCoefficients = FloatArray(9 * 3)
  val viewInverseMatrix = FloatArray(16)
  val worldLightDirection = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
  val viewLightDirection = FloatArray(4)

  val session
    get() = activity.arCoreSessionHelper.session

  val displayRotationHelper = DisplayRotationHelper(activity)
  val trackingStateHelper = TrackingStateHelper(activity)

    // --- MATH AND GEOMETRY HELPERS ---
    private fun calculateDistanceSquared(pos1: FloatArray, pos2: FloatArray): Float {
        val dx = pos1[0] - pos2[0]; val dy = pos1[1] - pos2[1]; val dz = pos1[2] - pos2[2]
        return dx * dx + dy * dy + dz * dz
    }

    private fun isPoseInFov(objectWorldPos: FloatArray, camera: Camera, minDepth: Float, maxDepth: Float): Boolean {
        val objectPositionView = FloatArray(4)
        Matrix.multiplyMV(objectPositionView, 0, viewMatrix, 0, floatArrayOf(objectWorldPos[0], objectWorldPos[1], objectWorldPos[2], 1f), 0)

        if (objectPositionView[2] > -minDepth || objectPositionView[2] < -maxDepth) return false // Z is negative in view space

        val objectPositionClip = FloatArray(4)
        Matrix.multiplyMV(objectPositionClip, 0, projectionMatrix, 0, objectPositionView, 0)

        if (objectPositionClip[3] <= 0) return false // Behind camera near plane

        val ndcX = objectPositionClip[0] / objectPositionClip[3]
        val ndcY = objectPositionClip[1] / objectPositionClip[3]
        // val ndcZ = objectPositionClip[2] / objectPositionClip[3] // For standard -1 to 1 Z NDC check

        return ndcX >= -1.0f && ndcX <= 1.0f && ndcY >= -1.0f && ndcY <= 1.0f //&& ndcZ >= -1.0f && ndcZ <= 1.0f
    }

    /**
     * Checks if the space for an object is occupied by a real-world surface using the depth map.
     * @param worldPoint The center of the object in world coordinates.
     * @param objectVisualRadius The radius of the object.
     * @param frame The current ARCore Frame.
     * @param camera The current ARCore Camera.
     * @return True if the space is considered occupied, false otherwise.
     */
    private fun isSpaceOccupiedByDepth(
        worldPoint: FloatArray,
        objectVisualRadius: Float,
        frame: Frame,
        camera: Camera
    ): Boolean {
        // Acquire depth image. If not available, assume space is free (conservative for placement)
        // This needs to be efficient.
        val depthImage: Image = try {
            frame.acquireDepthImage16Bits()
        } catch (e: NotYetAvailableException) {
            // Log.w(TAG, "Depth image not available for collision check.")
            return false // Or true, depending on desired behavior when depth is missing
        }

        // Project world point to screen coordinates
        val viewPos = FloatArray(4)
        val screenPos = FloatArray(2)
        Matrix.multiplyMV(viewPos, 0, viewMatrix, 0, floatArrayOf(worldPoint[0], worldPoint[1], worldPoint[2], 1f), 0)

        // If behind camera or too close to near plane in view space
        if (viewPos[2] > -Z_NEAR) {
            depthImage.close()
            return true // Considered occupied if behind camera
        }

        val clipPos = FloatArray(4)
        Matrix.multiplyMV(clipPos, 0, projectionMatrix, 0, viewPos, 0)

        // NDC coordinates
        val ndcX = clipPos[0] / clipPos[3]
        val ndcY = clipPos[1] / clipPos[3]

        // Convert NDC to screen image coordinates (0 to width, 0 to height)
        // Depth image might have different dimensions than screen or camera texture
        val imageX = ((ndcX + 1.0f) / 2.0f * depthImage.width).toInt()
        val imageY = ((1.0f - ndcY) / 2.0f * depthImage.height).toInt() // Y is often flipped

        var occupied = false
        if (imageX >= 0 && imageX < depthImage.width && imageY >= 0 && imageY < depthImage.height) {
            // Get depth value from depth image (in millimeters)
            val plane = depthImage.planes[0]
            val byteIndex = imageX * plane.pixelStride + imageY * plane.rowStride
            val buffer = plane.buffer.order(ByteOrder.nativeOrder())
            val depthSampleMillis = buffer.getShort(byteIndex) // Depth in millimeters
            val depthSampleMeters = depthSampleMillis / 1000.0f

            // Depth of the virtual object in camera view space (is negative)
            val objectDepthInView = abs(viewPos[2])

            // If depth sample is valid (not 0, which can mean no data)
            // And if the real surface is closer than or very near the object's center minus its radius
            if (depthSampleMeters > 0 && depthSampleMeters < objectDepthInView + objectVisualRadius - DEPTH_CHECK_OFFSET) {
                occupied = true
                 Log.d(TAG, "Depth occupied: ObjDepthView=${String.format("%.2f",objectDepthInView)}m, SurfaceDepth=${String.format("%.2f",depthSampleMeters)}m, Radius=${String.format("%.2f",objectVisualRadius)}m at $imageX, $imageY")
            }
        } else {
             // Object projects outside depth image bounds, consider it free or handle as error
             // Log.d(TAG, "Object projects outside depth image bounds.")
        }

        depthImage.close()
        return occupied
    }


    // --- LEVEL GENERATION ---
    private fun placeObjectsRelatively(numPlanetsToSpawn: Int, cameraForOrientation: Camera?) {
        Log.d(TAG, "Placing objects relatively (fallback)")
        val cameraPose = cameraForOrientation?.pose ?: Pose.IDENTITY // Default if no camera
        val basePosition = cameraPose.translation
        val forwardDir = FloatArray(3).apply { cameraPose.getTransformedAxis(2, -1f, this, 0) } // -Z
        val upDir = FloatArray(3).apply { cameraPose.getTransformedAxis(1, 1f, this, 0) }    // +Y
        val rightDir = FloatArray(3).apply { cameraPose.getTransformedAxis(0, 1f, this, 0) } // +X
        val cameraY = cameraPose.ty()

        val placedObjectPositions = mutableListOf<Pair<FloatArray, Float>>() // Pair<Position, CollisionRadius>

        // Place apple
        val appleDist = MAX_PLACEMENT_DIST_FROM_CAMERA * 0.8f
        val appleVerticalOffset = (Random.nextFloat() * 2f - 1f) * VERTICAL_PLACEMENT_SPREAD * 0.5f
        val applePos = floatArrayOf(
            basePosition[0] + forwardDir[0] * appleDist,
            cameraY + appleVerticalOffset + forwardDir[1] * appleDist,
            basePosition[2] + forwardDir[2] * appleDist
        )
        apple = Apple(applePos.copyOf(), APPLE_VISUAL_RADIUS)
        placedObjectPositions.add(Pair(apple!!.position, apple!!.visualRadius))
        Log.d(TAG, "Relative apple at: ${applePos.contentToString()}, radius ${apple!!.visualRadius}")

        // Place planets
        for (i in 0 until numPlanetsToSpawn) {
            val planetRadius = Random.nextFloat() * (PLANET_VISUAL_RADIUS_MAX - PLANET_VISUAL_RADIUS_MIN) + PLANET_VISUAL_RADIUS_MIN
            val angle = Random.nextFloat() * PI.toFloat() * 2f // Full 360 for fallback
            val horizontalDist = MIN_PLACEMENT_DIST_FROM_CAMERA + Random.nextFloat() * (MAX_PLACEMENT_DIST_FROM_CAMERA - MIN_PLACEMENT_DIST_FROM_CAMERA - planetRadius)
            val verticalOffset = (Random.nextFloat() * 2f - 1f) * VERTICAL_PLACEMENT_SPREAD

            val xCamOffset = cos(angle) * horizontalDist
            val zCamOffset = sin(angle) * horizontalDist

            val worldOffsetX = rightDir[0] * xCamOffset + forwardDir[0] * zCamOffset
            val worldOffsetYForPlanar = rightDir[1] * xCamOffset + forwardDir[1] * zCamOffset
            val worldOffsetZ = rightDir[2] * xCamOffset + forwardDir[2] * zCamOffset

            val planetPos = floatArrayOf(
                basePosition[0] + worldOffsetX,
                cameraY + verticalOffset + worldOffsetYForPlanar,
                basePosition[2] + worldOffsetZ
            )
            planets.add(Planet(planetPos.copyOf(), 2.0f * planetRadius, i % kotlin.math.max(1, planetTextures.size), planetRadius))
            placedObjectPositions.add(Pair(planetPos.copyOf(), planetRadius))
            Log.d(TAG, "Relative planet $i at: ${planetPos.contentToString()}, radius $planetRadius")
        }
    }

    private fun tryPlaceObjectsDynamically(session: Session, frame: Frame, camera: Camera, numPlanetsToSpawn: Int): Boolean {
        Log.d(TAG, "Attempting dynamic object placement (depth-based) for level ${gameState.level}")
        val cameraPose = camera.pose
        val placedObjectPositions = mutableListOf<Pair<FloatArray, Float>>() // Pair<Position, CollisionRadius>

        // --- APPLE PLACEMENT ---
        var applePlaced = false
        for (attempt in 0 until OBJECT_SPAWN_ATTEMPTS) {
            val horizontalAngle = Random.nextFloat() * PI.toFloat() - PI.toFloat() / 2f // -90 to +90 deg
            val verticalAngle = Random.nextFloat() * (PI.toFloat() / 3f) - (PI.toFloat() / 6f) // Wider vertical search
            val dist = (MIN_PLACEMENT_DIST_FROM_CAMERA + APPLE_VISUAL_RADIUS) +
                       Random.nextFloat() * (MAX_PLACEMENT_DIST_FROM_CAMERA * APPLE_MAX_PLACEMENT_DIST_FACTOR - (MIN_PLACEMENT_DIST_FROM_CAMERA + APPLE_VISUAL_RADIUS))

            val localPos = floatArrayOf(dist * sin(horizontalAngle) * cos(verticalAngle),
                                        dist * sin(verticalAngle),
                                       -dist * cos(horizontalAngle) * cos(verticalAngle)) // Z is negative
            val worldPos = cameraPose.transformPoint(localPos)

            if (!isPoseInFov(worldPos, camera, MIN_PLACEMENT_DIST_FROM_CAMERA, MAX_PLACEMENT_DIST_FROM_CAMERA * APPLE_MAX_PLACEMENT_DIST_FACTOR)) continue
            if (isSpaceOccupiedByDepth(worldPos, APPLE_VISUAL_RADIUS, frame, camera)) continue
            if (calculateDistanceSquared(worldPos, cameraPose.translation) < (MIN_PLACEMENT_DIST_FROM_CAMERA + APPLE_VISUAL_RADIUS).pow(2)) continue

            apple = Apple(worldPos.copyOf(), APPLE_VISUAL_RADIUS)
            placedObjectPositions.add(Pair(apple!!.position, apple!!.visualRadius))
            applePlaced = true
            Log.i(TAG, "Dynamically placed apple at ${apple!!.position.contentToString()}, radius ${apple!!.visualRadius}")
            break
        }
        if (!applePlaced) {
            Log.w(TAG, "Failed to dynamically place apple after $OBJECT_SPAWN_ATTEMPTS attempts.")
            return false
        }

        // --- PLANET PLACEMENT ---
        var planetsSuccessfullyPlaced = 0
        for (i in 0 until numPlanetsToSpawn) {
            var planetPlacedThisIteration = false
            val planetRadius = Random.nextFloat() * (PLANET_VISUAL_RADIUS_MAX - PLANET_VISUAL_RADIUS_MIN) + PLANET_VISUAL_RADIUS_MIN

            for (attempt in 0 until OBJECT_SPAWN_ATTEMPTS) {
                val horizontalAngle = Random.nextFloat() * PI.toFloat() * 1.8f - (PI.toFloat() * 0.9f) // Wider spread
                val verticalOffset = (Random.nextFloat() * 2f - 1f) * VERTICAL_PLACEMENT_SPREAD
                val dist = (MIN_PLACEMENT_DIST_FROM_CAMERA + planetRadius) +
                           Random.nextFloat() * (MAX_PLACEMENT_DIST_FROM_CAMERA - (MIN_PLACEMENT_DIST_FROM_CAMERA + planetRadius))

                val localPosPlanar = floatArrayOf(dist * sin(horizontalAngle), 0f, -dist * cos(horizontalAngle))
                val worldPosPlanar = cameraPose.transformPoint(localPosPlanar)
                val planetPos = floatArrayOf(worldPosPlanar[0], cameraPose.ty() + verticalOffset, worldPosPlanar[2])

                if (!isPoseInFov(planetPos, camera, MIN_PLACEMENT_DIST_FROM_CAMERA, MAX_PLACEMENT_DIST_FROM_CAMERA)) continue
                if (isSpaceOccupiedByDepth(planetPos, planetRadius, frame, camera)) continue
                if (calculateDistanceSquared(planetPos, cameraPose.translation) < (MIN_PLACEMENT_DIST_FROM_CAMERA + planetRadius).pow(2)) continue

                var tooCloseToOtherObject = false
                for (obj in placedObjectPositions) {
                    val minDistBetween = (planetRadius + obj.second) * MIN_DIST_BETWEEN_OBJECTS_FACTOR
                    if (calculateDistanceSquared(planetPos, obj.first) < minDistBetween.pow(2)) {
                        tooCloseToOtherObject = true
                        break
                    }
                }
                if (tooCloseToOtherObject) continue

                planets.add(Planet(planetPos.copyOf(), 2.0f * planetRadius, planetsSuccessfullyPlaced % kotlin.math.max(1, planetTextures.size), planetRadius))
                placedObjectPositions.add(Pair(planetPos.copyOf(), planetRadius))
                planetsSuccessfullyPlaced++
                planetPlacedThisIteration = true
                Log.i(TAG, "Dynamically placed planet $planetsSuccessfullyPlaced at ${planetPos.contentToString()}, radius $planetRadius")
                break
            }
            if (!planetPlacedThisIteration) Log.w(TAG, "Failed to place planet ${i + 1} after $OBJECT_SPAWN_ATTEMPTS attempts.")
        }
        Log.i(TAG, "Dynamic placement: Apple ${if(applePlaced)"placed" else "NOT placed"}. $planetsSuccessfullyPlaced / $numPlanetsToSpawn planets placed.")
        return applePlaced
    }

    private fun resetLevel(session: Session?, frame: Frame?, camera: Camera?) {
        Log.i(TAG, "Resetting level. Current level: ${gameState.level}")
        planets.clear(); arrows.clear(); apple = null
        gameState.arrowsLeft = INITIAL_ARROWS_PER_LEVEL; gameState.state = PuzzleState.PLAYING
        val numPlanetsToSpawn = kotlin.math.min(gameState.level, MAX_PLANETS_CAP)

        var placementSuccessful = false
        if (session != null && frame != null && camera != null &&
            camera.trackingState == TrackingState.TRACKING &&
            framesSinceTrackingStarted >= MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT &&
            activity.depthSettings.useDepthForOcclusion() // Ensure depth is enabled for depth-based placement
            ) {
            Log.i(TAG, "Attempting dynamic placement, framesSinceTracking: $framesSinceTrackingStarted")
            placementSuccessful = tryPlaceObjectsDynamically(session, frame, camera, numPlanetsToSpawn)
        } else {
            val reason = when {
                camera?.trackingState != TrackingState.TRACKING -> "Camera not tracking."
                framesSinceTrackingStarted < MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT -> "Not enough tracking frames ($framesSinceTrackingStarted/$MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT)."
                !activity.depthSettings.useDepthForOcclusion() -> "Depth mode not enabled in settings."
                else -> "Unknown reason."
            }
            Log.w(TAG, "Dynamic placement skipped: $reason")
        }

        if (!placementSuccessful) {
            Log.w(TAG, "Dynamic placement failed or skipped. Falling back to relative placement.")
            placeObjectsRelatively(numPlanetsToSpawn, camera)
        }
        Log.i(TAG, "Level reset complete. Apple: ${apple!=null}, Planets: ${planets.size}")
    }

    // --- GAME LOGIC AND RENDERING ---
    private fun launchArrow(camera: Camera) {
        if (gameState.state != PuzzleState.PLAYING || gameState.arrowsLeft <= 0) return
        val cameraPose = camera.pose
        val startOffset = 0.1f
        val camForward = FloatArray(3).apply { cameraPose.getTransformedAxis(2, -1f, this, 0) } // -Z

        val pos = floatArrayOf(cameraPose.tx() + camForward[0] * startOffset,
                               cameraPose.ty() + camForward[1] * startOffset,
                               cameraPose.tz() + camForward[2] * startOffset)
        val initialSpeed = 5.0f // Increased speed
        val velocity = floatArrayOf(camForward[0] * initialSpeed, camForward[1] * initialSpeed, camForward[2] * initialSpeed)
        arrows.add(Arrow(pos.copyOf(), velocity, 1.0f))
        gameState.arrowsLeft--
    }

    private fun updateGameLogic(dt: Float) {
        val currentApple = apple ?: return
        arrows.filter { it.active }.forEach { arrow ->
            planets.forEach { planet ->
                val dx = planet.position[0] - arrow.position[0]
                val dy = planet.position[1] - arrow.position[1]
                val dz = planet.position[2] - arrow.position[2]
                val distSqr = dx * dx + dy * dy + dz * dz + 0.001f
                val dist = sqrt(distSqr)
                val forceMagnitude = gravityConstant * planet.mass * arrow.mass / distSqr
                arrow.velocity[0] += forceMagnitude * dx / dist * dt
                arrow.velocity[1] += forceMagnitude * dy / dist * dt
                arrow.velocity[2] += forceMagnitude * dz / dist * dt
            }
            arrow.position[0] += arrow.velocity[0] * dt
            arrow.position[1] += arrow.velocity[1] * dt
            arrow.position[2] += arrow.velocity[2] * dt

            // Apple collision (using visualRadius as collision radius)
            if (calculateDistanceSquared(arrow.position, currentApple.position) < (ARROW_COLLISION_RADIUS + currentApple.visualRadius).pow(2)) {
                gameState.score += 100 * gameState.level
                gameState.state = PuzzleState.VICTORY
                arrow.active = false
                Log.i(TAG, "Arrow hit apple! Radius: ${currentApple.visualRadius}")
                return // Exit early from updateGameLogic
            }

        }
        if (gameState.arrowsLeft == 0 && arrows.none { it.active }) {
            gameState.state = PuzzleState.DEFEAT
        }
    }

    override fun onSurfaceCreated(render: SampleRender) {
        this.render = render
        try {
            planetMesh = Mesh.createFromAsset(render, planetObjFile)
            appleMesh = Mesh.createFromAsset(render, appleObjFile)
            arrowMesh = Mesh.createFromAsset(render, arrowObjFile)

            planetTextures.clear()
            planetTextureFiles.forEach { file ->
                planetTextures.add(Texture.createFromAsset(render, file, Texture.WrapMode.REPEAT, Texture.ColorFormat.SRGB))
            }
            if (planetTextures.isEmpty()) Log.e(TAG, "No planet textures loaded!")

            appleTexture = Texture.createFromAsset(render, appleTextureFile, Texture.WrapMode.REPEAT, Texture.ColorFormat.SRGB)
            arrowTexture = Texture.createFromAsset(render, arrowTextureFile, Texture.WrapMode.REPEAT, Texture.ColorFormat.SRGB)

            planeRenderer = PlaneRenderer(render)
            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFramebuffer = Framebuffer(render, 1, 1)
            cubemapFilter = SpecularCubemapFilter(render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES)
            dfgTexture = Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE, false)
            val dfgResolution = 64
            val buffer = ByteBuffer.allocateDirect(dfgResolution * dfgResolution * 2 * 2) // RG16F
            activity.assets.open("models/dfg.raw").use { it.read(buffer.array()) }
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.textureId)
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RG16F, dfgResolution, dfgResolution, 0, GLES30.GL_RG, GLES30.GL_HALF_FLOAT, buffer)

            pointCloudShader = Shader.createFromAssets(render, "shaders/point_cloud.vert", "shaders/point_cloud.frag", null)
                .setVec4("u_Color", floatArrayOf(31.0f/255f, 188.0f/255f, 210.0f/255f, 1.0f))
                .setFloat("u_PointSize", 5.0f)
            pointCloudVertexBuffer = VertexBuffer(render, 4, null)
            pointCloudMesh = Mesh(render, Mesh.PrimitiveMode.POINTS, null, arrayOf(pointCloudVertexBuffer))

            virtualObjectShader = Shader.createFromAssets(render, "shaders/environmental_hdr.vert", "shaders/environmental_hdr.frag",
                mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubemapFilter.numberOfMipmapLevels.toString()))
                .setTexture("u_Cubemap", cubemapFilter.filteredCubemapTexture)
                .setTexture("u_DfgTexture", dfgTexture)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read asset file", e); showError("Failed to read asset file: $e")
        }
        resetLevel(null, null, null) // Initial level setup
    }

    override fun onResume(owner: LifecycleOwner) { displayRotationHelper.onResume(); hasSetTextureNames = false }
    override fun onPause(owner: LifecycleOwner) { displayRotationHelper.onPause() }
    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        virtualSceneFramebuffer.resize(width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        val localSession = session ?: return
        if (!hasSetTextureNames) {
            localSession.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
            hasSetTextureNames = true
        }
        displayRotationHelper.updateSessionIfNeeded(localSession)
        val frame = try { localSession.update() } catch (e: CameraNotAvailableException) { Log.e(TAG, "Camera not available", e); showError("Camera not available."); return }
        val camera = frame.camera

        if (camera.trackingState == TrackingState.TRACKING) {
            if (framesSinceTrackingStarted < MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT + 100) framesSinceTrackingStarted++ // Cap to avoid overflow but keep it high
        } else {
            framesSinceTrackingStarted = 0
        }

        try {
            backgroundRenderer.setUseDepthVisualization(render, activity.depthSettings.depthColorVisualizationEnabled())
            backgroundRenderer.setUseOcclusion(render, activity.depthSettings.useDepthForOcclusion())
        } catch (e: IOException) { Log.e(TAG, "Failed to read asset for depth settings", e); return }
        backgroundRenderer.updateDisplayGeometry(frame)
        if (camera.trackingState == TrackingState.TRACKING && activity.depthSettings.useDepthForOcclusion()) {
            try { frame.acquireDepthImage16Bits().use { backgroundRenderer.updateCameraDepthTexture(it) } }
            catch (e: NotYetAvailableException) { /* Normal, depth not always available */ }
        }

        if (gameState.state == PuzzleState.PLAYING) updateGameLogic(1f / 60f) // Approx 60fps
        handleTap(frame, camera)
        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        val message: String? = when {
            camera.trackingState == TrackingState.PAUSED && camera.trackingFailureReason == TrackingFailureReason.NONE -> activity.getString(R.string.searching_planes)
            camera.trackingState == TrackingState.PAUSED -> TrackingStateHelper.getTrackingFailureReasonString(camera)
            (localSession.hasTrackingPlane() || activity.instantPlacementSettings.isInstantPlacementEnabled || framesSinceTrackingStarted > 10) -> null
            else -> activity.getString(R.string.searching_planes)
        }
        if (message == null) activity.view.snackbarHelper.hide(activity) else activity.view.snackbarHelper.showMessage(activity, message)

        if (frame.timestamp != 0L) backgroundRenderer.drawBackground(render)
        if (camera.trackingState == TrackingState.PAUSED) return

        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)
        camera.getViewMatrix(viewMatrix, 0)

        frame.acquirePointCloud().use { pointCloud ->
            if (pointCloud.timestamp > lastPointCloudTimestamp) {
                pointCloudVertexBuffer.set(pointCloud.points); lastPointCloudTimestamp = pointCloud.timestamp
            }
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            render.draw(pointCloudMesh, pointCloudShader)
        }
        planeRenderer.drawPlanes(render, localSession.getAllTrackables(Plane::class.java), camera.displayOrientedPose, projectionMatrix)

        updateLightEstimation(frame.lightEstimate, viewMatrix)
        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)

        // Draw Apple (using its visualRadius for scaling)
        apple?.let { currentApple ->
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, currentApple.position[0], currentApple.position[1], currentApple.position[2])
            Matrix.scaleM(modelMatrix, 0, currentApple.visualRadius, currentApple.visualRadius, currentApple.visualRadius) // Apply scale
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix)
            virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            virtualObjectShader.setTexture("u_AlbedoTexture", appleTexture)
            render.draw(appleMesh, virtualObjectShader, virtualSceneFramebuffer)
        }

        // Draw Arrows (no scaling for arrow currently, could add ARROW_VISUAL_RADIUS if needed)
        arrows.filter { it.active }.forEach { currentArrow ->
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, currentArrow.position[0], currentArrow.position[1], currentArrow.position[2])
            // TODO: Arrow rotation based on velocity
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix)
            virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            virtualObjectShader.setTexture("u_AlbedoTexture", arrowTexture)
            render.draw(arrowMesh, virtualObjectShader, virtualSceneFramebuffer)
        }

        // Draw Planets (using planet.visualRadius for scaling)
        planets.forEach { currentPlanet ->
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, currentPlanet.position[0], currentPlanet.position[1], currentPlanet.position[2])
            Matrix.scaleM(modelMatrix, 0, currentPlanet.visualRadius, currentPlanet.visualRadius, currentPlanet.visualRadius) // Apply scale
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix)
            virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            if (planetTextures.isNotEmpty()) {
                virtualObjectShader.setTexture("u_AlbedoTexture", planetTextures[currentPlanet.textureIdx % planetTextures.size])
            }
            render.draw(planetMesh, virtualObjectShader, virtualSceneFramebuffer)
        }

        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)

        if (gameState.state == PuzzleState.VICTORY) {
            Log.i(TAG, "Victory! Level ${gameState.level} cleared. Score: ${gameState.score}")
            gameState.level++; resetLevel(localSession, frame, camera)
        } else if (gameState.state == PuzzleState.DEFEAT) {
            Log.i(TAG, "Defeat! Score: ${gameState.score} at Level ${gameState.level}.")
            // Consider adding a UI tap to restart, or auto-restart for testing:
            // gameState.level = 1; gameState.score = 0; resetLevel(localSession, frame, camera)
        }
    }

    private fun Session.hasTrackingPlane() = getAllTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING }
    private fun updateLightEstimation(lightEstimate: LightEstimate, viewMatrixParam: FloatArray) {
        if (lightEstimate.state != LightEstimate.State.VALID) {
            virtualObjectShader.setBool("u_LightEstimateIsValid", false); return
        }
        virtualObjectShader.setBool("u_LightEstimateIsValid", true)
        Matrix.invertM(viewInverseMatrix, 0, viewMatrixParam, 0)
        virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix)
        worldLightDirection[0] = lightEstimate.environmentalHdrMainLightDirection[0]
        worldLightDirection[1] = lightEstimate.environmentalHdrMainLightDirection[1]
        worldLightDirection[2] = lightEstimate.environmentalHdrMainLightDirection[2]
        Matrix.multiplyMV(viewLightDirection, 0, viewMatrixParam, 0, worldLightDirection, 0)
        virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection)
        virtualObjectShader.setVec3("u_LightIntensity", lightEstimate.environmentalHdrMainLightIntensity)
        val harmonics = lightEstimate.environmentalHdrAmbientSphericalHarmonics
        for (i in sphericalHarmonicsCoefficients.indices) {
            sphericalHarmonicsCoefficients[i] = harmonics[i] * sphericalHarmonicFactors[i / 3]
        }
        virtualObjectShader.setVec3Array("u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients)
        cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap())
    }

    private fun handleTap(frame: Frame, camera: Camera) {
        if (camera.trackingState != TrackingState.TRACKING || gameState.state != PuzzleState.PLAYING) return
        activity.view.tapHelper.poll()?.let { launchArrow(camera) }
    }
    private fun showError(errorMessage: String) = activity.view.snackbarHelper.showError(activity, errorMessage)
}

/** Associates an Anchor with the trackable it was attached to. Not used by game logic directly. */
private data class WrappedAnchor(val anchor: Anchor, val trackable: Trackable)