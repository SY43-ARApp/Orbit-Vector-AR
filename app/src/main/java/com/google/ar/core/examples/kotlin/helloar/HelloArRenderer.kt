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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random


// --- CONSTANTS (moved here for clarity, can be in companion object too) ---
private const val INITIAL_ARROWS_PER_LEVEL = 10
private const val MAX_PLANETS_CAP = 5
private const val MIN_PLACEMENT_DIST_FROM_CAMERA = 0.75f
private const val MAX_PLACEMENT_DIST_FROM_CAMERA = 2.0f
private const val APPLE_MAX_PLACEMENT_DIST_FACTOR = 1.25f
private const val MIN_DIST_BETWEEN_OBJECTS = 0.4f
private const val PLANET_MIN_SCALE = 5f
private const val PLANET_MAX_SCALE = 10f
private const val PLANET_COLLISION_RADIUS_FACTOR = 0.6f 
private const val VERTICAL_PLACEMENT_SPREAD = 1.5f // Max meters above/below camera eye level for planets
private const val PLANE_COLLISION_OFFSET = 0.15f // Min distance from a plane's "inside"
private const val OBJECT_SPAWN_ATTEMPTS = 30 // How many times to try placing an object before giving up
private const val MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT = 90 // Approx 1.5 seconds at 60fps


// --- GAME STATE DATA CLASSES ---
data class Planet(val position: FloatArray, val mass: Float, val textureIdx: Int, val scale: Float)
data class Arrow(var position: FloatArray, var velocity: FloatArray, val mass: Float, var active: Boolean = true)
data class Apple(var position: FloatArray)
enum class PuzzleState { PLAYING, VICTORY, DEFEAT }
data class GameState(
    var level: Int = 1,
    var arrowsLeft: Int = INITIAL_ARROWS_PER_LEVEL, // MODIFIED
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
    // Add more as needed
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
private val gravityConstant = 0.5f
private val arrowRadius = 0.05f
private val appleRadius = 1f
private val planetBaseRadius = 5f // Base radius, will be multiplied by planet.scale

private fun isPointOccludedByDetectedPlanes(
    pointToCheck: FloatArray,
    allPlanes: Collection<Plane>,
    cameraPose: Pose // Used to resolve ambiguity for vertical planes
): Boolean {
    if (allPlanes.isEmpty()) return false

    for (plane in allPlanes) {
        if (plane.trackingState != TrackingState.TRACKING) continue

        val planeCenter = plane.centerPose.translation
        // Compute normal based on plane type
        val planeNormal = when (plane.type) {
            Plane.Type.HORIZONTAL_UPWARD_FACING -> floatArrayOf(0f, 1f, 0f)
            Plane.Type.HORIZONTAL_DOWNWARD_FACING -> floatArrayOf(0f, -1f, 0f)
            Plane.Type.VERTICAL -> plane.centerPose.zAxis // ARCore: zAxis is normal for vertical
            else -> continue // Ignore other types
        }

        // Vector from a point on the plane (center) to the pointToCheck
        val vecToPoint = floatArrayOf(
            pointToCheck[0] - planeCenter[0],
            pointToCheck[1] - planeCenter[1],
            pointToCheck[2] - planeCenter[2]
        )

        // Signed distance to plane: dot(vecToPoint, planeNormal)
        val distanceToPlane = vecToPoint[0] * planeNormal[0] +
                              vecToPoint[1] * planeNormal[1] +
                              vecToPoint[2] * planeNormal[2]

        when (plane.type) {
            Plane.Type.HORIZONTAL_UPWARD_FACING, Plane.Type.HORIZONTAL_DOWNWARD_FACING -> {
                if (distanceToPlane < -PLANE_COLLISION_OFFSET) {
                    // Check if the point is roughly within the plane's horizontal extent
                    if (plane.isPoseInExtents(Pose(pointToCheck, FloatArray(4).apply { this[3] = 1f }))) {
                        Log.d(HelloArRenderer.TAG, "Point ${pointToCheck.contentToString()} occluded by horizontal plane ${plane.type}, dist: $distanceToPlane")
                        return true
                    }
                }
            }
            Plane.Type.VERTICAL -> {
                if (distanceToPlane < -PLANE_COLLISION_OFFSET) {
                    if (plane.isPoseInExtents(Pose(pointToCheck, FloatArray(4).apply { this[3] = 1f }))) {
                        Log.d(HelloArRenderer.TAG, "Point ${pointToCheck.contentToString()} occluded by vertical plane, dist: $distanceToPlane")
                        return true
                    }
                }
            }
            else -> {} // Ignore slanted or other types for now
        }
    }
    return false
}

/** Renders the HelloAR application using our example Renderer. */
class HelloArRenderer(val activity: HelloArActivity) :
  SampleRender.Renderer, DefaultLifecycleObserver {
  companion object {
    val TAG = "HelloArRenderer" // Standard TAG

    // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
    // constants.
    private val sphericalHarmonicFactors =
      floatArrayOf(
        0.282095f,
        -0.325735f,
        0.325735f,
        -0.325735f,
        0.273137f,
        -0.273137f,
        0.078848f,
        -0.273137f,
        0.136569f
      )

    private val Z_NEAR = 0.1f
    private val Z_FAR = 100f

    val APPROXIMATE_DISTANCE_METERS = 2.0f

    val CUBEMAP_RESOLUTION = 16
    val CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32
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

  lateinit var virtualObjectMesh: Mesh // Generic mesh, will be replaced by specific ones
  lateinit var virtualObjectShader: Shader // Generic shader for game objects
  // Specific textures are loaded globally for game objects

  private val wrappedAnchors = mutableListOf<WrappedAnchor>()

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


    // --- HELPER FUNCTIONS FOR LEVEL GENERATION ---
    private fun normalize(v: FloatArray) {
      val norm = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
      if (norm > 1e-5f) {
          v[0] /= norm
          v[1] /= norm
          v[2] /= norm
      }
    }

    private fun calculateDistanceSquared(pos1: FloatArray, pos2: FloatArray): Float {
        val dx = pos1[0] - pos2[0]
        val dy = pos1[1] - pos2[1]
        val dz = pos1[2] - pos2[2]
        return dx * dx + dy * dy + dz * dz
    }

    private fun calculateDistance(pos1: FloatArray, pos2: FloatArray): Float {
        return sqrt(calculateDistanceSquared(pos1, pos2))
    }

    private fun Pose.distanceTo(otherPose: Pose): Float {
        val dx = this.tx() - otherPose.tx()
        val dy = this.ty() - otherPose.ty()
        val dz = this.tz() - otherPose.tz()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun isPoseInFov(objectPose: Pose, camera: Camera, minDepth: Float, maxDepth: Float): Boolean {
        val objectPositionWorld = objectPose.translation
        // Use current member viewMatrix and projectionMatrix updated in onDrawFrame
        // val currentViewMatrix = FloatArray(16).also { camera.getViewMatrix(it, 0) } // Or use this.viewMatrix
        // val currentProjectionMatrix = FloatArray(16).also { camera.getProjectionMatrix(it, 0, Z_NEAR, Z_FAR) } // Or use this.projectionMatrix

        // Transform point to view space
        val objectPositionView = FloatArray(4)
        val objectPositionWorldHomogeneous = floatArrayOf(objectPositionWorld[0], objectPositionWorld[1], objectPositionWorld[2], 1f)
        Matrix.multiplyMV(objectPositionView, 0, this.viewMatrix, 0, objectPositionWorldHomogeneous, 0)

        // Check depth (Z in view space is negative for points in front)
        if (objectPositionView[2] > -minDepth || objectPositionView[2] < -maxDepth) {
            return false
        }

        // Transform to clip space
        val objectPositionClip = FloatArray(4)
        Matrix.multiplyMV(objectPositionClip, 0, this.projectionMatrix, 0, objectPositionView, 0)

        // Perform perspective divide to get NDC
        // Check for w <= 0 to avoid division by zero or negative (point behind camera plane)
        if (objectPositionClip[3] <= 0) return false

        val ndcX = objectPositionClip[0] / objectPositionClip[3]
        val ndcY = objectPositionClip[1] / objectPositionClip[3]
        val ndcZ = objectPositionClip[2] / objectPositionClip[3]


        // Check if within NDC bounds (typically -1 to 1 for X, Y and Z after perspective divide for visible region)
        return ndcX >= -1.0f && ndcX <= 1.0f &&
               ndcY >= -1.0f && ndcY <= 1.0f &&
               ndcZ >= -1.0f && ndcZ <= 1.0f // Check Z in NDC as well
    }


    private fun placeObjectsRelatively(planetCountToSpawn: Int, cameraForOrientation: Camera?) {
      Log.d(TAG, "Placing objects relatively (fallback)")
      val basePosition: FloatArray
      val forwardDir: FloatArray
      val upDir: FloatArray
      val rightDir: FloatArray
      val cameraY: Float // For vertical positioning
  
      if (cameraForOrientation != null && cameraForOrientation.trackingState == TrackingState.TRACKING) {
          val camPose = cameraForOrientation.pose
          basePosition = camPose.translation.copyOf() // Use camera translation as origin
          forwardDir = camPose.zAxis.let { floatArrayOf(-it[0], -it[1], -it[2]) } // Camera forward
          upDir = camPose.yAxis.copyOf()
          rightDir = camPose.xAxis.copyOf()
          cameraY = camPose.ty()
      } else {
          basePosition = floatArrayOf(0f, 0f, 0f)
          forwardDir = floatArrayOf(0f, 0f, -1f)
          upDir = floatArrayOf(0f, 1f, 0f)
          rightDir = floatArrayOf(1f, 0f, 0f)
          cameraY = 0.5f // Assume a default height if no camera
      }
  
      val placedPositions = mutableListOf<Pair<FloatArray, Float>>()
  
      // Place apple
      val appleDist = MAX_PLACEMENT_DIST_FROM_CAMERA * 0.9f // Fallback apple distance
      val appleVerticalOffset = (Random.nextFloat() * 2f - 1f) * VERTICAL_PLACEMENT_SPREAD * 0.3f // Less spread for apple
  
      val applePos = floatArrayOf(
          basePosition[0] + forwardDir[0] * appleDist,
          cameraY + appleVerticalOffset + forwardDir[1] * appleDist, // Apply vertical offset relative to cameraY
          basePosition[2] + forwardDir[2] * appleDist
      )
      apple = Apple(applePos)
      placedPositions.add(Pair(applePos, appleRadius))
      Log.d(TAG, "Relative apple at: ${applePos.contentToString()}")
  
      // Place planets
      for (i in 0 until planetCountToSpawn) {
          val angle = (PI.toFloat() * (i + 0.5f) / planetCountToSpawn) - (PI.toFloat() / 2f) // Spread in front hemisphere
          val horizontalDist = (MIN_PLACEMENT_DIST_FROM_CAMERA + MAX_PLACEMENT_DIST_FROM_CAMERA) / 2.5f + Random.nextFloat() * 0.5f
          // More varied vertical placement for fallback planets
          val verticalOffset = (Random.nextFloat() * 2f - 1f) * VERTICAL_PLACEMENT_SPREAD
          val depthDist = (MIN_PLACEMENT_DIST_FROM_CAMERA + MAX_PLACEMENT_DIST_FROM_CAMERA) / 2f + (Random.nextFloat() - 0.5f) * 1.0f
  
          // Calculate offsets based on camera orientation
          val xCamOffset = cos(angle) * horizontalDist
          val zCamOffset = sin(angle) * horizontalDist - depthDist // Ensure in front, adjust depth with depthDist
  
          // Transform these local offsets to world offsets using camera's axes
          val worldOffsetX = rightDir[0] * xCamOffset + forwardDir[0] * zCamOffset
          val worldOffsetY = rightDir[1] * xCamOffset + forwardDir[1] * zCamOffset // This is planar offset Y component
          val worldOffsetZ = rightDir[2] * xCamOffset + forwardDir[2] * zCamOffset
  
          val planetPos = floatArrayOf(
              basePosition[0] + worldOffsetX,
              cameraY + verticalOffset + worldOffsetY, // Add random vertical offset AND y component from planar offset
              basePosition[2] + worldOffsetZ
          )
  
          val scale = (PLANET_MIN_SCALE + Random.nextFloat() * (PLANET_MAX_SCALE - PLANET_MIN_SCALE))
          val mass = 2.0f * scale
          planets.add(Planet(planetPos, mass, i % kotlin.math.max(1, planetTextures.size), scale))
          placedPositions.add(Pair(planetPos, planetBaseRadius * scale))
          Log.d(TAG, "Relative planet $i at: ${planetPos.contentToString()}")
      }
  }

  private fun tryPlaceObjectsDynamically(session: Session, frame: Frame, camera: Camera, numPlanetsToSpawn: Int): Boolean {
    Log.d(TAG, "Attempting dynamic object placement for level ${gameState.level}")
    val cameraPose = camera.pose
    val allPlanes = session.getAllTrackables(Plane::class.java)
        .filter { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }

    val placedObjectPositions = mutableListOf<Pair<FloatArray, Float>>() // position, effective radius

    // --- APPLE PLACEMENT ---
    var applePlaced = false
    for (attempt in 0 until OBJECT_SPAWN_ATTEMPTS) {
        val horizontalAngle = Random.nextFloat() * PI.toFloat() - PI.toFloat() / 2f // -90 to +90 deg in front
        val verticalAngle = Random.nextFloat() * (PI.toFloat()/4f) - (PI.toFloat()/8f) // Slight vertical variation for apple search
        val dist = MAX_PLACEMENT_DIST_FROM_CAMERA * 0.8f + Random.nextFloat() * (MAX_PLACEMENT_DIST_FROM_CAMERA * APPLE_MAX_PLACEMENT_DIST_FACTOR * 0.4f)

        // Create point in camera local space
        val localPos = floatArrayOf(
            dist * sin(horizontalAngle) * cos(verticalAngle),
            dist * sin(verticalAngle),
            -dist * cos(horizontalAngle) * cos(verticalAngle) // Z is negative in front
        )
        // Convert to world space
        val worldPos = cameraPose.transformPoint(localPos)

        val appleCandidatePose = Pose(worldPos, cameraPose.rotationQuaternion) // Use camera rotation for FoV check consistency

        if (!isPoseInFov(appleCandidatePose, camera, MIN_PLACEMENT_DIST_FROM_CAMERA, MAX_PLACEMENT_DIST_FROM_CAMERA * APPLE_MAX_PLACEMENT_DIST_FACTOR)) continue
        if (isPointOccludedByDetectedPlanes(worldPos, allPlanes, cameraPose)) continue
        // Check distance to camera (redundant with FoV depth but good sanity check)
        if (calculateDistanceSquared(worldPos, cameraPose.translation) < MIN_PLACEMENT_DIST_FROM_CAMERA.pow(2)) continue


        apple = Apple(worldPos.copyOf())
        placedObjectPositions.add(Pair(apple!!.position, appleRadius))
        applePlaced = true
        Log.i(TAG, "Dynamically placed apple at ${apple!!.position.contentToString()}")
        break
    }

    if (!applePlaced) {
        Log.w(TAG, "Failed to dynamically place apple after $OBJECT_SPAWN_ATTEMPTS attempts.")
        return false // If apple can't be placed, fail dynamic placement for this level
    }

    // --- PLANET PLACEMENT ---
    var planetsSuccessfullyPlaced = 0
    for (i in 0 until numPlanetsToSpawn) {
        var planetPlacedThisIteration = false
        for (attempt in 0 until OBJECT_SPAWN_ATTEMPTS) {
            val horizontalAngle = Random.nextFloat() * PI.toFloat() * 1.6f - (PI.toFloat() * 0.8f) // Wider spread for planets
            val verticalOffset = (Random.nextFloat() * 2f - 1f) * VERTICAL_PLACEMENT_SPREAD
            val dist = MIN_PLACEMENT_DIST_FROM_CAMERA + Random.nextFloat() * (MAX_PLACEMENT_DIST_FROM_CAMERA - MIN_PLACEMENT_DIST_FROM_CAMERA) * 0.8f // Planets generally closer than apple

            // Create point in camera local space (relative to camera position and orientation)
            // X and Z form a point on a circle in front of camera, Y is an offset from camera's Y
            val localPosPlanar = floatArrayOf(
                dist * sin(horizontalAngle),
                0f, // Y will be handled in world space relative to camera's Y
                -dist * cos(horizontalAngle) // Z is negative in front
            )
            // Convert planar part to world space
            val worldPosPlanar = cameraPose.transformPoint(localPosPlanar)

            // Now apply vertical offset in world space, centered around camera's current height
            val planetPos = floatArrayOf(worldPosPlanar[0], cameraPose.ty() + verticalOffset, worldPosPlanar[2])

            val planetCandidatePose = Pose(planetPos, cameraPose.rotationQuaternion) // Use camera rotation for FoV consistency

            if (!isPoseInFov(planetCandidatePose, camera, MIN_PLACEMENT_DIST_FROM_CAMERA * 0.9f, MAX_PLACEMENT_DIST_FROM_CAMERA * 1.1f)) continue
            if (isPointOccludedByDetectedPlanes(planetPos, allPlanes, cameraPose)) continue
            
            val scale = (PLANET_MIN_SCALE + Random.nextFloat() * (PLANET_MAX_SCALE - PLANET_MIN_SCALE))
            val currentPlanetEffectiveRadius = scale * PLANET_COLLISION_RADIUS_FACTOR

            var tooCloseToOtherObject = false
            for (obj in placedObjectPositions) {
                if (calculateDistanceSquared(planetPos, obj.first) < (currentPlanetEffectiveRadius + obj.second + MIN_DIST_BETWEEN_OBJECTS).pow(2)) {
                    tooCloseToOtherObject = true
                    break
                }
            }
            if (tooCloseToOtherObject) continue
            
            // Check distance to camera
            if (calculateDistanceSquared(planetPos, cameraPose.translation) < (currentPlanetEffectiveRadius + MIN_PLACEMENT_DIST_FROM_CAMERA).pow(2)) continue


            val mass = 2.0f * scale
            planets.add(Planet(planetPos.copyOf(), mass, planetsSuccessfullyPlaced % kotlin.math.max(1, planetTextures.size), scale))
            placedObjectPositions.add(Pair(planetPos.copyOf(), currentPlanetEffectiveRadius))
            planetsSuccessfullyPlaced++
            planetPlacedThisIteration = true
            Log.i(TAG, "Dynamically placed planet $planetsSuccessfullyPlaced at ${planetPos.contentToString()}")
            break // Next planet
        }
        if (!planetPlacedThisIteration) {
            Log.w(TAG, "Failed to place planet ${i + 1} after $OBJECT_SPAWN_ATTEMPTS attempts.")
        }
    }
    Log.i(TAG, "Dynamic placement: Apple ${if(applePlaced)"placed" else "NOT placed"}. $planetsSuccessfullyPlaced / $numPlanetsToSpawn planets placed.")
    return applePlaced // Success if at least apple is placed. Could require planets too.
}


  private fun resetLevel(session: Session?, frame: Frame?, camera: Camera?) {
    Log.i(TAG, "Resetting level. Current level: ${gameState.level}")
    planets.clear()
    arrows.clear()
    apple = null

    gameState.arrowsLeft = INITIAL_ARROWS_PER_LEVEL
    gameState.state = PuzzleState.PLAYING

    val numPlanetsToSpawn = kotlin.math.min(gameState.level, MAX_PLANETS_CAP)
    Log.d(TAG, "Attempting to spawn $numPlanetsToSpawn planets for level ${gameState.level}")

    var placementSuccessful = false
    if (session != null && frame != null && camera != null &&
        camera.trackingState == TrackingState.TRACKING &&
        framesSinceTrackingStarted >= MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT) { // MODIFIED Check
        Log.i(TAG, "Attempting dynamic placement, framesSinceTracking: $framesSinceTrackingStarted")
        placementSuccessful = tryPlaceObjectsDynamically(session, frame, camera, numPlanetsToSpawn)
    } else {
        if (camera?.trackingState != TrackingState.TRACKING) {
            Log.w(TAG, "Dynamic placement skipped: Camera not tracking.")
        } else if (framesSinceTrackingStarted < MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT) {
            Log.w(TAG, "Dynamic placement skipped: Not enough tracking frames ($framesSinceTrackingStarted / $MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT).")
        }
    }

    if (!placementSuccessful) {
        Log.w(TAG, "Dynamic placement failed or skipped. Falling back to relative placement.")
        placeObjectsRelatively(numPlanetsToSpawn, camera)
    }
    Log.i(TAG, "Level reset complete. Apple: ${apple!=null}, Planets: ${planets.size}")
  }


    private fun launchArrow(camera: Camera) {
        if (gameState.state != PuzzleState.PLAYING || gameState.arrowsLeft <= 0) return

        val cameraPose = camera.pose // Use the actual camera pose from ARCore
        
        // Arrow starts slightly in front of the camera to avoid immediate collision with near plane or user
        val startOffset = 0.1f 
        val camForward = floatArrayOf(-cameraPose.zAxis[0], -cameraPose.zAxis[1], -cameraPose.zAxis[2])

        val pos = floatArrayOf(
            cameraPose.tx() + camForward[0] * startOffset,
            cameraPose.ty() + camForward[1] * startOffset,
            cameraPose.tz() + camForward[2] * startOffset
        )
        
        // Arrow velocity based on camera direction
        val initialSpeed = 4.5f // Tunable: speed of the arrow
        val velocity = floatArrayOf(
            camForward[0] * initialSpeed,
            camForward[1] * initialSpeed,
            camForward[2] * initialSpeed
        )
        
        arrows.add(Arrow(pos.copyOf(), velocity, 1.0f))
        gameState.arrowsLeft--
        Log.d(TAG, "Arrow launched. Arrows left: ${gameState.arrowsLeft}")
    }

    private fun updateGameLogic(dt: Float) {
        // This function is only called if gameState.state == PuzzleState.PLAYING (checked in onDrawFrame)
        val appleCurrent = apple ?: return // If apple is null, something is wrong with level setup

        for (arrow in arrows) {
            if (!arrow.active) continue

            // Apply gravity from planets
            for (planet in planets) {
                val dx = planet.position[0] - arrow.position[0]
                val dy = planet.position[1] - arrow.position[1]
                val dz = planet.position[2] - arrow.position[2]
                // Adding a small epsilon to distSqr to prevent division by zero if arrow is at planet center
                val distSqr = dx * dx + dy * dy + dz * dz + 0.001f 
                val dist = sqrt(distSqr)
                val forceMagnitude = gravityConstant * planet.mass * arrow.mass / distSqr
                
                arrow.velocity[0] += forceMagnitude * dx / dist * dt
                arrow.velocity[1] += forceMagnitude * dy / dist * dt
                arrow.velocity[2] += forceMagnitude * dz / dist * dt
            }

            // Update arrow position
            arrow.position[0] += arrow.velocity[0] * dt
            arrow.position[1] += arrow.velocity[1] * dt
            arrow.position[2] += arrow.velocity[2] * dt

            // Check collision with apple
            val effectiveAppleRadius = appleRadius // Assuming appleRadius is its actual collision radius
            if (calculateDistanceSquared(arrow.position, appleCurrent.position) < (arrowRadius + effectiveAppleRadius).pow(2)) {
                Log.i(TAG, "Arrow hit the apple!")
                gameState.score += 100 * gameState.level // Score for completing the current level
                gameState.state = PuzzleState.VICTORY
                arrow.active = false // Deactivate arrow
                // The rest (level progression, reset) is handled in onDrawFrame
                return // Exit early as level is won
            }

            // Optional: Check collision with planets (deactivate arrow)
            // for (planet in planets) {
            //     val effectivePlanetRadius = planetBaseRadius * planet.scale
            //      if (calculateDistanceSquared(arrow.position, planet.position) < (arrowRadius + effectivePlanetRadius).pow(2)) {
            //         Log.d(TAG, "Arrow hit a planet.")
            //         arrow.active = false // Deactivate arrow
            //         break // Arrow can only hit one planet
            //     }
            // }

        }

        // Check defeat condition: no arrows left AND all fired arrows are inactive
        if (gameState.arrowsLeft == 0 && arrows.none { it.active }) {
            Log.i(TAG, "No arrows left and all fired arrows are inactive. Defeat.")
            gameState.state = PuzzleState.DEFEAT
        }
    }

  override fun onResume(owner: LifecycleOwner) {
    displayRotationHelper.onResume()
    hasSetTextureNames = false
  }

  override fun onPause(owner: LifecycleOwner) {
    displayRotationHelper.onPause()
  }

  override fun onSurfaceCreated(render: SampleRender) {
    this.render = render
    try {
        // --- MESH LOADING ---
        planetMesh = Mesh.createFromAsset(render, planetObjFile)
        appleMesh = Mesh.createFromAsset(render, appleObjFile)
        arrowMesh = Mesh.createFromAsset(render, arrowObjFile)

        // --- TEXTURE LOADING ---
        planetTextures.clear()
        for (file in planetTextureFiles) {
            planetTextures.add(Texture.createFromAsset(render, file, Texture.WrapMode.REPEAT, Texture.ColorFormat.SRGB))
        }
        if (planetTextures.isEmpty()) { // Fallback if no textures found
            Log.e(TAG, "Planet textures list is empty. Attempting to load a default.")
            // Try to load at least one, or handle error (e.g. use a plain color shader)
            // For now, if planetTextureFiles is empty, it might crash later when accessing planetTextures[planet.textureIdx]
            // A robust solution would be to have a fallback texture or color.
            // For this prototype, we assume textures are present.
        }
        appleTexture = Texture.createFromAsset(render, appleTextureFile, Texture.WrapMode.REPEAT, Texture.ColorFormat.SRGB)
        arrowTexture = Texture.createFromAsset(render, arrowTextureFile, Texture.WrapMode.REPEAT, Texture.ColorFormat.SRGB)

        // --- RENDERER SETUP (Standard ARCore setup) ---
      planeRenderer = PlaneRenderer(render)
      backgroundRenderer = BackgroundRenderer(render)
      virtualSceneFramebuffer = Framebuffer(render, /*width=*/ 1, /*height=*/ 1)

      cubemapFilter =
        SpecularCubemapFilter(render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES)
      dfgTexture =
        Texture(
          render,
          Texture.Target.TEXTURE_2D,
          Texture.WrapMode.CLAMP_TO_EDGE,
          /*useMipmaps=*/ false
        )
      val dfgResolution = 64
      val dfgChannels = 2
      val halfFloatSize = 2
      val buffer: ByteBuffer =
        ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize)
      activity.assets.open("models/dfg.raw").use { it.read(buffer.array()) }
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.textureId)
      GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture")
      GLES30.glTexImage2D(
        GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RG16F, dfgResolution, dfgResolution, 0,
        GLES30.GL_RG, GLES30.GL_HALF_FLOAT, buffer
      )
      GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D")

      pointCloudShader =
        Shader.createFromAssets(
            render, "shaders/point_cloud.vert", "shaders/point_cloud.frag", null)
          .setVec4("u_Color", floatArrayOf(31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f))
          .setFloat("u_PointSize", 5.0f)
      pointCloudVertexBuffer = VertexBuffer(render, 4, null)
      pointCloudMesh = Mesh(render, Mesh.PrimitiveMode.POINTS, null, arrayOf(pointCloudVertexBuffer))
      
      // Shader for all game objects (planets, apple, arrow)
      // The original virtualObjectShader using pawn_albedo is not directly used for game items.
      // We'll use a similar environmental HDR shader for our game objects.
      virtualObjectShader =
        Shader.createFromAssets(
            render,
            "shaders/environmental_hdr.vert", // Standard PBR shader
            "shaders/environmental_hdr.frag",
            mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubemapFilter.numberOfMipmapLevels.toString())
          )
          // u_AlbedoTexture will be set per object type in onDrawFrame
          // .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", pbrTexture) // If you have PBR maps for your objects
          .setTexture("u_Cubemap", cubemapFilter.filteredCubemapTexture)
          .setTexture("u_DfgTexture", dfgTexture)
        // If game objects don't have specific PBR maps (RoughnessMetallicAmbientOcclusion),
        // you might need to set default values or use a simpler shader.
        // For this prototype, we assume the environmental_hdr shader can work with just albedo + environment.
        // Often, you'd have a default white texture for metallic/roughness/AO if not specified.
        // Or set uniform floats for default roughness/metallic values.
        // For simplicity, we'll rely on u_AlbedoTexture and environmental lighting.
        // You may need to create a placeholder 1x1 white texture for u_RoughnessMetallicAmbientOcclusionTexture
        // if the shader strictly requires it. Let's assume it can handle it being potentially unbound or
        // use a default if not set (though explicit is better).
        // We will set albedo texture per object.
        // A simple diffuse shader might be easier if PBR is not the focus for these items.
        // But since environmental_hdr.frag is provided, let's try to use it.


    } catch (e: IOException) {
      Log.e(TAG, "Failed to read a required asset file", e)
      showError("Failed to read a required asset file: $e")
    }
    // Initial level setup: Call with nulls as session/frame/camera might not be fully ready or tracking.
    // This will use the fallback relative placement.
    resetLevel(null, null, null)
  }

  override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
    displayRotationHelper.onSurfaceChanged(width, height)
    virtualSceneFramebuffer.resize(width, height)
  }

  override fun onDrawFrame(render: SampleRender) {
    val session = session ?: return // Should not happen if onResume was called.

    if (!hasSetTextureNames) {
      session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
      hasSetTextureNames = true
    }

    displayRotationHelper.updateSessionIfNeeded(session)
    
    val frame = try {
        session.update()
    } catch (e: CameraNotAvailableException) {
      Log.e(TAG, "Camera not available during onDrawFrame", e)
      showError("Camera not available. Try restarting the app.")
      return
    }
    val camera = frame.camera

     // Increment tracking frame counter
     if (camera.trackingState == TrackingState.TRACKING) {
          if (framesSinceTrackingStarted < MIN_TRACKING_FRAMES_FOR_DYNAMIC_PLACEMENT + 10) { // Prevent overflow, but allow it to stay high
              framesSinceTrackingStarted++
          }
      } else {
          framesSinceTrackingStarted = 0 // Reset if tracking is lost
      }

    // Update ARCore state for background and occlusion
    try {
      backgroundRenderer.setUseDepthVisualization(render, activity.depthSettings.depthColorVisualizationEnabled())
      backgroundRenderer.setUseOcclusion(render, activity.depthSettings.useDepthForOcclusion())
    } catch (e: IOException) {
      Log.e(TAG, "Failed to read a required asset file for depth", e); showError("Failed to read a required asset file: $e"); return
    }
    backgroundRenderer.updateDisplayGeometry(frame)
    if (camera.trackingState == TrackingState.TRACKING && (activity.depthSettings.useDepthForOcclusion() || activity.depthSettings.depthColorVisualizationEnabled())) {
      try { frame.acquireDepthImage16Bits().use { depthImage -> backgroundRenderer.updateCameraDepthTexture(depthImage) } }
      catch (e: NotYetAvailableException) { /* Depth data not yet available, common case */ }
    }

    // --- GAME LOGIC UPDATE ---
    if (gameState.state == PuzzleState.PLAYING) {
        updateGameLogic(1f / 60f) // Assuming 60 FPS for fixed time step
    }

    // --- HANDLE USER INPUT ---
    handleTap(frame, camera) // Arrow launch

    trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)
    //Snackbar messages for ARCore state
    val message: String? = when {
        camera.trackingState == TrackingState.PAUSED && camera.trackingFailureReason == TrackingFailureReason.NONE -> activity.getString(R.string.searching_planes)
        camera.trackingState == TrackingState.PAUSED -> TrackingStateHelper.getTrackingFailureReasonString(camera)
        session.hasTrackingPlane() || activity.instantPlacementSettings.isInstantPlacementEnabled -> null // Hide if planes or instant placement
        else -> activity.getString(R.string.searching_planes) // Still searching
    }
    if (message == null) activity.view.snackbarHelper.hide(activity) else activity.view.snackbarHelper.showMessage(activity, message)


    // --- DRAWING ---
    // Background
    if (frame.timestamp != 0L) backgroundRenderer.drawBackground(render)
    if (camera.trackingState == TrackingState.PAUSED) return


    // Matrices
    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)
    camera.getViewMatrix(viewMatrix, 0) // viewMatrix is now updated for this frame

    // Point cloud and planes (non-occluded)
    frame.acquirePointCloud().use { pointCloud ->
      if (pointCloud.timestamp > lastPointCloudTimestamp) {
        pointCloudVertexBuffer.set(pointCloud.points)
        lastPointCloudTimestamp = pointCloud.timestamp
      }
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
      pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
      render.draw(pointCloudMesh, pointCloudShader)
    }
    planeRenderer.drawPlanes(render, session.getAllTrackables(Plane::class.java), camera.displayOrientedPose, projectionMatrix)

    // Occluded objects: Setup lighting and framebuffer
    updateLightEstimation(frame.lightEstimate, viewMatrix)
    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)

    // Draw Apple
    apple?.let { currentApple ->
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, currentApple.position[0], currentApple.position[1], currentApple.position[2])
      // Add scaling for apple if necessary: Matrix.scaleM(modelMatrix, 0, appleScale, appleScale, appleScale)
      Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
      
      virtualObjectShader.setMat4("u_ModelView", modelViewMatrix)
      virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
      virtualObjectShader.setTexture("u_AlbedoTexture", appleTexture)
      // virtualObjectShader.setTexture("u_RoughnessMetallicAmbientOcclusionTexture", defaultPbrTexture) // If needed
      render.draw(appleMesh, virtualObjectShader, virtualSceneFramebuffer)
    }

    // Draw Arrows
    for (currentArrow in arrows) {
      if (!currentArrow.active) continue
      Matrix.setIdentityM(modelMatrix, 0)
      // Translation
      Matrix.translateM(modelMatrix, 0, currentArrow.position[0], currentArrow.position[1], currentArrow.position[2])
      // Rotation to align with velocity
      val v = currentArrow.velocity
      val normV = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2])
      if (normV > 1e-4f) {
          // Simple rotation: align Z axis with velocity. Arrow model should point along its local Z.
          // This requires calculating rotation axis and angle from (0,0,1) to normalized velocity.
          // A lookAt matrix functionality is easier:
          // Create a rotation matrix that makes the arrow point in direction of velocity
          // Default arrow model points along -Z or +Z? Assume +Z for now.
          // If arrow model points along +X, adjust accordingly.
          // For simplicity, this part is often tricky. A full quaternion/axis-angle solution is robust.
          // Let's use a simplified approach or skip complex rotation for prototype if it becomes an issue.
          // Matrix.rotateM for axis-angle. Angle is acos(dot(forward, norm_vel)), axis is cross(forward, norm_vel)
      }
      // Add scaling for arrow if necessary
      Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
      virtualObjectShader.setMat4("u_ModelView", modelViewMatrix)
      virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
      virtualObjectShader.setTexture("u_AlbedoTexture", arrowTexture)
      render.draw(arrowMesh, virtualObjectShader, virtualSceneFramebuffer)
    }

    // Draw Planets
    for (currentPlanet in planets) {
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, currentPlanet.position[0], currentPlanet.position[1], currentPlanet.position[2])
      Matrix.scaleM(modelMatrix, 0, currentPlanet.scale, currentPlanet.scale, currentPlanet.scale)
      // Add rotation for planets if desired (e.g., slow spin)
      Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
      virtualObjectShader.setMat4("u_ModelView", modelViewMatrix)
      virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
      if (planetTextures.isNotEmpty()) { // Check to prevent crash if textures failed to load
        virtualObjectShader.setTexture("u_AlbedoTexture", planetTextures[currentPlanet.textureIdx % planetTextures.size])
      }
      render.draw(planetMesh, virtualObjectShader, virtualSceneFramebuffer)
    }

    // Compose virtual scene with background
    backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)

    // --- HANDLE GAME STATE CHANGES (VICTORY/DEFEAT) ---
    if (gameState.state == PuzzleState.VICTORY) {
      Log.i(TAG, "Victory! Level ${gameState.level} cleared. Current Total Score: ${gameState.score}")
      
      gameState.level++ // Advance to next level
      // Regenerate level (this will set state to PLAYING, reset arrows)
      // Pass current session, frame, camera for dynamic placement
      resetLevel(session, frame, camera) 
      Log.i(TAG, "Starting Level ${gameState.level}. Arrows: ${gameState.arrowsLeft}")

    } else if (gameState.state == PuzzleState.DEFEAT) {
      Log.i(TAG, "Defeat! Final Score: ${gameState.score} at Level ${gameState.level}.")
      // Game remains in DEFEAT state. Player needs to manually restart app or UI interaction (not implemented here).
      // To auto-restart current level for testing:
      // Log.i(TAG, "Auto-restarting current level ${gameState.level} due to defeat.")
      // resetLevel(session, frame, camera)
    }
  }

  private fun Session.hasTrackingPlane() =
    getAllTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING }

  private fun updateLightEstimation(lightEstimate: LightEstimate, viewMatrixParam: FloatArray) {
    if (lightEstimate.state != LightEstimate.State.VALID) {
      virtualObjectShader.setBool("u_LightEstimateIsValid", false)
      return
    }
    virtualObjectShader.setBool("u_LightEstimateIsValid", true)
    Matrix.invertM(viewInverseMatrix, 0, viewMatrixParam, 0) // Use passed viewMatrix if it's specific to this call
    virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix)
    updateMainLight(
      lightEstimate.environmentalHdrMainLightDirection,
      lightEstimate.environmentalHdrMainLightIntensity,
      viewMatrixParam
    )
    updateSphericalHarmonicsCoefficients(lightEstimate.environmentalHdrAmbientSphericalHarmonics)
    cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap())
  }

  private fun updateMainLight(direction: FloatArray, intensity: FloatArray, viewMatrixParam: FloatArray) {
    worldLightDirection[0] = direction[0]; worldLightDirection[1] = direction[1]; worldLightDirection[2] = direction[2]
    Matrix.multiplyMV(viewLightDirection, 0, viewMatrixParam, 0, worldLightDirection, 0)
    virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection)
    virtualObjectShader.setVec3("u_LightIntensity", intensity)
  }

  private fun updateSphericalHarmonicsCoefficients(coefficients: FloatArray) {
    require(coefficients.size == 9 * 3) { "Spherical harmonics coefficients array must be of length 27." }
    for (i in 0 until 9 * 3) {
      sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3]
    }
    virtualObjectShader.setVec3Array("u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients)
  }

  private fun handleTap(frame: Frame, camera: Camera) {
    if (camera.trackingState != TrackingState.TRACKING) return
    if (gameState.state != PuzzleState.PLAYING) return // Only allow taps if playing

    val tap = activity.view.tapHelper.poll() ?: return

    // The original example uses tap for placing anchors. We use it for shooting.
    // The hitTest logic below is for placing anchors, not strictly needed for shooting
    // in the current camera direction, but can be kept if future features need it.
    // For now, launchArrow only depends on camera pose.
    
    // Example anchor placement logic (can be removed if not used for anything else)
    // val hitResultList = if (activity.instantPlacementSettings.isInstantPlacementEnabled) {
    //    frame.hitTestInstantPlacement(tap.x, tap.y, APPROXIMATE_DISTANCE_METERS)
    // } else {
    //    frame.hitTest(tap)
    // }
    // val firstHitResult = hitResultList.firstOrNull { ... } // (original hit filtering logic)
    // if (firstHitResult != null) {
    //    if (wrappedAnchors.size >= 20) { wrappedAnchors[0].anchor.detach(); wrappedAnchors.removeAt(0) }
    //    wrappedAnchors.add(WrappedAnchor(firstHitResult.createAnchor(), firstHitResult.trackable))
    //    activity.runOnUiThread { activity.view.showOcclusionDialogIfNeeded() }
    // }

    launchArrow(camera) // Launch arrow regardless of hit test result for now
  }

  private fun showError(errorMessage: String) =
    activity.view.snackbarHelper.showError(activity, errorMessage)
}

/**
 * Associates an Anchor with the trackable it was attached to.
 */
private data class WrappedAnchor(
  val anchor: Anchor,
  val trackable: Trackable,
)