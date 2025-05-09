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
import com.google.ar.core.Frame
import android.media.Image
import com.google.ar.core.LightEstimate
import com.google.ar.core.Plane
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import android.widget.TextView

// --- CONSTANTS ---
private const val INITIAL_ARROWS_PER_LEVEL = 10
private const val MAX_PLANETS_CAP = 50
private const val INITIAL_PLANET_COUNT = 10
private const val LEVELS_PER_NEW_PLANET = 2

private const val LEVEL_ANCHOR_DISTANCE_FORWARD = 5.5f
private const val LEVEL_ANCHOR_DISTANCE_UP = 0.7f

private const val CLUSTER_MAX_RADIUS_APPLE = 1.5f
private const val CLUSTER_MAX_RADIUS_PLANETS = 2.0f
private const val CLUSTER_MIN_DIST_PLANETS_FROM_ANCHOR = 0.5f
private const val CLUSTER_VERTICAL_SPREAD_FACTOR = 0.8f

private const val APPLE_MODEL_DEFAULT_RADIUS = 0.1f
private const val PLANET_MODEL_DEFAULT_RADIUS = 0.1f
private const val ARROW_MODEL_DEFAULT_RADIUS = 0.1f
private const val TRAJECTORY_DOT_MODEL_DEFAULT_RADIUS = 0.05f

private const val APPLE_TARGET_RADIUS = 0.2f
private const val PLANET_TARGET_RADIUS_MIN = 0.15f
private const val PLANET_TARGET_RADIUS_MAX = 0.55f
private const val ARROW_VISUAL_AND_COLLISION_RADIUS = 0.1f
private const val TRAJECTORY_DOT_TARGET_RADIUS = 0.01f

private const val PLANET_MASS_SCALE_FACTOR = 4500.0f
private const val ARROW_LAUNCH_SPEED = 7.0f
private const val ARROW_MASS = 0.5f

private const val MIN_TRACKING_FRAMES_FOR_ANCHOR_PLACEMENT = 60

private const val TRAJECTORY_SIMULATION_START_AT_STEP = 15
private const val TRAJECTORY_SIMULATION_STEPS = 70
private const val TRAJECTORY_SIMULATION_TIMESTEP = 0.015f


// --- GAME STATE DATA CLASSES ---
data class Planet(val worldPosition: FloatArray, val mass: Float, val textureIdx: Int, val targetRadius: Float) {
    override fun equals(other: Any?): Boolean { if (this === other) return true; if (javaClass != other?.javaClass) return false; other as Planet; if (!worldPosition.contentEquals(other.worldPosition)) return false; if (mass != other.mass) return false; if (textureIdx != other.textureIdx) return false; if (targetRadius != other.targetRadius) return false; return true }
    override fun hashCode(): Int { var result = worldPosition.contentHashCode(); result = 31 * result + mass.hashCode(); result = 31 * result + textureIdx; result = 31 * result + targetRadius.hashCode(); return result }
}
data class Arrow(var position: FloatArray, var velocity: FloatArray, val mass: Float, var active: Boolean = true) {
     override fun equals(other: Any?): Boolean { if (this === other) return true; if (javaClass != other?.javaClass) return false; other as Arrow; if (!position.contentEquals(other.position)) return false; return true } override fun hashCode(): Int { return position.contentHashCode() }
}
data class Apple(var worldPosition: FloatArray, val targetRadius: Float) {
     override fun equals(other: Any?): Boolean { if (this === other) return true; if (javaClass != other?.javaClass) return false; other as Apple; if (!worldPosition.contentEquals(other.worldPosition)) return false; return true } override fun hashCode(): Int { return worldPosition.contentHashCode() }
}
enum class PuzzleState { WAITING_FOR_ANCHOR, PLAYING, VICTORY, DEFEAT }
data class GameState(
    var level: Int = 1,
    var arrowsLeft: Int = INITIAL_ARROWS_PER_LEVEL,
    var score: Int = 0,
    var state: PuzzleState = PuzzleState.WAITING_FOR_ANCHOR
)

// --- MODEL AND TEXTURE FIELDS ---
private lateinit var planetMesh: Mesh
private lateinit var appleMesh: Mesh
private lateinit var arrowMesh: Mesh
private lateinit var trajectoryDotMesh: Mesh
private val planetTextures: MutableList<Texture> = mutableListOf()
private lateinit var appleTexture: Texture
private lateinit var arrowTexture: Texture
private lateinit var trajectoryDotTexture: Texture

private val planetTextureFiles = listOf("models/textures/planet_texture_1.jpg", "models/textures/planet_texture_2a.jpg", "models/textures/planet_texture_2b.jpg", "models/textures/planet_texture_3a.jpg", "models/textures/planet_texture_3b.jpg")

private const val appleTextureFile = "models/textures/Apple_BaseColor.jpg"
private const val arrowTextureFile = "models/textures/arrow_texture.png"
private const val trajectoryDotTextureFile = "models/textures/dot_texture.jpg"

private const val planetObjFile = "models/planet.obj"
private const val appleObjFile = "models/apple.obj"
private const val arrowObjFile = "models/arrow.obj"
private const val trajectoryDotObjFile = "models/trajectory_dot.obj"


// --- GAME STATE FIELDS ---
private var gameState = GameState()
private var framesSinceTrackingStable = 0
private var planets: MutableList<Planet> = mutableListOf()
private var arrows: MutableList<Arrow> = mutableListOf()
private var apple: Apple? = null
private var levelOriginAnchor: Anchor? = null
private val gravityConstant = 0.05f
private val trajectoryPoints: MutableList<FloatArray> = mutableListOf()

private val worldUpVector = floatArrayOf(0f, 1f, 0f)


/** Renders the HelloAR application using our example Renderer. */
class HelloArRenderer(val activity: HelloArActivity) :
  SampleRender.Renderer, DefaultLifecycleObserver {
  companion object {
    val TAG = "HelloArRenderer"
    private val sphericalHarmonicFactors = floatArrayOf(0.282095f, -0.325735f, 0.325735f, -0.325735f, 0.273137f, -0.273137f, 0.078848f, -0.273137f, 0.136569f)
    private const val Z_NEAR = 0.1f; private const val Z_FAR = 100f
    const val CUBEMAP_RESOLUTION = 16; const val CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32
  }

  private val levelTextView: TextView by lazy {
      activity.findViewById<TextView>(activity.resources.getIdentifier("level_text", "id", activity.packageName))
  }
  private val arrowsLeftTextView: TextView by lazy {
      activity.findViewById<TextView>(activity.resources.getIdentifier("arrows_left_text", "id", activity.packageName))
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
  lateinit var dfgTexture: Texture
  lateinit var cubemapFilter: SpecularCubemapFilter
  val modelMatrix = FloatArray(16); val viewMatrix = FloatArray(16); val projectionMatrix = FloatArray(16)
  val modelViewMatrix = FloatArray(16); val modelViewProjectionMatrix = FloatArray(16)
  val sphericalHarmonicsCoefficients = FloatArray(9 * 3); val viewInverseMatrix = FloatArray(16)
  val worldLightDirection = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f); val viewLightDirection = FloatArray(4)

  // arrow rotation matices
  private val rotationMatrix = FloatArray(16)
  private val tempMatrix = FloatArray(16)


  val session get() = activity.arCoreSessionHelper.session
  val displayRotationHelper = DisplayRotationHelper(activity)
  val trackingStateHelper = TrackingStateHelper(activity)

    private fun calculateDistanceSquared(pos1: FloatArray, pos2: FloatArray): Float {
        val dx = pos1[0] - pos2[0]; val dy = pos1[1] - pos2[1]; val dz = pos1[2] - pos2[2]
        return dx * dx + dy * dy + dz * dz
    }

    private fun rotationMatrixFromTo(from: FloatArray, to: FloatArray, outMatrix: FloatArray) {
        // axis and angle
        val cross = floatArrayOf(
            from[1]*to[2] - from[2]*to[1],
            from[2]*to[0] - from[0]*to[2],
            from[0]*to[1] - from[1]*to[0]
        )
        val dot = from[0]*to[0] + from[1]*to[1] + from[2]*to[2]
        val normCross = sqrt(cross[0]*cross[0] + cross[1]*cross[1] + cross[2]*cross[2])

        if (normCross < 1e-6 && dot > 0.9999f) {    //same vector
            Matrix.setIdentityM(outMatrix, 0)
            return
        }
        if (normCross < 1e-6 && dot < -0.9999f) {   //opposite vector
            val axis = if (abs(from[0]) < 0.1f) floatArrayOf(1f,0f,0f) else floatArrayOf(0f,1f,0f)
            val ortho = floatArrayOf(
                from[1]*axis[2] - from[2]*axis[1],
                from[2]*axis[0] - from[0]*axis[2],
                from[0]*axis[1] - from[1]*axis[0]
            )
            val n = sqrt(ortho[0]*ortho[0] + ortho[1]*ortho[1] + ortho[2]*ortho[2])
            if (n > 1e-6) {
                ortho[0] /= n; ortho[1] /= n; ortho[2] /= n
                Matrix.setRotateM(outMatrix, 0, 180f, ortho[0], ortho[1], ortho[2])
            } else {
                Matrix.setIdentityM(outMatrix, 0)
            }
            return
        }
        // axis angle to quaternion
        val axis = floatArrayOf(cross[0]/normCross, cross[1]/normCross, cross[2]/normCross)
        val angle = Math.acos(dot.coerceIn(-1f,1f).toDouble()).toFloat() * (180f/PI.toFloat())
        Matrix.setRotateM(outMatrix, 0, angle, axis[0], axis[1], axis[2])
    }

    private fun attemptCreateLevelAnchor(session: Session, camera: Camera): Boolean {
        if (levelOriginAnchor != null && levelOriginAnchor!!.trackingState == TrackingState.TRACKING) {
            return true
        }
        levelOriginAnchor?.detach()
        levelOriginAnchor = null

        if (camera.trackingState == TrackingState.TRACKING && framesSinceTrackingStable >= MIN_TRACKING_FRAMES_FOR_ANCHOR_PLACEMENT) {
            val cameraPose = camera.pose
            val translationInCameraFrame = floatArrayOf(0f, LEVEL_ANCHOR_DISTANCE_UP, -LEVEL_ANCHOR_DISTANCE_FORWARD)
            val anchorPoseInWorld = cameraPose.compose(Pose.makeTranslation(translationInCameraFrame))
            
            try {
                levelOriginAnchor = session.createAnchor(anchorPoseInWorld)
                Log.i(TAG, "Level anchor created at ${anchorPoseInWorld.translation.joinToString()}")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create level anchor", e)
                return false
            }
        }
        return false
    }

    private fun generateLevelLayout(anchor: Anchor, numPlanetsToSpawn: Int) {
        Log.i(TAG, "Generating level layout for $numPlanetsToSpawn planets around anchor for level ${gameState.level}")
        planets.clear(); arrows.clear(); apple = null

        val anchorPose = anchor.pose
        val placedObjectPositionsAndRadii = mutableListOf<Pair<FloatArray, Float>>()

        val appleRadius = APPLE_TARGET_RADIUS
        var applePlaced = false
        for (i in 0..100) {
            val r = Random.nextFloat() * CLUSTER_MAX_RADIUS_APPLE
            val theta = Random.nextFloat() * 2f * PI.toFloat() 
            val phi = Random.nextFloat() * PI.toFloat() * CLUSTER_VERTICAL_SPREAD_FACTOR - (PI.toFloat() * CLUSTER_VERTICAL_SPREAD_FACTOR / 2f)
            val localX = r * cos(theta) * cos(phi); val localY = r * sin(phi); val localZ = r * sin(theta) * cos(phi)
            val localPos = floatArrayOf(localX, localY, localZ)
            val worldPos = anchorPose.transformPoint(localPos)
            apple = Apple(worldPos.copyOf(), appleRadius)
            placedObjectPositionsAndRadii.add(Pair(worldPos.copyOf(), appleRadius))
            applePlaced = true; break
        }
        if (!applePlaced) {
            Log.w(TAG, "Failed to place apple, placing at anchor as fallback.")
            val worldPos = anchorPose.translation
            apple = Apple(worldPos.copyOf(), appleRadius)
            placedObjectPositionsAndRadii.add(Pair(worldPos.copyOf(), appleRadius))
        }

        var planetsSuccessfullyPlaced = 0
        for (pIdx in 0 until numPlanetsToSpawn) {
            val planetRad = Random.nextFloat() * (PLANET_TARGET_RADIUS_MAX - PLANET_TARGET_RADIUS_MIN) + PLANET_TARGET_RADIUS_MIN
            var planetPlacedThisIter = false
            for (attempt in 0..200) { 
                val r = CLUSTER_MIN_DIST_PLANETS_FROM_ANCHOR + Random.nextFloat() * (CLUSTER_MAX_RADIUS_PLANETS - CLUSTER_MIN_DIST_PLANETS_FROM_ANCHOR)
                val theta = Random.nextFloat() * 2f * PI.toFloat()
                val phi = Random.nextFloat() * PI.toFloat() * CLUSTER_VERTICAL_SPREAD_FACTOR - (PI.toFloat() * CLUSTER_VERTICAL_SPREAD_FACTOR / 2f)
                val localX = r * cos(theta) * cos(phi); val localY = r * sin(phi); val localZ = r * sin(theta) * cos(phi)
                val localPos = floatArrayOf(localX, localY, localZ)
                val worldPos = anchorPose.transformPoint(localPos)
                var tooCloseToOthers = false
                for ((otherPos, otherRadius) in placedObjectPositionsAndRadii) {
                    val minDistSq = (planetRad + otherRadius + 0.2f).pow(2) 
                    if (calculateDistanceSquared(worldPos, otherPos) < minDistSq) { tooCloseToOthers = true; break }
                }
                if (tooCloseToOthers) continue
                val mass = PLANET_MASS_SCALE_FACTOR * planetRad.pow(2.0f)
                val textureIdx = planetsSuccessfullyPlaced % kotlin.math.max(1, planetTextures.size)
                planets.add(Planet(worldPos.copyOf(), mass, textureIdx, planetRad))
                placedObjectPositionsAndRadii.add(Pair(worldPos.copyOf(), planetRad))
                planetsSuccessfullyPlaced++; planetPlacedThisIter = true; break
            }
            if (!planetPlacedThisIter) Log.w(TAG, "Failed to place planet ${pIdx + 1} after multiple attempts.")
        }
        Log.i(TAG, "Level generation complete. Apple ${if(applePlaced)"OK" else "FALLBACK"}. $planetsSuccessfullyPlaced/$numPlanetsToSpawn planets.")
    }

    private fun resetLevel(session: Session, camera: Camera?) {
        Log.i(TAG, "Resetting level ${gameState.level}")
        if (camera != null && attemptCreateLevelAnchor(session, camera)) {
            levelOriginAnchor?.let { anchor ->
                if (anchor.trackingState == TrackingState.TRACKING) {
                    val additionalPlanets = if (gameState.level <= 1) 0 else (gameState.level - 1) / LEVELS_PER_NEW_PLANET
                    val numPlanets = kotlin.math.min(INITIAL_PLANET_COUNT + additionalPlanets, MAX_PLANETS_CAP)
                    generateLevelLayout(anchor, numPlanets)
                    gameState.arrowsLeft = INITIAL_ARROWS_PER_LEVEL + (gameState.level / 2) 
                    gameState.state = PuzzleState.PLAYING
                } else {
                    Log.w(TAG, "Level anchor not tracking. Waiting."); gameState.state = PuzzleState.WAITING_FOR_ANCHOR
                }
            }
        } else {
            Log.w(TAG, "Failed to create or confirm level anchor. Waiting for stable tracking."); gameState.state = PuzzleState.WAITING_FOR_ANCHOR
            planets.clear(); arrows.clear(); apple = null; trajectoryPoints.clear()
        }
    }

    private fun launchArrow(camera: Camera) {
        if (gameState.state != PuzzleState.PLAYING || gameState.arrowsLeft <= 0) return
        val camPose = camera.pose
        val startOff = 0.2f
        val forward = FloatArray(3).apply{ camPose.getTransformedAxis(2, -1f, this, 0) } 
        val pos = floatArrayOf(camPose.tx() + forward[0] * startOff, camPose.ty() + forward[1] * startOff, camPose.tz() + forward[2] * startOff)
        val vel = floatArrayOf(forward[0] * ARROW_LAUNCH_SPEED, forward[1] * ARROW_LAUNCH_SPEED, forward[2] * ARROW_LAUNCH_SPEED)
        arrows.add(Arrow(pos.copyOf(), vel, ARROW_MASS))
        gameState.arrowsLeft--
    }

    private fun simulateArrowTrajectory(startCamera: Camera) {
        trajectoryPoints.clear()
        if (planets.isEmpty() && apple == null) return 

        val camPose = startCamera.pose
        val startOffset = 0.2f
        val forward = FloatArray(3).apply { camPose.getTransformedAxis(2, -1f, this, 0) }

        val currentPosition = floatArrayOf(
            camPose.tx() + forward[0] * startOffset,
            camPose.ty() + forward[1] * startOffset,
            camPose.tz() + forward[2] * startOffset
        )
        val currentVelocity = floatArrayOf(
            forward[0] * ARROW_LAUNCH_SPEED,
            forward[1] * ARROW_LAUNCH_SPEED,
            forward[2] * ARROW_LAUNCH_SPEED
        )
        val simulatedArrowMass = ARROW_MASS

        for (step in 0 until TRAJECTORY_SIMULATION_STEPS) {
            planets.forEach { planet ->
                val dx = planet.worldPosition[0] - currentPosition[0]; val dy = planet.worldPosition[1] - currentPosition[1]; val dz = planet.worldPosition[2] - currentPosition[2]
                var distSq = dx * dx + dy * dy + dz * dz
                if (distSq < (planet.targetRadius * 0.5f).pow(2)) { distSq = (planet.targetRadius * 0.5f).pow(2) }
                distSq += 0.01f 
                val dist = sqrt(distSq)
                val forceMagnitude = gravityConstant * planet.mass * simulatedArrowMass / distSq
                currentVelocity[0] += forceMagnitude * dx / dist * TRAJECTORY_SIMULATION_TIMESTEP; currentVelocity[1] += forceMagnitude * dy / dist * TRAJECTORY_SIMULATION_TIMESTEP; currentVelocity[2] += forceMagnitude * dz / dist * TRAJECTORY_SIMULATION_TIMESTEP
            }
            currentPosition[0] += currentVelocity[0] * TRAJECTORY_SIMULATION_TIMESTEP; currentPosition[1] += currentVelocity[1] * TRAJECTORY_SIMULATION_TIMESTEP; currentPosition[2] += currentVelocity[2] * TRAJECTORY_SIMULATION_TIMESTEP
            trajectoryPoints.add(currentPosition.copyOf())
            apple?.let { currentApple -> val collisionDistSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + currentApple.targetRadius).pow(2); if (calculateDistanceSquared(currentPosition, currentApple.worldPosition) < collisionDistSq) { return } }
        }
    }

    private fun updateGameLogic(dt: Float) {
        if (gameState.state != PuzzleState.PLAYING) return
        val currentApple = apple ?: return
        arrows.filter { it.active }.forEach { arrow ->
            planets.forEach { planet ->
                val dx = planet.worldPosition[0] - arrow.position[0]; val dy = planet.worldPosition[1] - arrow.position[1]; val dz = planet.worldPosition[2] - arrow.position[2]
                var distSq = dx*dx + dy*dy + dz*dz
                if (distSq < (planet.targetRadius * 0.5f).pow(2)) { distSq = (planet.targetRadius * 0.5f).pow(2) }
                distSq += 0.01f 
                val dist = sqrt(distSq)
                val forceMagnitude = gravityConstant * planet.mass * arrow.mass / distSq
                arrow.velocity[0] += forceMagnitude * dx / dist * dt; arrow.velocity[1] += forceMagnitude * dy / dist * dt; arrow.velocity[2] += forceMagnitude * dz / dist * dt
            }
            arrow.position[0] += arrow.velocity[0] * dt; arrow.position[1] += arrow.velocity[1] * dt; arrow.position[2] += arrow.velocity[2] * dt
            val collisionDistanceSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + currentApple.targetRadius).pow(2)
            if (calculateDistanceSquared(arrow.position, currentApple.worldPosition) < collisionDistanceSq) {
                Log.i(TAG, "Apple hit!"); gameState.score += 100 * gameState.level; gameState.state = PuzzleState.VICTORY; arrow.active = false; return 
            }
            levelOriginAnchor?.pose?.translation?.let { origin -> if (calculateDistanceSquared(arrow.position, origin) > 50f.pow(2)) { arrow.active = false } }
        }
        if (gameState.arrowsLeft == 0 && arrows.none { it.active } && gameState.state == PuzzleState.PLAYING) {
            Log.i(TAG, "No arrows left and all shot arrows inactive. Defeat."); gameState.state = PuzzleState.DEFEAT
        }
    }

    override fun onSurfaceCreated(render: SampleRender) {
        this.render = render
        try {
            planetMesh = Mesh.createFromAsset(render, planetObjFile); appleMesh = Mesh.createFromAsset(render, appleObjFile); arrowMesh = Mesh.createFromAsset(render, arrowObjFile); trajectoryDotMesh = Mesh.createFromAsset(render, trajectoryDotObjFile)
            planetTextures.clear(); planetTextureFiles.forEach { planetTextures.add(Texture.createFromAsset(render, it, Texture.WrapMode.REPEAT, Texture.ColorFormat.SRGB)) }
            if (planetTextures.isEmpty()) Log.e(TAG, "No planet textures loaded!")
            appleTexture = Texture.createFromAsset(render, appleTextureFile, Texture.WrapMode.REPEAT, Texture.ColorFormat.SRGB); arrowTexture = Texture.createFromAsset(render, arrowTextureFile, Texture.WrapMode.REPEAT, Texture.ColorFormat.SRGB); trajectoryDotTexture = Texture.createFromAsset(render, trajectoryDotTextureFile, Texture.WrapMode.CLAMP_TO_EDGE, Texture.ColorFormat.SRGB)
            planeRenderer = PlaneRenderer(render); backgroundRenderer = BackgroundRenderer(render); virtualSceneFramebuffer = Framebuffer(render, 1, 1) 
            cubemapFilter = SpecularCubemapFilter(render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES); dfgTexture = Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE, false)
            val dfgRes = 64; val buffer = ByteBuffer.allocateDirect(dfgRes * dfgRes * 4).apply { activity.assets.open("models/dfg.raw").use { it.read(array()) } }
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.textureId); GLError.maybeThrowGLException("",""); GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RG16F, dfgRes, dfgRes, 0, GLES30.GL_RG, GLES30.GL_HALF_FLOAT, buffer); GLError.maybeThrowGLException("","")
            pointCloudShader = Shader.createFromAssets(render, "shaders/point_cloud.vert", "shaders/point_cloud.frag", null).setVec4("u_Color", floatArrayOf(0.12f,0.74f,0.82f,1f)).setFloat("u_PointSize", 5f)
            pointCloudVertexBuffer = VertexBuffer(render, 4, null); pointCloudMesh = Mesh(render, Mesh.PrimitiveMode.POINTS, null, arrayOf(pointCloudVertexBuffer))
            virtualObjectShader = Shader.createFromAssets(render, "shaders/environmental_hdr.vert", "shaders/environmental_hdr.frag", mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubemapFilter.numberOfMipmapLevels.toString())).setTexture("u_Cubemap", cubemapFilter.filteredCubemapTexture).setTexture("u_DfgTexture", dfgTexture)
            gameState = GameState() 
        } catch (e: Exception) { Log.e(TAG, "Error during onSurfaceCreated", e); showError("Initialization Error: ${e.message}") }
    }

    override fun onResume(owner: LifecycleOwner) { displayRotationHelper.onResume(); hasSetTextureNames = false }
    override fun onPause(owner: LifecycleOwner) { displayRotationHelper.onPause() }
    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) { displayRotationHelper.onSurfaceChanged(width, height); virtualSceneFramebuffer.resize(width, height) }

    override fun onDrawFrame(render: SampleRender) {
        val localSession = session ?: return
        if (!hasSetTextureNames) { localSession.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId)); hasSetTextureNames = true }
        displayRotationHelper.updateSessionIfNeeded(localSession)
        val frame = try { localSession.update() } catch (e: CameraNotAvailableException) { Log.e(TAG, "Camera not available", e); showError("Camera error."); return }
        val camera = frame.camera

        if (camera.trackingState == TrackingState.TRACKING) { if (framesSinceTrackingStable < MIN_TRACKING_FRAMES_FOR_ANCHOR_PLACEMENT + 10) framesSinceTrackingStable++ } 
        else { framesSinceTrackingStable = 0; if (gameState.state == PuzzleState.PLAYING) { Log.w(TAG, "Tracking lost during play."); gameState.state = PuzzleState.WAITING_FOR_ANCHOR } }
        if (gameState.state == PuzzleState.WAITING_FOR_ANCHOR) { if (attemptCreateLevelAnchor(localSession, camera)) { resetLevel(localSession, camera) } }
        if (gameState.state == PuzzleState.PLAYING) simulateArrowTrajectory(camera) else trajectoryPoints.clear() 

        val userWantsOcclusion = activity.depthSettings.useDepthForOcclusion()
        try { backgroundRenderer.setUseDepthVisualization(render, activity.depthSettings.depthColorVisualizationEnabled()); backgroundRenderer.setUseOcclusion(render, userWantsOcclusion) } 
        catch (e: IOException) { Log.e(TAG, "Depth settings asset error", e); return }
        backgroundRenderer.updateDisplayGeometry(frame)
        if (camera.trackingState == TrackingState.TRACKING && userWantsOcclusion) { try { frame.acquireDepthImage16Bits().use { backgroundRenderer.updateCameraDepthTexture(it) } } catch (e: NotYetAvailableException) { /* Common */ } }
        if (gameState.state == PuzzleState.PLAYING) updateGameLogic(1f / 60f)
        handleTap(frame, camera)
        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)
        val snackbarMessage = when {
            gameState.state == PuzzleState.WAITING_FOR_ANCHOR -> activity.getString(R.string.waiting_for_anchor)
            camera.trackingState == TrackingState.PAUSED && camera.trackingFailureReason == TrackingFailureReason.NONE -> activity.getString(R.string.searching_planes)
            camera.trackingState == TrackingState.PAUSED -> TrackingStateHelper.getTrackingFailureReasonString(camera)
            gameState.state == PuzzleState.VICTORY -> "Level Cleared! Next level..." 
            gameState.state == PuzzleState.DEFEAT -> "Game Over. Tap to retry."
            else -> null
        }
        if (snackbarMessage != null) activity.view.snackbarHelper.showMessage(activity, snackbarMessage) else activity.view.snackbarHelper.hide(activity)
        if (frame.timestamp != 0L) backgroundRenderer.drawBackground(render)
        if (camera.trackingState == TrackingState.PAUSED && gameState.state != PuzzleState.WAITING_FOR_ANCHOR) return
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR); camera.getViewMatrix(viewMatrix, 0)
        // frame.acquirePointCloud().use { pc -> if (pc.timestamp > lastPointCloudTimestamp) { pointCloudVertexBuffer.set(pc.points); lastPointCloudTimestamp = pc.timestamp }
        //     Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        //     pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix); render.draw(pointCloudMesh, pointCloudShader)
        // }
        updateLightEstimation(frame.lightEstimate, viewMatrix)
        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST); GLES30.glDepthMask(false); GLES30.glDisable(GLES30.GL_BLEND)    

        if (gameState.state == PuzzleState.PLAYING && trajectoryPoints.isNotEmpty()) {
            val dotScaleFactor = TRAJECTORY_DOT_TARGET_RADIUS / TRAJECTORY_DOT_MODEL_DEFAULT_RADIUS
            trajectoryPoints.forEachIndexed { idx, point ->
                if (idx < TRAJECTORY_SIMULATION_START_AT_STEP) return@forEachIndexed
                Matrix.setIdentityM(modelMatrix, 0); Matrix.translateM(modelMatrix, 0, point[0], point[1], point[2])
                Matrix.scaleM(modelMatrix, 0, dotScaleFactor, dotScaleFactor, dotScaleFactor) 
                Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0); Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
                virtualObjectShader.setMat4("u_ModelView", modelViewMatrix).setMat4("u_ModelViewProjection", modelViewProjectionMatrix).setTexture("u_AlbedoTexture", trajectoryDotTexture)
                render.draw(trajectoryDotMesh, virtualObjectShader, virtualSceneFramebuffer)
            }
        }
        apple?.let { currentApple ->
            Matrix.setIdentityM(modelMatrix, 0); Matrix.translateM(modelMatrix, 0, currentApple.worldPosition[0], currentApple.worldPosition[1], currentApple.worldPosition[2])
            val appleScaleFactor = currentApple.targetRadius / APPLE_MODEL_DEFAULT_RADIUS; Matrix.scaleM(modelMatrix, 0, appleScaleFactor, appleScaleFactor, appleScaleFactor)
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0); Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix).setMat4("u_ModelViewProjection", modelViewProjectionMatrix).setTexture("u_AlbedoTexture", appleTexture)
            render.draw(appleMesh, virtualObjectShader, virtualSceneFramebuffer)
        }
        val arrowScaleFactor = ARROW_VISUAL_AND_COLLISION_RADIUS / ARROW_MODEL_DEFAULT_RADIUS
        arrows.filter { it.active }.forEach { currentArrow ->
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, currentArrow.position[0], currentArrow.position[1], currentArrow.position[2])

            // arrow rotation to match the velocity
            val direction = currentArrow.velocity.clone()
            val length = Matrix.length(direction[0], direction[1], direction[2])
            if (length > 0.001f) {
                direction[0] /= length; direction[1] /= length; direction[2] /= length
                // forward is +Z
                val defaultForward = floatArrayOf(0f, 0f, 1f)
                rotationMatrixFromTo(defaultForward, direction, rotationMatrix)
                // M = T * R * S
                Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, rotationMatrix, 0)
                for (i in tempMatrix.indices) modelMatrix[i] = tempMatrix[i]
            }
            Matrix.scaleM(modelMatrix, 0, arrowScaleFactor, arrowScaleFactor, arrowScaleFactor)
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix).setMat4("u_ModelViewProjection", modelViewProjectionMatrix).setTexture("u_AlbedoTexture", arrowTexture)
            render.draw(arrowMesh, virtualObjectShader, virtualSceneFramebuffer)
        }
        if (planetTextures.isNotEmpty()) { planets.forEach { currentPlanet ->
                Matrix.setIdentityM(modelMatrix, 0); Matrix.translateM(modelMatrix, 0, currentPlanet.worldPosition[0], currentPlanet.worldPosition[1], currentPlanet.worldPosition[2])
                val planetScaleFactor = currentPlanet.targetRadius / PLANET_MODEL_DEFAULT_RADIUS; Matrix.scaleM(modelMatrix, 0, planetScaleFactor, planetScaleFactor, planetScaleFactor)
                Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0); Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
                virtualObjectShader.setMat4("u_ModelView", modelViewMatrix).setMat4("u_ModelViewProjection", modelViewProjectionMatrix).setTexture("u_AlbedoTexture", planetTextures[currentPlanet.textureIdx % planetTextures.size])
                render.draw(planetMesh, virtualObjectShader, virtualSceneFramebuffer)
        } }
        GLES30.glEnable(GLES30.GL_BLEND); GLES30.glDepthMask(true); GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        var occlusionWasForcedOffForGameScene = false
        if (userWantsOcclusion) { backgroundRenderer.setUseOcclusion(render, false); occlusionWasForcedOffForGameScene = true }
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
        if (occlusionWasForcedOffForGameScene) { backgroundRenderer.setUseOcclusion(render, true) }
        if (gameState.state == PuzzleState.VICTORY) { Log.i(TAG, "Victory Lvl ${gameState.level}!"); gameState.level++; resetLevel(localSession, camera) } 
        else if (gameState.state == PuzzleState.DEFEAT) { Log.i(TAG, "Defeat Lvl ${gameState.level}.") }

        levelTextView.post {
            levelTextView.text = "Level ${gameState.level}"
        }
        arrowsLeftTextView.post {
            arrowsLeftTextView.text = gameState.arrowsLeft.toString()
        }
    }

    private fun updateLightEstimation(lightEstimate: LightEstimate, viewMatrixParam: FloatArray) {
        // default light bc otherwhise weird albedo 
        virtualObjectShader.setBool("u_LightEstimateIsValid", false)
        val defaultLightDirection = floatArrayOf(0.0f, -1.0f, 0.0f, 0.0f)
        val defaultLightIntensity = floatArrayOf(1.0f, 1.0f, 1.0f)
        Matrix.invertM(viewInverseMatrix, 0, viewMatrixParam, 0)
        virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix)
        Matrix.multiplyMV(viewLightDirection, 0, viewMatrixParam, 0, defaultLightDirection, 0)
        virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection)
        virtualObjectShader.setVec3("u_LightIntensity", defaultLightIntensity)
        for (i in sphericalHarmonicsCoefficients.indices) sphericalHarmonicsCoefficients[i] = 0.2f
        virtualObjectShader.setVec3Array("u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients)
    }

    private fun handleTap(frame: Frame, camera: Camera) {
        if (camera.trackingState != TrackingState.TRACKING) return
        activity.view.tapHelper.poll()?.let { _ ->
            if (gameState.state == PuzzleState.PLAYING) launchArrow(camera)
            else if (gameState.state == PuzzleState.DEFEAT) { Log.i(TAG, "Tap in DEFEAT state."); gameState.level = 1; gameState.score = 0; resetLevel(session!!, camera) } 
            else if (gameState.state == PuzzleState.WAITING_FOR_ANCHOR) { Log.d(TAG, "Tap while waiting for anchor.")
                 if (attemptCreateLevelAnchor(session!!, camera)) resetLevel(session!!, camera) else activity.view.snackbarHelper.showMessage(activity, "Still trying...")
            }
        }
    }
    private fun showError(errorMessage: String) = activity.view.snackbarHelper.showError(activity, errorMessage)
}