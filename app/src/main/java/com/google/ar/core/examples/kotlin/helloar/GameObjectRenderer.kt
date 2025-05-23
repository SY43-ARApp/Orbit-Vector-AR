package com.google.ar.core.examples.kotlin.helloar

import android.opengl.GLES30
import android.opengl.Matrix
import com.google.ar.core.examples.java.common.samplerender.Framebuffer
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.kotlin.helloar.GameConstants.APPLE_MODEL_DEFAULT_RADIUS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.ARROW_MODEL_DEFAULT_RADIUS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.ARROW_TARGET_RADIUS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.PLANET_MODEL_DEFAULT_RADIUS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.TRAJECTORY_DOT_MODEL_DEFAULT_RADIUS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.TRAJECTORY_DOT_TARGET_RADIUS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.TRAJECTORY_SIMULATION_START_AT_STEP

class GameObjectRenderer(private val assetLoader: AssetLoader) {

    // temp matrices for math
    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    fun drawPlanets(
        render: SampleRender,
        planets: List<Planet>,
        shader: Shader,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        framebuffer: Framebuffer
    ) {
        if (assetLoader.planetTextures.isEmpty()) return

        planets.forEach { planet ->
            Matrix.setIdentityM(modelMatrix, 0)

            // move planet to its world position
            Matrix.translateM(modelMatrix, 0, planet.worldPosition[0], planet.worldPosition[1], planet.worldPosition[2])
            val scaleFactor = planet.targetRadius / PLANET_MODEL_DEFAULT_RADIUS
            // scale planet to match radius
            Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor)
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            shader.setMat4("u_ModelView", modelViewMatrix)
            shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            shader.setTexture("u_AlbedoTexture", assetLoader.planetTextures[planet.textureIdx % assetLoader.planetTextures.size])
            render.draw(assetLoader.planetMesh, shader, framebuffer)
        }
    }

    fun drawApple(
        render: SampleRender,
        apple: Apple?,
        shader: Shader,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        framebuffer: Framebuffer
    ) {
        apple?.let {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, it.worldPosition[0], it.worldPosition[1], it.worldPosition[2])
            val scaleFactor = it.targetRadius / APPLE_MODEL_DEFAULT_RADIUS
            Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor)

            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            shader.setMat4("u_ModelView", modelViewMatrix)
            shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            shader.setTexture("u_AlbedoTexture", assetLoader.appleTexture)
            render.draw(assetLoader.appleMesh, shader, framebuffer)
        }
    }

    fun drawArrows(
        render: SampleRender,
        arrows: List<Arrow>,
        shader: Shader,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        framebuffer: Framebuffer
    ) {
        val scaleFactor = ARROW_TARGET_RADIUS / ARROW_MODEL_DEFAULT_RADIUS
        arrows.filter { it.active }.forEach { arrow ->
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, arrow.position[0], arrow.position[1], arrow.position[2])

            // rotate arrow to match velocity direction
            val direction = arrow.velocity.clone()
            val length = Matrix.length(direction[0], direction[1], direction[2])
            if (length > 0.001f) {
                direction[0] /= length; direction[1] /= length; direction[2] /= length
                val defaultForward = floatArrayOf(0f, 0f, 1f) // arrow model points +Z
                MathUtils.rotationMatrixFromTo(defaultForward, direction, rotationMatrix)
                // apply rotation: modelMatrix = Translation * Rotation
                Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, rotationMatrix, 0)
                System.arraycopy(tempMatrix, 0, modelMatrix, 0, 16)
            }

            // scale arrow
            Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor)

            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            shader.setMat4("u_ModelView", modelViewMatrix)
            shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            shader.setTexture("u_AlbedoTexture", assetLoader.arrowTexture)
            render.draw(assetLoader.arrowMesh, shader, framebuffer)
        }
    }

    fun drawTrajectory(
        render: SampleRender,
        trajectoryPoints: List<FloatArray>,
        shader: Shader,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        framebuffer: Framebuffer
    ) {
        if (trajectoryPoints.isEmpty()) return

        val scaleFactor = TRAJECTORY_DOT_TARGET_RADIUS / TRAJECTORY_DOT_MODEL_DEFAULT_RADIUS
        trajectoryPoints.forEachIndexed { index, point ->
            if (index < TRAJECTORY_SIMULATION_START_AT_STEP) return@forEachIndexed // skip early points

            Matrix.setIdentityM(modelMatrix, 0)
            // move dot to trajectory point
            Matrix.translateM(modelMatrix, 0, point[0], point[1], point[2])
            Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor)

            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            shader.setMat4("u_ModelView", modelViewMatrix)
            shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            shader.setTexture("u_AlbedoTexture", assetLoader.trajectoryDotTexture)
            render.draw(assetLoader.trajectoryDotMesh, shader, framebuffer)
        }
    }

    fun drawReadyArrow(
        render: SampleRender,
        position: FloatArray,
        direction: FloatArray,
        shader: Shader,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        framebuffer: Framebuffer
    ) {
        val scaleFactor = ARROW_TARGET_RADIUS / ARROW_MODEL_DEFAULT_RADIUS
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, position[0], position[1], position[2])
        // rotate arrow to match direction
        val dir = direction.clone()
        val length = Matrix.length(dir[0], dir[1], dir[2])
        if (length > 0.001f) {
            dir[0] /= length; dir[1] /= length; dir[2] /= length
            val defaultForward = floatArrayOf(0f, 0f, 1f) // arrow model points +Z
            MathUtils.rotationMatrixFromTo(defaultForward, dir, rotationMatrix)
            Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, rotationMatrix, 0)
            System.arraycopy(tempMatrix, 0, modelMatrix, 0, 16)
        }
        Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor)
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
        shader.setMat4("u_ModelView", modelViewMatrix)
        shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
        shader.setTexture("u_AlbedoTexture", assetLoader.arrowTexture)
        render.draw(assetLoader.arrowMesh, shader, framebuffer)
    }

    fun drawMoons(
        render: SampleRender,
        moons: List<Moon>,
        shader: Shader,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        framebuffer: Framebuffer
    ) {
        if (assetLoader.moonTextures.isEmpty()) return
        moons.forEach { moon ->
            val pos = moon.getWorldPosition()
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, pos[0], pos[1], pos[2])
            val scaleFactor = moon.targetRadius / PLANET_MODEL_DEFAULT_RADIUS
            Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor)
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
            shader.setMat4("u_ModelView", modelViewMatrix)
            shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            shader.setTexture("u_AlbedoTexture", assetLoader.moonTextures[moon.textureIdx % assetLoader.moonTextures.size])
            render.draw(assetLoader.planetMesh, shader, framebuffer)
        }
    }
}