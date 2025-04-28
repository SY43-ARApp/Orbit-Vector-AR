package com.google.ar.core.examples.kotlin.helloar

import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Camera
import com.google.ar.core.Session
import com.google.ar.core.Pose
import com.google.ar.core.examples.java.common.samplerender.Framebuffer
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.java.common.samplerender.Texture

/**
 * Base class for all game objects.
 */
abstract class GameObject {
    /**
     * Called every frame by GameEngine.
     */
    open fun update(session: Session, frame: Frame, camera: Camera) {}
}

/**
 * Interface for game objects in the AR scene.
 */
interface ARGameObject {
    val anchor: Anchor? // ARCore anchor, null if not placed yet or fixed in world space without anchor
    val pose: Pose // World pose, derived from anchor or set directly

    /** Updates the game object's state. Called once per frame. */
    fun update(deltaTime: Float)

    /** Draws the game object. */
    fun draw(
        render: SampleRender,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        lightEstimateIsValid: Boolean,
        viewInverseMatrix: FloatArray,
        viewLightDirection: FloatArray,
        lightIntensity: FloatArray,
        sphericalHarmonicsCoefficients: FloatArray,
        cubemapFilter: com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter,
        dfgTexture: Texture,
        virtualSceneFramebuffer: Framebuffer
    )

    /** Gets the mesh associated with this object. */
    fun getMesh(): Mesh?

    /** Gets the shader associated with this object. */
    fun getShader(): Shader?

    /** Gets the albedo texture associated with this object. */
    fun getAlbedoTexture(): Texture?

    /** Gets the PBR texture associated with this object. */
    fun getPbrTexture(): Texture?

    /** Indicates if the object should use the instant placement texture variant. */
    fun useInstantPlacementTexture(): Boolean
}

/** A default implementation for a simple static mesh game object. */
open class StaticGameObject(
    override var anchor: Anchor?,
    val initialPose: Pose?,
    private val mesh: Mesh,
    private val shader: Shader,
    private val albedoTexture: Texture,
    private val pbrTexture: Texture,
    private val instantPlacementTexture: Texture? = null // Optional texture for instant placement
) : ARGameObject {

    override val pose: Pose
        get() = anchor?.pose ?: initialPose ?: Pose.IDENTITY

    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    override fun update(deltaTime: Float) {
        // Static objects don't update their state over time unless moved by ARCore anchor updates
    }

    override fun draw(
        render: SampleRender,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        lightEstimateIsValid: Boolean,
        viewInverseMatrix: FloatArray,
        viewLightDirection: FloatArray,
        lightIntensity: FloatArray,
        sphericalHarmonicsCoefficients: FloatArray,
        cubemapFilter: com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter,
        dfgTexture: Texture,
        virtualSceneFramebuffer: Framebuffer
    ) {
        // Update shader parameters only if light estimate is valid
        if (lightEstimateIsValid) {
            shader.setBool("u_LightEstimateIsValid", true)
            shader.setMat4("u_ViewInverse", viewInverseMatrix)
            shader.setVec4("u_ViewLightDirection", viewLightDirection)
            shader.setVec3("u_LightIntensity", lightIntensity)
            shader.setVec3Array("u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients)
            shader.setTexture("u_Cubemap", cubemapFilter.filteredCubemapTexture)
            shader.setTexture("u_DfgTexture", dfgTexture)
        } else {
            shader.setBool("u_LightEstimateIsValid", false)
        }

        // Get model matrix from pose
        pose.toMatrix(modelMatrix, 0)

        // Calculate model/view/projection matrices
        android.opengl.Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        android.opengl.Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        // Update shader properties and draw
        shader.setMat4("u_ModelView", modelViewMatrix)
        shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)

        val currentAlbedo = if (useInstantPlacementTexture() && instantPlacementTexture != null) {
            instantPlacementTexture
        } else {
            albedoTexture
        }
        shader.setTexture("u_AlbedoTexture", currentAlbedo)
        shader.setTexture("u_RoughnessMetallicAmbientOcclusionTexture", pbrTexture)

        render.draw(mesh, shader, virtualSceneFramebuffer)
    }

    override fun getMesh(): Mesh? = mesh
    override fun getShader(): Shader? = shader
    override fun getAlbedoTexture(): Texture? = albedoTexture
    override fun getPbrTexture(): Texture? = pbrTexture

    override fun useInstantPlacementTexture(): Boolean {
        // Determine if the anchor is an Instant Placement anchor
        val trackable = anchor?.let { a ->
            // We need a way to associate the trackable back, this is tricky without modifying WrappedAnchor
            // For now, assume non-anchored or non-instant placement
            // A better approach would be to store the trackable type alongside the anchor
            // when the GameObject is created.
            // Let's assume for this simple case, if anchor exists and is tracking, it's not instant placement
            // unless explicitly told otherwise during creation (which we aren't doing here yet).
            false // Defaulting to false
        } ?: false // If no anchor, definitely not instant placement

        // Example check (needs Trackable info passed during construction or stored):
        // return (anchor?.trackable as? InstantPlacementPoint)?.trackingMethod == 
        //        InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE

        return false // Simplified: Assume not using instant placement texture for now
    }
}
