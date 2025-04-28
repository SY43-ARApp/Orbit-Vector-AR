package com.google.ar.core.examples.kotlin.helloar

import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.Camera
import kotlin.random.Random
import com.google.ar.core.Pose
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.java.common.samplerender.Texture

/**
 * Simple game engine with update loop and game object management.
 */
class GameEngine {
    private val gameObjects = mutableListOf<GameObject>()
    var initialized = false
    private var planetSpawned = false
    private val planetTextures = listOf(
        "models/textures/planet_texture_1.jpg",
        "models/textures/planet_texture_2.jpg"
    )
    private val planetModelPath = "models/planet.obj"
    var planetRadiusRange = 0.1f..0.2f

    fun addObject(obj: GameObject) {
        gameObjects.add(obj)
    }

    fun removeObject(obj: GameObject) {
        gameObjects.remove(obj)
    }

    fun clearObjects() {
        gameObjects.clear()
    }

    fun getGameObjects(): List<GameObject> = gameObjects

    fun getPlanetTextures(): List<String> = planetTextures

    /**
     * Called every frame from the renderer.
     * session, frame, and camera are passed for AR context.
     */
    fun update(session: Session, frame: Frame, camera: Camera) {
        if (!initialized) {
            // Example: spawn default pawn at center of first detected plane
            ARUtils.trySpawnDefaultPawn(session, this)
            initialized = true
        }
        // Demo: spawn a planet if at least one surface and not already spawned
        if (!planetSpawned && ARUtils.hasSurface(session)) {
            val plane = ARUtils.getTrackedPlanes(session).firstOrNull()
            if (plane != null) {
                val pose = randomPoseOnPlane(plane)
                val min = planetRadiusRange.start
                val max = planetRadiusRange.endInclusive
                val radius = Random.nextFloat() * (max - min) + min
                val texturePath = planetTextures.random()
                addObject(PlanetObject(pose, radius, texturePath))
                planetSpawned = true
            }
        }
        for (obj in gameObjects) {
            obj.update(session, frame, camera)
        }
    }

    private fun randomPoseOnPlane(plane: com.google.ar.core.Plane): Pose {
        val extentX = plane.extentX
        val extentZ = plane.extentZ
        val center = plane.centerPose
        val randX = (Random.nextFloat() - 0.5f) * extentX
        val randZ = (Random.nextFloat() - 0.5f) * extentZ
        val randY = Random.nextFloat() * 0.5f // up to 0.5m above the plane
        val translation = floatArrayOf(
            center.tx() + randX,
            center.ty() + randY,
            center.tz() + randZ
        )
        return Pose(translation, center.rotationQuaternion)
    }
}

class PlanetObject(
    val pose: Pose,
    val radius: Float,
    val texturePath: String
) : GameObject() {
    // These would be loaded in a real renderer
    var mesh: Mesh? = null
    var shader: Shader? = null
    var texture: Texture? = null
    override fun update(session: Session, frame: Frame, camera: Camera) {
        // Rendering logic would go here
        // For demo: just keep the pose, radius, and texturePath
    }
}